import { after, before, beforeEach, test } from "node:test";
import { readFileSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from "@firebase/rules-unit-testing";
import {
  Timestamp,
  collection,
  doc,
  getDoc,
  getDocs,
  increment,
  query,
  serverTimestamp,
  setDoc,
  updateDoc,
  where,
  writeBatch,
} from "firebase/firestore";
import { getBytes, ref, uploadBytes } from "firebase/storage";

const projectId = "joblink-rules-test";
const rootDirectory = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const firestoreRules = readFileSync(resolve(rootDirectory, "firestore.rules"), "utf8");
const storageRules = readFileSync(resolve(rootDirectory, "storage.rules"), "utf8");

const seekerId = "seeker-one";
const secondSeekerId = "seeker-two";
const employerId = "employer-one";
const otherEmployerId = "employer-two";
const jobId = "job-one";
const applicationId = `${jobId}_${seekerId}`;

let testEnvironment;

before(async () => {
  testEnvironment = await initializeTestEnvironment({
    projectId,
    firestore: { rules: firestoreRules },
    storage: { rules: storageRules },
  });
});

beforeEach(async () => {
  await testEnvironment.clearFirestore();
  await testEnvironment.clearStorage();
});

after(async () => {
  await testEnvironment.cleanup();
});

function context(uid, email = `${uid}@example.com`) {
  return testEnvironment.authenticatedContext(uid, { email });
}

function userData(uid, role, overrides = {}) {
  return {
    uid,
    fullName: role === "EMPLOYER" ? "Hiring Manager" : "Job Seeker",
    email: `${uid}@example.com`,
    role,
    createdAt: Timestamp.fromMillis(1_700_000_000_000),
    ...overrides,
  };
}

function companyData(uid, overrides = {}) {
  const timestamp = Timestamp.fromMillis(1_700_000_000_000);
  return {
    ownerUid: uid,
    companyName: "Example Company",
    companyDescription: "A company description",
    industry: "Technology",
    companySize: "11-50",
    location: "Colombo",
    website: "https://example.com",
    contactEmail: `${uid}@example.com`,
    createdAt: timestamp,
    updatedAt: timestamp,
    ...overrides,
  };
}

function jobData(ownerUid = employerId, overrides = {}) {
  const timestamp = Timestamp.fromMillis(1_700_000_000_000);
  return {
    id: jobId,
    employerId: ownerUid,
    companyName: "Example Company",
    title: "Android Developer",
    description: "Build reliable Android applications.",
    category: "Engineering",
    location: "Colombo",
    workMode: "HYBRID",
    jobType: "FULL_TIME",
    salaryMin: 100000,
    salaryMax: 200000,
    currency: "LKR",
    experienceLevel: "JUNIOR",
    requiredSkills: ["Kotlin"],
    requirements: ["Android development"],
    benefits: ["Learning budget"],
    applicationDeadline: Timestamp.fromMillis(1_900_000_000_000),
    createdAt: timestamp,
    updatedAt: timestamp,
    active: true,
    applicantCount: 0,
    ...overrides,
  };
}

function applicationData(overrides = {}) {
  return {
    applicationId,
    jobId,
    employerId,
    applicantId: seekerId,
    jobTitle: "Android Developer",
    companyName: "Example Company",
    applicantFullName: "Job Seeker",
    applicantEmail: `${seekerId}@example.com`,
    applicantHeadline: "Android developer",
    applicantLocation: "Colombo",
    applicantPhone: "+94111111111",
    applicantBio: "Candidate bio",
    applicantEducation: "BSc",
    applicantExperienceSummary: "Junior Android experience",
    applicantSkills: ["Kotlin"],
    submittedAt: Timestamp.fromMillis(1_700_000_000_000),
    status: "SUBMITTED",
    coverMessage: null,
    cvReference: `users/${seekerId}/cv/current.pdf`,
    ...overrides,
  };
}

function cvAccessData(overrides = {}) {
  return {
    employerId,
    applicantId: seekerId,
    jobId,
    applicationId,
    grantedAt: Timestamp.fromMillis(1_700_000_000_000),
    ...overrides,
  };
}

async function seedFirestore({ includeApplication = false } = {}) {
  await testEnvironment.withSecurityRulesDisabled(async (adminContext) => {
    const database = adminContext.firestore();
    await Promise.all([
      setDoc(doc(database, "users", seekerId), userData(seekerId, "JOB_SEEKER", {
        professionalHeadline: "Android developer",
        location: "Colombo",
        phone: "+94111111111",
        bio: "Candidate bio",
        education: "BSc",
        experienceSummary: "Junior Android experience",
        skills: ["Kotlin"],
        preferredJobTypes: ["FULL_TIME"],
        cv: {
          fileName: "cv.pdf",
          storagePath: `users/${seekerId}/cv/current.pdf`,
          contentType: "application/pdf",
          sizeBytes: 4,
          uploadedAt: Timestamp.fromMillis(1_700_000_000_000),
        },
      })),
      setDoc(doc(database, "users", secondSeekerId), userData(secondSeekerId, "JOB_SEEKER")),
      setDoc(doc(database, "users", employerId), userData(employerId, "EMPLOYER")),
      setDoc(doc(database, "users", otherEmployerId), userData(otherEmployerId, "EMPLOYER")),
      setDoc(doc(database, "companies", employerId), companyData(employerId)),
      setDoc(doc(database, "companies", otherEmployerId), companyData(otherEmployerId, {
        companyName: "Other Company",
      })),
      setDoc(doc(database, "jobs", jobId), jobData()),
    ]);

    if (includeApplication) {
      await Promise.all([
        setDoc(doc(database, "applications", applicationId), applicationData()),
        setDoc(doc(database, "cvAccess", `${employerId}_${seekerId}`), cvAccessData()),
      ]);
    }
  });
}

async function submitApplication(database, overrides = {}) {
  const batch = writeBatch(database);
  batch.set(
    doc(database, "applications", applicationId),
    applicationData({ submittedAt: serverTimestamp(), ...overrides }),
  );
  batch.set(
    doc(database, "cvAccess", `${employerId}_${seekerId}`),
    cvAccessData({ grantedAt: serverTimestamp() }),
  );
  batch.update(doc(database, "jobs", jobId), { applicantCount: increment(1) });
  return batch.commit();
}

test("users can create and read only their own base profile", async () => {
  const seeker = context(seekerId);
  const database = seeker.firestore();
  await assertSucceeds(setDoc(doc(database, "users", seekerId), {
    uid: seekerId,
    fullName: "Job Seeker",
    email: `${seekerId}@example.com`,
    role: "JOB_SEEKER",
    createdAt: serverTimestamp(),
  }));
  await assertSucceeds(getDoc(doc(database, "users", seekerId)));
  await assertFails(getDoc(doc(context(secondSeekerId).firestore(), "users", seekerId)));
});

test("a user cannot create another user's profile or spoof the auth email", async () => {
  const database = context(seekerId).firestore();
  await assertFails(setDoc(doc(database, "users", secondSeekerId), {
    uid: secondSeekerId,
    fullName: "Other User",
    email: `${secondSeekerId}@example.com`,
    role: "JOB_SEEKER",
    createdAt: serverTimestamp(),
  }));
  await assertFails(setDoc(doc(database, "users", seekerId), {
    uid: seekerId,
    fullName: "Job Seeker",
    email: "spoofed@example.com",
    role: "JOB_SEEKER",
    createdAt: serverTimestamp(),
  }));
});

test("profile owners may edit profile fields but cannot change role or identity", async () => {
  await seedFirestore();
  const database = context(seekerId).firestore();
  const profile = doc(database, "users", seekerId);
  await assertSucceeds(updateDoc(profile, { professionalHeadline: "Kotlin Developer" }));
  await assertFails(updateDoc(profile, { role: "EMPLOYER" }));
  await assertFails(updateDoc(profile, { uid: employerId }));
  await assertFails(updateDoc(profile, { email: "changed@example.com" }));
});

test("employers cannot read or modify applicant user profiles", async () => {
  await seedFirestore();
  const applicant = doc(context(employerId).firestore(), "users", seekerId);
  await assertFails(getDoc(applicant));
  await assertFails(updateDoc(applicant, { fullName: "Changed by employer" }));
});

test("company profiles are writable only by their authenticated employer owner", async () => {
  await seedFirestore();
  const ownCompany = doc(context(employerId).firestore(), "companies", employerId);
  await assertSucceeds(updateDoc(ownCompany, {
    industry: "Software",
    updatedAt: serverTimestamp(),
  }));
  await assertFails(updateDoc(
    doc(context(otherEmployerId).firestore(), "companies", employerId),
    { industry: "Changed by another employer", updatedAt: serverTimestamp() },
  ));
  await assertFails(getDoc(doc(context(seekerId).firestore(), "companies", employerId)));
});

test("job seekers cannot create employer jobs", async () => {
  await seedFirestore();
  const database = context(seekerId).firestore();
  await assertFails(setDoc(doc(database, "jobs", "seeker-job"), {
    ...jobData(seekerId),
    id: "seeker-job",
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  }));
});

test("an employer can create a job only for their own authoritative company", async () => {
  await seedFirestore();
  const database = context(employerId).firestore();
  await assertSucceeds(setDoc(doc(database, "jobs", "new-job"), {
    ...jobData(employerId),
    id: "new-job",
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  }));
  await assertFails(setDoc(doc(database, "jobs", "spoofed-job"), {
    ...jobData(otherEmployerId),
    id: "spoofed-job",
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  }));
  await assertFails(setDoc(doc(database, "jobs", "wrong-company"), {
    ...jobData(employerId),
    id: "wrong-company",
    companyName: "Spoofed Company",
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  }));
});

test("only the owning employer can edit a job and protected counts remain immutable", async () => {
  await seedFirestore();
  const ownerJob = doc(context(employerId).firestore(), "jobs", jobId);
  const otherEmployerJob = doc(context(otherEmployerId).firestore(), "jobs", jobId);
  await assertSucceeds(updateDoc(ownerJob, {
    title: "Senior Android Developer",
    updatedAt: serverTimestamp(),
  }));
  await assertFails(updateDoc(otherEmployerJob, {
    title: "Hijacked title",
    updatedAt: serverTimestamp(),
  }));
  await assertFails(updateDoc(ownerJob, {
    applicantCount: 99,
    updatedAt: serverTimestamp(),
  }));
});

test("active jobs are available to authenticated users while inactive jobs remain owner-only", async () => {
  await seedFirestore();
  await testEnvironment.withSecurityRulesDisabled(async (adminContext) => {
    await setDoc(doc(adminContext.firestore(), "jobs", "inactive-job"), jobData(employerId, {
      id: "inactive-job",
      active: false,
    }));
  });
  await assertSucceeds(getDoc(doc(context(seekerId).firestore(), "jobs", jobId)));
  await assertFails(getDoc(doc(context(seekerId).firestore(), "jobs", "inactive-job")));
  await assertSucceeds(getDoc(doc(context(employerId).firestore(), "jobs", "inactive-job")));
  await assertFails(getDoc(doc(testEnvironment.unauthenticatedContext().firestore(), "jobs", jobId)));
});

test("job list queries must constrain results to active jobs or the owning employer", async () => {
  await seedFirestore();
  await testEnvironment.withSecurityRulesDisabled(async (adminContext) => {
    await setDoc(doc(adminContext.firestore(), "jobs", "inactive-job"), jobData(employerId, {
      id: "inactive-job",
      active: false,
    }));
  });

  const seekerJobs = collection(context(seekerId).firestore(), "jobs");
  await assertSucceeds(getDocs(query(seekerJobs, where("active", "==", true))));
  await assertFails(getDocs(seekerJobs));

  const employerJobs = collection(context(employerId).firestore(), "jobs");
  await assertSucceeds(getDocs(query(employerJobs, where("employerId", "==", employerId))));
});

test("legacy jobs without applicantCount can still be edited and applied to safely", async () => {
  await seedFirestore();
  await testEnvironment.withSecurityRulesDisabled(async (adminContext) => {
    const legacy = jobData();
    delete legacy.applicantCount;
    await setDoc(doc(adminContext.firestore(), "jobs", jobId), legacy);
  });

  await assertSucceeds(updateDoc(
    doc(context(employerId).firestore(), "jobs", jobId),
    { description: "Updated legacy description", updatedAt: serverTimestamp() },
  ));
  await assertSucceeds(submitApplication(context(seekerId).firestore()));
});

test("a job seeker can submit one atomic application for themselves", async () => {
  await seedFirestore();
  const database = context(seekerId).firestore();
  await assertSucceeds(submitApplication(database));
  await assertFails(submitApplication(database));
});

test("application IDs and ownership cannot be supplied for another applicant or employer", async () => {
  await seedFirestore();
  const database = context(seekerId).firestore();
  await assertFails(submitApplication(database, { applicantId: secondSeekerId }));
  await assertFails(submitApplication(database, { employerId: otherEmployerId }));
});

test("applicants and owning employers can read applications but unrelated employers cannot", async () => {
  await seedFirestore({ includeApplication: true });
  await assertSucceeds(getDoc(doc(context(seekerId).firestore(), "applications", applicationId)));
  await assertSucceeds(getDoc(doc(context(employerId).firestore(), "applications", applicationId)));
  await assertFails(getDoc(doc(context(otherEmployerId).firestore(), "applications", applicationId)));

  const employerQuery = query(
    collection(context(employerId).firestore(), "applications"),
    where("employerId", "==", employerId),
  );
  await assertSucceeds(getDocs(employerQuery));
});

test("only the job owner can update application status and no applicant data can change", async () => {
  await seedFirestore({ includeApplication: true });
  const ownerApplication = doc(context(employerId).firestore(), "applications", applicationId);
  await assertSucceeds(updateDoc(ownerApplication, { status: "SHORTLISTED" }));
  await assertFails(updateDoc(
    doc(context(otherEmployerId).firestore(), "applications", applicationId),
    { status: "REJECTED" },
  ));
  await assertFails(updateDoc(
    doc(context(seekerId).firestore(), "applications", applicationId),
    { status: "WITHDRAWN" },
  ));
  await assertFails(updateDoc(ownerApplication, {
    status: "REVIEWED",
    applicantEmail: "changed@example.com",
  }));
});

test("CV access grants cannot be created independently of a valid application batch", async () => {
  await seedFirestore();
  const access = doc(context(seekerId).firestore(), "cvAccess", `${employerId}_${seekerId}`);
  await assertFails(setDoc(access, {
    ...cvAccessData(),
    grantedAt: serverTimestamp(),
  }));
});

test("saved jobs are private to job seekers and must reference an active job", async () => {
  await seedFirestore();
  const savedJob = doc(context(seekerId).firestore(), "users", seekerId, "savedJobs", jobId);
  await assertSucceeds(setDoc(savedJob, {
    jobId,
    job: jobData(),
    savedAt: serverTimestamp(),
  }));
  await assertFails(getDoc(
    doc(context(secondSeekerId).firestore(), "users", seekerId, "savedJobs", jobId),
  ));
  await assertFails(setDoc(
    doc(context(employerId).firestore(), "users", employerId, "savedJobs", jobId),
    { jobId, job: jobData(), savedAt: serverTimestamp() },
  ));
});

test("messaging registrations are writable only below the authenticated user's profile", async () => {
  await seedFirestore();
  const registration = {
    installationId: "fcm-installation-id",
    platform: "ANDROID",
    updatedAt: serverTimestamp(),
  };
  await assertSucceeds(setDoc(
    doc(context(seekerId).firestore(), "users", seekerId, "messagingRegistrations", "hash"),
    registration,
  ));
  await assertFails(setDoc(
    doc(context(seekerId).firestore(), "users", secondSeekerId, "messagingRegistrations", "hash"),
    registration,
  ));
});

test("only a job seeker may upload a bounded PDF to their canonical CV path", async () => {
  await seedFirestore();
  const seekerStorage = context(seekerId).storage();
  await assertSucceeds(uploadBytes(
    ref(seekerStorage, `users/${seekerId}/cv/current.pdf`),
    new Uint8Array([0x25, 0x50, 0x44, 0x46]),
    { contentType: "application/pdf" },
  ));
  await assertFails(uploadBytes(
    ref(seekerStorage, `users/${seekerId}/cv/not-current.pdf`),
    new Uint8Array([0x25, 0x50, 0x44, 0x46]),
    { contentType: "application/pdf" },
  ));
  await assertFails(uploadBytes(
    ref(seekerStorage, `users/${seekerId}/cv/current.pdf`),
    new Uint8Array([1, 2, 3]),
    { contentType: "text/plain" },
  ));
  await assertFails(uploadBytes(
    ref(context(employerId).storage(), `users/${seekerId}/cv/current.pdf`),
    new Uint8Array([0x25, 0x50, 0x44, 0x46]),
    { contentType: "application/pdf" },
  ));
});

test("CV downloads are limited to the owner and the employer with validated application access", async () => {
  await seedFirestore({ includeApplication: true });
  const objectPath = `users/${seekerId}/cv/current.pdf`;
  await uploadBytes(
    ref(context(seekerId).storage(), objectPath),
    new Uint8Array([0x25, 0x50, 0x44, 0x46]),
    { contentType: "application/pdf" },
  );
  await assertSucceeds(getBytes(ref(context(seekerId).storage(), objectPath)));
  await assertSucceeds(getBytes(ref(context(employerId).storage(), objectPath)));
  await assertFails(getBytes(ref(context(otherEmployerId).storage(), objectPath)));
  await assertFails(getBytes(ref(context(secondSeekerId).storage(), objectPath)));
  await assertFails(getBytes(ref(testEnvironment.unauthenticatedContext().storage(), objectPath)));
});
