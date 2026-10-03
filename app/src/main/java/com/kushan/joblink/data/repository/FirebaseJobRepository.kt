package com.kushan.joblink.data.repository

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.kushan.joblink.data.model.Job
import com.kushan.joblink.data.model.JobType
import com.kushan.joblink.data.model.UserRole
import com.kushan.joblink.data.model.WorkMode
import kotlinx.coroutines.tasks.await

class FirebaseJobRepository(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : JobRepository {

    override suspend fun getEmployerJobs(): JobResult<List<Job>> {
        val uid = firebaseAuth.currentUser?.uid
            ?: return JobResult.Failure(JobError.NOT_AUTHENTICATED)

        return try {
            val accountError = employerAccountError(uid)
            if (accountError != null) return JobResult.Failure(accountError)

            val jobs = firestore.collection(JOBS_COLLECTION)
                .whereEqualTo(FIELD_EMPLOYER_ID, uid)
                .get()
                .await()
                .documents
                .mapNotNull { document -> document.toJobOrNull() }
                .sortedByDescending { it.createdAt?.seconds ?: Long.MIN_VALUE }
            JobResult.Success(jobs)
        } catch (exception: Exception) {
            JobResult.Failure(exception.toJobError())
        }
    }

    override suspend fun getEmployerJob(jobId: String): JobResult<Job> {
        val uid = firebaseAuth.currentUser?.uid
            ?: return JobResult.Failure(JobError.NOT_AUTHENTICATED)
        if (jobId.isBlank()) return JobResult.Failure(JobError.JOB_NOT_FOUND)

        return try {
            val accountError = employerAccountError(uid)
            if (accountError != null) return JobResult.Failure(accountError)

            val document = firestore.collection(JOBS_COLLECTION).document(jobId).get().await()
            val job = document.toJobOrNull()
            if (job == null || job.employerId != uid) {
                JobResult.Failure(JobError.JOB_NOT_FOUND)
            } else {
                JobResult.Success(job)
            }
        } catch (exception: Exception) {
            JobResult.Failure(exception.toJobError())
        }
    }

    override suspend fun updateEmployerJob(jobId: String, job: Job): JobResult<Job> {
        val uid = firebaseAuth.currentUser?.uid
            ?: return JobResult.Failure(JobError.NOT_AUTHENTICATED)
        if (jobId.isBlank()) return JobResult.Failure(JobError.JOB_NOT_FOUND)

        return try {
            val accountError = employerAccountError(uid)
            if (accountError != null) return JobResult.Failure(accountError)

            val jobDocument = firestore.collection(JOBS_COLLECTION).document(jobId)
            val existingDocument = jobDocument.get().await()
            val existingJob = existingDocument.toJobOrNull()
            if (existingJob == null || existingJob.employerId != uid) {
                return JobResult.Failure(JobError.JOB_NOT_FOUND)
            }

            val companyDocument = firestore.collection(COMPANIES_COLLECTION).document(uid).get().await()
            val companyName = companyDocument.getString(FIELD_COMPANY_NAME)
                ?.trim()
                .orEmpty()
                .ifBlank { existingJob.companyName }
            val updatedJob = job.copy(
                id = jobId,
                employerId = uid,
                companyName = companyName,
                createdAt = existingJob.createdAt,
                updatedAt = null,
                active = existingJob.active,
                applicantCount = existingJob.applicantCount,
            )
            jobDocument.update(updatedJob.toEditableFirestoreData()).await()
            JobResult.Success(updatedJob)
        } catch (exception: Exception) {
            JobResult.Failure(exception.toJobError())
        }
    }

    override suspend fun setEmployerJobActive(
        jobId: String,
        active: Boolean,
    ): JobResult<Job> {
        val uid = firebaseAuth.currentUser?.uid
            ?: return JobResult.Failure(JobError.NOT_AUTHENTICATED)
        if (jobId.isBlank()) return JobResult.Failure(JobError.JOB_NOT_FOUND)

        return try {
            val accountError = employerAccountError(uid)
            if (accountError != null) return JobResult.Failure(accountError)

            val jobDocument = firestore.collection(JOBS_COLLECTION).document(jobId)
            val document = jobDocument.get().await()
            val job = document.toJobOrNull()
            if (job == null || job.employerId != uid) {
                return JobResult.Failure(JobError.JOB_NOT_FOUND)
            }

            jobDocument.update(
                mapOf(
                    FIELD_ACTIVE to active,
                    FIELD_UPDATED_AT to FieldValue.serverTimestamp(),
                ),
            ).await()
            JobResult.Success(job.copy(active = active, updatedAt = null))
        } catch (exception: Exception) {
            JobResult.Failure(exception.toJobError())
        }
    }

    override suspend fun getActiveJobs(): JobResult<List<Job>> {
        Log.d(
            TAG,
            "Firestore query: jobs where active == true, limit $JOB_FEED_LIMIT; " +
                "sorted locally by createdAt/updatedAt descending",
        )
        return try {
            val documents = firestore.collection(JOBS_COLLECTION)
                .whereEqualTo(FIELD_ACTIVE, true)
                .limit(JOB_FEED_LIMIT)
                .get()
                .await()
            val jobs = documents.documents
                .mapNotNull { document -> document.toJobOrNull() }
                .sortedByDescending { job ->
                    job.createdAt?.seconds ?: job.updatedAt?.seconds ?: Long.MIN_VALUE
                }
            JobResult.Success(jobs)
        } catch (exception: Exception) {
            logFirebaseFailure("load active jobs", exception)
            JobResult.Failure(exception.toJobError())
        }
    }

    override suspend fun getJob(jobId: String): JobResult<Job> {
        if (jobId.isBlank()) return JobResult.Failure(JobError.JOB_NOT_FOUND)

        return try {
            val document = firestore.collection(JOBS_COLLECTION).document(jobId).get().await()
            val job = document.toJobOrNull()
            if (job == null || !job.active) {
                JobResult.Failure(JobError.JOB_NOT_FOUND)
            } else {
                JobResult.Success(job)
            }
        } catch (exception: Exception) {
            logFirebaseFailure("load job details", exception)
            JobResult.Failure(exception.toJobError())
        }
    }

    override suspend fun getSavedJobs(): JobResult<List<Job>> {
        val uid = firebaseAuth.currentUser?.uid
            ?: return JobResult.Failure(JobError.NOT_AUTHENTICATED)

        return try {
            val accountError = jobSeekerAccountError(uid)
            if (accountError != null) return JobResult.Failure(accountError)

            val documents = savedJobsCollection(uid)
                .orderBy(FIELD_SAVED_AT, Query.Direction.DESCENDING)
                .get()
                .await()
            JobResult.Success(
                documents.documents.mapNotNull { document ->
                    document.toSavedJobOrLegacyJob()
                },
            )
        } catch (exception: Exception) {
            logFirebaseFailure("load saved jobs", exception)
            JobResult.Failure(exception.toJobError())
        }
    }

    override suspend fun isJobSaved(jobId: String): JobResult<Boolean> {
        val uid = firebaseAuth.currentUser?.uid
            ?: return JobResult.Failure(JobError.NOT_AUTHENTICATED)
        if (jobId.isBlank()) return JobResult.Failure(JobError.JOB_NOT_FOUND)

        return try {
            val accountError = jobSeekerAccountError(uid)
            if (accountError != null) return JobResult.Failure(accountError)

            JobResult.Success(savedJobsCollection(uid).document(jobId).get().await().exists())
        } catch (exception: Exception) {
            JobResult.Failure(exception.toJobError())
        }
    }

    override suspend fun saveJob(jobId: String): JobResult<Unit> {
        val uid = firebaseAuth.currentUser?.uid
            ?: return JobResult.Failure(JobError.NOT_AUTHENTICATED)
        if (jobId.isBlank()) return JobResult.Failure(JobError.JOB_NOT_FOUND)

        return try {
            val accountError = jobSeekerAccountError(uid)
            if (accountError != null) return JobResult.Failure(accountError)

            val jobDocument = firestore.collection(JOBS_COLLECTION).document(jobId).get().await()
            val job = jobDocument.toJobOrNull()
            if (job == null || !job.active) {
                return JobResult.Failure(JobError.JOB_NOT_FOUND)
            }

            savedJobsCollection(uid)
                .document(jobId)
                .set(
                    mapOf(
                        FIELD_JOB_ID to jobId,
                        FIELD_JOB to job.toSavedJobData(),
                        FIELD_SAVED_AT to FieldValue.serverTimestamp(),
                    ),
                )
                .await()
            JobResult.Success(Unit)
        } catch (exception: Exception) {
            JobResult.Failure(exception.toJobError())
        }
    }

    override suspend fun unsaveJob(jobId: String): JobResult<Unit> {
        val uid = firebaseAuth.currentUser?.uid
            ?: return JobResult.Failure(JobError.NOT_AUTHENTICATED)
        if (jobId.isBlank()) return JobResult.Failure(JobError.JOB_NOT_FOUND)

        return try {
            val accountError = jobSeekerAccountError(uid)
            if (accountError != null) return JobResult.Failure(accountError)

            savedJobsCollection(uid).document(jobId).delete().await()
            JobResult.Success(Unit)
        } catch (exception: Exception) {
            JobResult.Failure(exception.toJobError())
        }
    }

    override suspend fun postJob(job: Job): JobResult<Job> {
        val uid = firebaseAuth.currentUser?.uid
            ?: return JobResult.Failure(JobError.NOT_AUTHENTICATED)

        return try {
            val userDocument = firestore.collection(USERS_COLLECTION).document(uid).get().await()
            val accountError = userDocument.employerAccountError()
            if (accountError != null) return JobResult.Failure(accountError)

            val companyDocument = firestore.collection(COMPANIES_COLLECTION).document(uid).get().await()
            val companyName = companyDocument.getString(FIELD_COMPANY_NAME)?.trim().orEmpty()
            if (companyName.isEmpty()) {
                return JobResult.Failure(JobError.COMPANY_PROFILE_REQUIRED)
            }

            val jobDocument = firestore.collection(JOBS_COLLECTION).document()
            val savedJob = job.copy(
                id = jobDocument.id,
                employerId = uid,
                companyName = companyName,
                createdAt = null,
                updatedAt = null,
            )
            jobDocument.set(savedJob.toFirestoreData()).await()
            JobResult.Success(savedJob)
        } catch (exception: Exception) {
            JobResult.Failure(exception.toJobError())
        }
    }

    private fun DocumentSnapshot.employerAccountError(): JobError? = when {
        !exists() -> JobError.ACCOUNT_NOT_FOUND
        getString(FIELD_ROLE) != UserRole.EMPLOYER.name -> JobError.WRONG_ROLE
        else -> null
    }

    private suspend fun employerAccountError(uid: String): JobError? = firestore
        .collection(USERS_COLLECTION)
        .document(uid)
        .get()
        .await()
        .employerAccountError()

    private suspend fun jobSeekerAccountError(uid: String): JobError? {
        val document = firestore.collection(USERS_COLLECTION).document(uid).get().await()
        return when {
            !document.exists() -> JobError.ACCOUNT_NOT_FOUND
            document.getString(FIELD_ROLE) != UserRole.JOB_SEEKER.name -> JobError.WRONG_ROLE
            else -> null
        }
    }

    private fun savedJobsCollection(uid: String) = firestore.collection(USERS_COLLECTION)
        .document(uid)
        .collection(SAVED_JOBS_COLLECTION)

    private suspend fun DocumentSnapshot.toSavedJobOrLegacyJob(): Job? {
        get(FIELD_JOB, Job::class.java)?.let { return it.copy(id = id) }

        // Saved-job references created by earlier app versions did not contain a snapshot.
        val legacyJobId = getString(FIELD_JOB_ID).orEmpty().ifBlank { id }
        val legacyJob = firestore.collection(JOBS_COLLECTION).document(legacyJobId).get().await()
        return legacyJob.toJobOrNull()
    }

    private fun DocumentSnapshot.toJobOrNull(): Job? = try {
        val title = getString(FIELD_TITLE).orEmpty().trim()
        if (title.isBlank()) {
            Log.w(TAG, "Skipping job $id because title is missing or blank")
            return null
        }
        Job(
            id = id,
            employerId = getString(FIELD_EMPLOYER_ID).orEmpty(),
            companyName = getString(FIELD_COMPANY_NAME).orEmpty(),
            title = title,
            description = getString(FIELD_DESCRIPTION).orEmpty(),
            category = getString(FIELD_CATEGORY).orEmpty(),
            location = getString(FIELD_LOCATION).orEmpty(),
            workMode = enumValueOrDefault(FIELD_WORK_MODE, WorkMode.ONSITE),
            jobType = enumValueOrDefault(FIELD_JOB_TYPE, JobType.FULL_TIME),
            salaryMin = getNumberAsLong(FIELD_SALARY_MIN),
            salaryMax = getNumberAsLong(FIELD_SALARY_MAX),
            currency = getString(FIELD_CURRENCY).orEmpty(),
            experienceLevel = getString(FIELD_EXPERIENCE_LEVEL).orEmpty(),
            requiredSkills = getStringList(FIELD_REQUIRED_SKILLS),
            requirements = getStringList(FIELD_REQUIREMENTS),
            benefits = getStringList(FIELD_BENEFITS),
            applicationDeadline = get(FIELD_APPLICATION_DEADLINE) as? Timestamp,
            createdAt = get(FIELD_CREATED_AT) as? Timestamp,
            updatedAt = get(FIELD_UPDATED_AT) as? Timestamp,
            active = getBoolean(FIELD_ACTIVE) ?: true,
            applicantCount = getNumberAsLong(FIELD_APPLICANT_COUNT) ?: 0L,
        )
    } catch (exception: Exception) {
        Log.e(TAG, "Skipping malformed job $id without failing the feed", exception)
        null
    }

    private inline fun <reified T : Enum<T>> DocumentSnapshot.enumValueOrDefault(
        field: String,
        default: T,
    ): T {
        val rawValue = getString(field)
            ?.trim()
            ?.uppercase()
            ?.replace(Regex("[^A-Z0-9]+"), "_")
            .orEmpty()
        return enumValues<T>().firstOrNull { it.name == rawValue } ?: default
    }

    private fun DocumentSnapshot.getNumberAsLong(field: String): Long? =
        (get(field) as? Number)?.toLong()

    private fun DocumentSnapshot.getStringList(field: String): List<String> =
        (get(field) as? List<*>)
            .orEmpty()
            .filterIsInstance<String>()

    private fun logFirebaseFailure(operation: String, exception: Exception) {
        val firestoreException = generateSequence<Throwable>(exception) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreException != null) {
            Log.e(
                TAG,
                "Firestore failed to $operation: code=${firestoreException.code}, " +
                    "message=${firestoreException.message}",
                firestoreException,
            )
        } else {
            Log.e(TAG, "Failed to $operation", exception)
        }
    }

    private fun Job.toFirestoreData(): Map<String, Any?> = mapOf(
        FIELD_ID to id,
        FIELD_EMPLOYER_ID to employerId,
        FIELD_COMPANY_NAME to companyName,
        FIELD_TITLE to title,
        FIELD_DESCRIPTION to description,
        FIELD_CATEGORY to category,
        FIELD_LOCATION to location,
        FIELD_WORK_MODE to workMode.name,
        FIELD_JOB_TYPE to jobType.name,
        FIELD_SALARY_MIN to salaryMin,
        FIELD_SALARY_MAX to salaryMax,
        FIELD_CURRENCY to currency,
        FIELD_EXPERIENCE_LEVEL to experienceLevel,
        FIELD_REQUIRED_SKILLS to requiredSkills,
        FIELD_REQUIREMENTS to requirements,
        FIELD_BENEFITS to benefits,
        FIELD_APPLICATION_DEADLINE to applicationDeadline,
        FIELD_CREATED_AT to FieldValue.serverTimestamp(),
        FIELD_UPDATED_AT to FieldValue.serverTimestamp(),
        FIELD_ACTIVE to active,
        FIELD_APPLICANT_COUNT to applicantCount,
    )

    private fun Job.toEditableFirestoreData(): Map<String, Any?> = mapOf(
        FIELD_COMPANY_NAME to companyName,
        FIELD_TITLE to title,
        FIELD_DESCRIPTION to description,
        FIELD_CATEGORY to category,
        FIELD_LOCATION to location,
        FIELD_WORK_MODE to workMode.name,
        FIELD_JOB_TYPE to jobType.name,
        FIELD_SALARY_MIN to salaryMin,
        FIELD_SALARY_MAX to salaryMax,
        FIELD_CURRENCY to currency,
        FIELD_EXPERIENCE_LEVEL to experienceLevel,
        FIELD_REQUIRED_SKILLS to requiredSkills,
        FIELD_REQUIREMENTS to requirements,
        FIELD_BENEFITS to benefits,
        FIELD_APPLICATION_DEADLINE to applicationDeadline,
        FIELD_UPDATED_AT to FieldValue.serverTimestamp(),
    )

    private fun Job.toSavedJobData(): Map<String, Any?> = mapOf(
        FIELD_ID to id,
        FIELD_EMPLOYER_ID to employerId,
        FIELD_COMPANY_NAME to companyName,
        FIELD_TITLE to title,
        FIELD_DESCRIPTION to description,
        FIELD_CATEGORY to category,
        FIELD_LOCATION to location,
        FIELD_WORK_MODE to workMode.name,
        FIELD_JOB_TYPE to jobType.name,
        FIELD_SALARY_MIN to salaryMin,
        FIELD_SALARY_MAX to salaryMax,
        FIELD_CURRENCY to currency,
        FIELD_EXPERIENCE_LEVEL to experienceLevel,
        FIELD_REQUIRED_SKILLS to requiredSkills,
        FIELD_REQUIREMENTS to requirements,
        FIELD_BENEFITS to benefits,
        FIELD_APPLICATION_DEADLINE to applicationDeadline,
        FIELD_CREATED_AT to createdAt,
        FIELD_UPDATED_AT to updatedAt,
        FIELD_ACTIVE to active,
        FIELD_APPLICANT_COUNT to applicantCount,
    )

    private fun Exception.toJobError(): JobError = when (this) {
        is FirebaseFirestoreException -> when (code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> JobError.PERMISSION_DENIED
            FirebaseFirestoreException.Code.UNAVAILABLE,
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
            -> JobError.NETWORK

            else -> JobError.UNKNOWN
        }

        else -> JobError.UNKNOWN
    }

    private companion object {
        const val TAG = "JobLinkFirestore"
        const val USERS_COLLECTION = "users"
        const val COMPANIES_COLLECTION = "companies"
        const val JOBS_COLLECTION = "jobs"
        const val SAVED_JOBS_COLLECTION = "savedJobs"
        const val JOB_FEED_LIMIT = 50L
        const val FIELD_ROLE = "role"
        const val FIELD_ID = "id"
        const val FIELD_EMPLOYER_ID = "employerId"
        const val FIELD_COMPANY_NAME = "companyName"
        const val FIELD_TITLE = "title"
        const val FIELD_DESCRIPTION = "description"
        const val FIELD_CATEGORY = "category"
        const val FIELD_LOCATION = "location"
        const val FIELD_WORK_MODE = "workMode"
        const val FIELD_JOB_TYPE = "jobType"
        const val FIELD_SALARY_MIN = "salaryMin"
        const val FIELD_SALARY_MAX = "salaryMax"
        const val FIELD_CURRENCY = "currency"
        const val FIELD_EXPERIENCE_LEVEL = "experienceLevel"
        const val FIELD_REQUIRED_SKILLS = "requiredSkills"
        const val FIELD_REQUIREMENTS = "requirements"
        const val FIELD_BENEFITS = "benefits"
        const val FIELD_APPLICATION_DEADLINE = "applicationDeadline"
        const val FIELD_CREATED_AT = "createdAt"
        const val FIELD_UPDATED_AT = "updatedAt"
        const val FIELD_ACTIVE = "active"
        const val FIELD_APPLICANT_COUNT = "applicantCount"
        const val FIELD_JOB_ID = "jobId"
        const val FIELD_JOB = "job"
        const val FIELD_SAVED_AT = "savedAt"
    }
}
