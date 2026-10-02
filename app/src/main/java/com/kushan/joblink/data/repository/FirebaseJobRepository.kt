package com.kushan.joblink.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.kushan.joblink.data.model.Job
import com.kushan.joblink.data.model.UserRole
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
                .mapNotNull { document ->
                    document.toObject(Job::class.java)?.copy(id = document.id)
                }
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
            val job = document.toObject(Job::class.java)?.copy(id = document.id)
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
            val existingJob = existingDocument.toObject(Job::class.java)?.copy(id = jobId)
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
            val job = document.toObject(Job::class.java)?.copy(id = document.id)
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

    override suspend fun getActiveJobs(): JobResult<List<Job>> = try {
        val documents = firestore.collection(JOBS_COLLECTION)
            .whereEqualTo(FIELD_ACTIVE, true)
            .orderBy(FIELD_CREATED_AT, Query.Direction.DESCENDING)
            .limit(JOB_FEED_LIMIT)
            .get()
            .await()
        JobResult.Success(
            documents.documents.map { document ->
                document.toObject(Job::class.java)?.copy(id = document.id)
                    ?: throw IllegalStateException("Unable to read job ${document.id}")
            },
        )
    } catch (exception: Exception) {
        JobResult.Failure(exception.toJobError())
    }

    override suspend fun getJob(jobId: String): JobResult<Job> {
        if (jobId.isBlank()) return JobResult.Failure(JobError.JOB_NOT_FOUND)

        return try {
            val document = firestore.collection(JOBS_COLLECTION).document(jobId).get().await()
            val job = document.toObject(Job::class.java)?.copy(id = document.id)
            if (job == null || !job.active) {
                JobResult.Failure(JobError.JOB_NOT_FOUND)
            } else {
                JobResult.Success(job)
            }
        } catch (exception: Exception) {
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
            val job = jobDocument.toObject(Job::class.java)?.copy(id = jobDocument.id)
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
        return legacyJob.toObject(Job::class.java)?.copy(id = legacyJob.id)
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
