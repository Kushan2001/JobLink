package com.kushan.joblink.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.kushan.joblink.data.model.Job
import com.kushan.joblink.data.model.UserRole
import kotlinx.coroutines.tasks.await

class FirebaseJobRepository(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : JobRepository {

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
    }
}
