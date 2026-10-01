package com.kushan.joblink.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.kushan.joblink.data.model.ApplicationDraft
import com.kushan.joblink.data.model.ApplicationStatus
import com.kushan.joblink.data.model.Job
import com.kushan.joblink.data.model.JobApplication
import com.kushan.joblink.data.model.UserRole
import kotlinx.coroutines.tasks.await

class FirebaseApplicationRepository(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : ApplicationRepository {

    override suspend fun getMyApplications(): ApplicationResult<List<JobApplication>> {
        val applicantId = firebaseAuth.currentUser?.uid
            ?: return ApplicationResult.Failure(ApplicationError.NOT_AUTHENTICATED)

        return try {
            val profileError = userDocument(applicantId).get().await().jobSeekerProfileError()
            if (profileError != null) return ApplicationResult.Failure(profileError)

            val applications = firestore.collection(APPLICATIONS_COLLECTION)
                .whereEqualTo(FIELD_APPLICANT_ID, applicantId)
                .get()
                .await()
                .documents
                .mapNotNull { document ->
                    document.toObject(JobApplication::class.java)?.copy(
                        applicationId = document.id,
                    )
                }
                .sortedByDescending { it.submittedAt?.seconds ?: Long.MIN_VALUE }
            ApplicationResult.Success(applications)
        } catch (exception: Exception) {
            ApplicationResult.Failure(exception.toApplicationError())
        }
    }

    override suspend fun getMyApplication(
        applicationId: String,
    ): ApplicationResult<JobApplication> {
        val applicantId = firebaseAuth.currentUser?.uid
            ?: return ApplicationResult.Failure(ApplicationError.NOT_AUTHENTICATED)
        if (applicationId.isBlank()) {
            return ApplicationResult.Failure(ApplicationError.APPLICATION_NOT_FOUND)
        }

        return try {
            val profileError = userDocument(applicantId).get().await().jobSeekerProfileError()
            if (profileError != null) return ApplicationResult.Failure(profileError)

            val document = firestore.collection(APPLICATIONS_COLLECTION)
                .document(applicationId)
                .get()
                .await()
            val application = document.toObject(JobApplication::class.java)?.copy(
                applicationId = document.id,
            )
            if (application == null || application.applicantId != applicantId) {
                ApplicationResult.Failure(ApplicationError.APPLICATION_NOT_FOUND)
            } else {
                ApplicationResult.Success(application)
            }
        } catch (exception: Exception) {
            ApplicationResult.Failure(exception.toApplicationError())
        }
    }

    override suspend fun getApplicationDraft(
        jobId: String,
    ): ApplicationResult<ApplicationDraft> {
        val applicantId = firebaseAuth.currentUser?.uid
            ?: return ApplicationResult.Failure(ApplicationError.NOT_AUTHENTICATED)
        if (jobId.isBlank()) return ApplicationResult.Failure(ApplicationError.JOB_NOT_FOUND)

        return try {
            val profileDocument = userDocument(applicantId).get().await()
            val profileError = profileDocument.jobSeekerProfileError()
            if (profileError != null) return ApplicationResult.Failure(profileError)

            val cvReference = profileDocument.getString(FIELD_CV_STORAGE_PATH).orEmpty()
            val cvFileName = profileDocument.getString(FIELD_CV_FILE_NAME).orEmpty()
            if (cvReference.isBlank()) {
                return ApplicationResult.Failure(ApplicationError.CV_REQUIRED)
            }

            val jobDocument = jobDocument(jobId).get().await()
            val job = jobDocument.toObject(Job::class.java)?.copy(id = jobDocument.id)
            if (job == null || !job.active || job.employerId.isBlank()) {
                return ApplicationResult.Failure(ApplicationError.JOB_NOT_FOUND)
            }

            if (applicationDocument(jobId, applicantId).get().await().exists()) {
                return ApplicationResult.Failure(ApplicationError.ALREADY_APPLIED)
            }

            ApplicationResult.Success(
                ApplicationDraft(
                    jobId = job.id,
                    jobTitle = job.title,
                    companyName = job.companyName,
                    cvFileName = cvFileName,
                ),
            )
        } catch (exception: Exception) {
            ApplicationResult.Failure(exception.toApplicationError())
        }
    }

    override suspend fun submitApplication(
        jobId: String,
        coverMessage: String,
    ): ApplicationResult<JobApplication> {
        val applicantId = firebaseAuth.currentUser?.uid
            ?: return ApplicationResult.Failure(ApplicationError.NOT_AUTHENTICATED)
        if (jobId.isBlank()) return ApplicationResult.Failure(ApplicationError.JOB_NOT_FOUND)

        return try {
            val userReference = userDocument(applicantId)
            val jobReference = jobDocument(jobId)
            val applicationReference = applicationDocument(jobId, applicantId)
            val application = firestore.runTransaction { transaction ->
                val profileDocument = transaction.get(userReference)
                profileDocument.jobSeekerProfileError()?.let { error ->
                    throw ApplicationRepositoryException(error)
                }
                val cvReference = profileDocument.getString(FIELD_CV_STORAGE_PATH).orEmpty()
                if (cvReference.isBlank()) {
                    throw ApplicationRepositoryException(ApplicationError.CV_REQUIRED)
                }

                val jobSnapshot = transaction.get(jobReference)
                val job = jobSnapshot.toObject(Job::class.java)?.copy(id = jobSnapshot.id)
                if (job == null || !job.active || job.employerId.isBlank()) {
                    throw ApplicationRepositoryException(ApplicationError.JOB_NOT_FOUND)
                }

                if (transaction.get(applicationReference).exists()) {
                    throw ApplicationRepositoryException(ApplicationError.ALREADY_APPLIED)
                }

                val submittedApplication = JobApplication(
                    applicationId = applicationReference.id,
                    jobId = job.id,
                    employerId = job.employerId,
                    applicantId = applicantId,
                    jobTitle = job.title,
                    companyName = job.companyName,
                    status = ApplicationStatus.SUBMITTED,
                    coverMessage = coverMessage.trim().takeIf(String::isNotEmpty),
                    cvReference = cvReference,
                )
                transaction.set(applicationReference, submittedApplication.toFirestoreData())
                submittedApplication
            }.await()
            ApplicationResult.Success(application)
        } catch (exception: Exception) {
            ApplicationResult.Failure(exception.toApplicationError())
        }
    }

    private fun userDocument(uid: String) = firestore.collection(USERS_COLLECTION).document(uid)

    private fun jobDocument(jobId: String) = firestore.collection(JOBS_COLLECTION).document(jobId)

    private fun applicationDocument(jobId: String, applicantId: String) = firestore
        .collection(APPLICATIONS_COLLECTION)
        .document("${jobId}_$applicantId")

    private fun DocumentSnapshot.jobSeekerProfileError(): ApplicationError? = when {
        !exists() -> ApplicationError.PROFILE_NOT_FOUND
        getString(FIELD_ROLE) != UserRole.JOB_SEEKER.name -> ApplicationError.WRONG_ROLE
        else -> null
    }

    private fun JobApplication.toFirestoreData(): Map<String, Any?> = mapOf(
        FIELD_APPLICATION_ID to applicationId,
        FIELD_JOB_ID to jobId,
        FIELD_EMPLOYER_ID to employerId,
        FIELD_APPLICANT_ID to applicantId,
        FIELD_JOB_TITLE to jobTitle,
        FIELD_COMPANY_NAME to companyName,
        FIELD_SUBMITTED_AT to FieldValue.serverTimestamp(),
        FIELD_STATUS to status.name,
        FIELD_COVER_MESSAGE to coverMessage,
        FIELD_CV_REFERENCE to cvReference,
    )

    private fun Exception.toApplicationError(): ApplicationError {
        var throwable: Throwable? = this
        while (throwable != null) {
            when (throwable) {
                is ApplicationRepositoryException -> return throwable.error
                is FirebaseFirestoreException -> return throwable.toApplicationError()
            }
            throwable = throwable.cause
        }
        return ApplicationError.UNKNOWN
    }

    private fun FirebaseFirestoreException.toApplicationError(): ApplicationError = when (code) {
        FirebaseFirestoreException.Code.PERMISSION_DENIED -> {
            ApplicationError.PERMISSION_DENIED
        }

        FirebaseFirestoreException.Code.UNAVAILABLE,
        FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
        -> ApplicationError.NETWORK

        else -> ApplicationError.UNKNOWN
    }

    private class ApplicationRepositoryException(
        val error: ApplicationError,
    ) : Exception()

    private companion object {
        const val USERS_COLLECTION = "users"
        const val JOBS_COLLECTION = "jobs"
        const val APPLICATIONS_COLLECTION = "applications"
        const val FIELD_ROLE = "role"
        const val FIELD_CV_STORAGE_PATH = "cv.storagePath"
        const val FIELD_CV_FILE_NAME = "cv.fileName"
        const val FIELD_APPLICATION_ID = "applicationId"
        const val FIELD_JOB_ID = "jobId"
        const val FIELD_EMPLOYER_ID = "employerId"
        const val FIELD_APPLICANT_ID = "applicantId"
        const val FIELD_JOB_TITLE = "jobTitle"
        const val FIELD_COMPANY_NAME = "companyName"
        const val FIELD_SUBMITTED_AT = "submittedAt"
        const val FIELD_STATUS = "status"
        const val FIELD_COVER_MESSAGE = "coverMessage"
        const val FIELD_CV_REFERENCE = "cvReference"
    }
}
