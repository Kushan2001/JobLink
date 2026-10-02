package com.kushan.joblink.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.kushan.joblink.data.model.ApplicationDraft
import com.kushan.joblink.data.model.ApplicationStatus
import com.kushan.joblink.data.model.EmployerApplicationsData
import com.kushan.joblink.data.model.Job
import com.kushan.joblink.data.model.JobApplication
import com.kushan.joblink.data.model.UserRole
import com.kushan.joblink.data.model.employerApplicationStatuses
import java.io.File
import kotlinx.coroutines.tasks.await

class FirebaseApplicationRepository(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance(),
    private val cacheDirectory: File? = null,
) : ApplicationRepository {

    override suspend fun getRecentEmployerApplications(
        limit: Long,
    ): ApplicationResult<List<JobApplication>> {
        val employerId = firebaseAuth.currentUser?.uid
            ?: return ApplicationResult.Failure(ApplicationError.NOT_AUTHENTICATED)
        if (limit <= 0L) return ApplicationResult.Success(emptyList())

        return try {
            val profileError = userDocument(employerId).get().await().employerProfileError()
            if (profileError != null) return ApplicationResult.Failure(profileError)

            val applications = firestore.collection(APPLICATIONS_COLLECTION)
                .whereEqualTo(FIELD_EMPLOYER_ID, employerId)
                .orderBy(FIELD_SUBMITTED_AT, Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .await()
                .documents
                .mapNotNull { document ->
                    document.toObject(JobApplication::class.java)?.copy(
                        applicationId = document.id,
                    )
                }
            ApplicationResult.Success(applications)
        } catch (exception: Exception) {
            ApplicationResult.Failure(exception.toApplicationError())
        }
    }

    override suspend fun getEmployerApplications(
        jobId: String,
    ): ApplicationResult<EmployerApplicationsData> {
        val employerId = firebaseAuth.currentUser?.uid
            ?: return ApplicationResult.Failure(ApplicationError.NOT_AUTHENTICATED)
        if (jobId.isBlank()) return ApplicationResult.Failure(ApplicationError.JOB_NOT_FOUND)

        return try {
            val profileError = userDocument(employerId).get().await().employerProfileError()
            if (profileError != null) return ApplicationResult.Failure(profileError)

            val jobSnapshot = jobDocument(jobId).get().await()
            val job = jobSnapshot.toObject(Job::class.java)?.copy(id = jobSnapshot.id)
            if (job == null || job.employerId != employerId) {
                return ApplicationResult.Failure(ApplicationError.JOB_NOT_FOUND)
            }

            val applications = firestore.collection(APPLICATIONS_COLLECTION)
                .whereEqualTo(FIELD_JOB_ID, jobId)
                .whereEqualTo(FIELD_EMPLOYER_ID, employerId)
                .get()
                .await()
                .documents
                .mapNotNull { document ->
                    document.toObject(JobApplication::class.java)?.copy(
                        applicationId = document.id,
                    )
                }
                .filter { it.employerId == employerId }
                .sortedByDescending { it.submittedAt?.seconds ?: Long.MIN_VALUE }
            ApplicationResult.Success(
                EmployerApplicationsData(
                    jobId = job.id,
                    jobTitle = job.title,
                    applications = applications,
                ),
            )
        } catch (exception: Exception) {
            ApplicationResult.Failure(exception.toApplicationError())
        }
    }

    override suspend fun getEmployerApplication(
        applicationId: String,
    ): ApplicationResult<JobApplication> {
        val employerId = firebaseAuth.currentUser?.uid
            ?: return ApplicationResult.Failure(ApplicationError.NOT_AUTHENTICATED)
        if (applicationId.isBlank()) {
            return ApplicationResult.Failure(ApplicationError.APPLICATION_NOT_FOUND)
        }

        return try {
            val profileError = userDocument(employerId).get().await().employerProfileError()
            if (profileError != null) return ApplicationResult.Failure(profileError)

            val applicationSnapshot = firestore.collection(APPLICATIONS_COLLECTION)
                .document(applicationId)
                .get()
                .await()
            val application = applicationSnapshot.toObject(JobApplication::class.java)?.copy(
                applicationId = applicationSnapshot.id,
            )
            if (application == null || application.employerId != employerId) {
                return ApplicationResult.Failure(ApplicationError.APPLICATION_NOT_FOUND)
            }

            val jobSnapshot = jobDocument(application.jobId).get().await()
            val job = jobSnapshot.toObject(Job::class.java)?.copy(id = jobSnapshot.id)
            if (job == null || job.employerId != employerId) {
                ApplicationResult.Failure(ApplicationError.APPLICATION_NOT_FOUND)
            } else {
                ApplicationResult.Success(application)
            }
        } catch (exception: Exception) {
            ApplicationResult.Failure(exception.toApplicationError())
        }
    }

    override suspend fun updateEmployerApplicationStatus(
        applicationId: String,
        status: ApplicationStatus,
    ): ApplicationResult<JobApplication> {
        val employerId = firebaseAuth.currentUser?.uid
            ?: return ApplicationResult.Failure(ApplicationError.NOT_AUTHENTICATED)
        if (applicationId.isBlank()) {
            return ApplicationResult.Failure(ApplicationError.APPLICATION_NOT_FOUND)
        }
        if (status !in employerApplicationStatuses) {
            return ApplicationResult.Failure(ApplicationError.INVALID_STATUS)
        }

        return try {
            val profileError = userDocument(employerId).get().await().employerProfileError()
            if (profileError != null) return ApplicationResult.Failure(profileError)

            val applicationReference = firestore.collection(APPLICATIONS_COLLECTION)
                .document(applicationId)
            val updatedApplication = firestore.runTransaction { transaction ->
                val applicationSnapshot = transaction.get(applicationReference)
                val application = applicationSnapshot.toObject(JobApplication::class.java)?.copy(
                    applicationId = applicationSnapshot.id,
                ) ?: throw ApplicationRepositoryException(ApplicationError.APPLICATION_NOT_FOUND)
                if (application.employerId != employerId) {
                    throw ApplicationRepositoryException(ApplicationError.APPLICATION_NOT_FOUND)
                }

                val jobSnapshot = transaction.get(jobDocument(application.jobId))
                val job = jobSnapshot.toObject(Job::class.java)?.copy(id = jobSnapshot.id)
                if (job == null || job.employerId != employerId) {
                    throw ApplicationRepositoryException(ApplicationError.APPLICATION_NOT_FOUND)
                }

                transaction.update(applicationReference, FIELD_STATUS, status.name)
                application.copy(status = status)
            }.await()
            ApplicationResult.Success(updatedApplication)
        } catch (exception: Exception) {
            ApplicationResult.Failure(exception.toApplicationError())
        }
    }

    override suspend fun downloadApplicantCv(
        applicationId: String,
        onProgress: (Float) -> Unit,
    ): ApplicationResult<File> {
        val application = when (val result = getEmployerApplication(applicationId)) {
            is ApplicationResult.Success -> result.value
            is ApplicationResult.Failure -> return result
        }
        val expectedPrefix = "users/${application.applicantId}/cv/"
        if (
            application.cvReference.isBlank() ||
            !application.cvReference.startsWith(expectedPrefix)
        ) {
            return ApplicationResult.Failure(ApplicationError.CV_NOT_AVAILABLE)
        }
        val baseDirectory = cacheDirectory
            ?: return ApplicationResult.Failure(ApplicationError.CV_DOWNLOAD_FAILED)

        return try {
            val cvDirectory = File(baseDirectory, CV_CACHE_DIRECTORY)
            if (!cvDirectory.exists() && !cvDirectory.mkdirs()) {
                return ApplicationResult.Failure(ApplicationError.CV_DOWNLOAD_FAILED)
            }
            val safeApplicationId = applicationId
                .filter { it.isLetterOrDigit() || it == '-' || it == '_' }
                .ifBlank { "application" }
            val destination = File(cvDirectory, "$safeApplicationId.pdf")
            val downloadTask = storage.reference
                .child(application.cvReference)
                .getFile(destination)
            downloadTask.addOnProgressListener { snapshot ->
                if (snapshot.totalByteCount > 0L) {
                    onProgress(
                        (snapshot.bytesTransferred.toFloat() / snapshot.totalByteCount.toFloat())
                            .coerceIn(0f, 1f),
                    )
                }
            }
            downloadTask.await()
            onProgress(1f)
            ApplicationResult.Success(destination)
        } catch (exception: Exception) {
            ApplicationResult.Failure(exception.toApplicationError())
        }
    }

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
                    applicantFullName = profileDocument.getString(FIELD_FULL_NAME).orEmpty(),
                    applicantEmail = profileDocument.getString(FIELD_EMAIL).orEmpty(),
                    applicantHeadline = profileDocument.getString(FIELD_HEADLINE).orEmpty(),
                    applicantLocation = profileDocument.getString(FIELD_LOCATION).orEmpty(),
                    applicantPhone = profileDocument.getString(FIELD_PHONE).orEmpty(),
                    applicantBio = profileDocument.getString(FIELD_BIO).orEmpty(),
                    applicantEducation = profileDocument.getString(FIELD_EDUCATION).orEmpty(),
                    applicantExperienceSummary = profileDocument
                        .getString(FIELD_EXPERIENCE_SUMMARY)
                        .orEmpty(),
                    applicantSkills = profileDocument.getStringList(FIELD_SKILLS),
                    status = ApplicationStatus.SUBMITTED,
                    coverMessage = coverMessage.trim().takeIf(String::isNotEmpty),
                    cvReference = cvReference,
                )
                transaction.set(applicationReference, submittedApplication.toFirestoreData())
                transaction.set(
                    cvAccessDocument(job.employerId, applicantId),
                    mapOf(
                        FIELD_EMPLOYER_ID to job.employerId,
                        FIELD_APPLICANT_ID to applicantId,
                        FIELD_JOB_ID to job.id,
                        FIELD_APPLICATION_ID to applicationReference.id,
                        FIELD_GRANTED_AT to FieldValue.serverTimestamp(),
                    ),
                )
                transaction.update(
                    jobReference,
                    FIELD_APPLICANT_COUNT,
                    FieldValue.increment(1),
                )
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

    private fun cvAccessDocument(employerId: String, applicantId: String) = firestore
        .collection(CV_ACCESS_COLLECTION)
        .document("${employerId}_$applicantId")

    private fun DocumentSnapshot.jobSeekerProfileError(): ApplicationError? = when {
        !exists() -> ApplicationError.PROFILE_NOT_FOUND
        getString(FIELD_ROLE) != UserRole.JOB_SEEKER.name -> ApplicationError.WRONG_ROLE
        else -> null
    }

    private fun DocumentSnapshot.employerProfileError(): ApplicationError? = when {
        !exists() -> ApplicationError.PROFILE_NOT_FOUND
        getString(FIELD_ROLE) != UserRole.EMPLOYER.name -> ApplicationError.WRONG_ROLE
        else -> null
    }

    private fun DocumentSnapshot.getStringList(field: String): List<String> =
        (get(field) as? List<*>)
            .orEmpty()
            .filterIsInstance<String>()

    private fun JobApplication.toFirestoreData(): Map<String, Any?> = mapOf(
        FIELD_APPLICATION_ID to applicationId,
        FIELD_JOB_ID to jobId,
        FIELD_EMPLOYER_ID to employerId,
        FIELD_APPLICANT_ID to applicantId,
        FIELD_JOB_TITLE to jobTitle,
        FIELD_COMPANY_NAME to companyName,
        FIELD_APPLICANT_FULL_NAME to applicantFullName,
        FIELD_APPLICANT_EMAIL to applicantEmail,
        FIELD_APPLICANT_HEADLINE to applicantHeadline,
        FIELD_APPLICANT_LOCATION to applicantLocation,
        FIELD_APPLICANT_PHONE to applicantPhone,
        FIELD_APPLICANT_BIO to applicantBio,
        FIELD_APPLICANT_EDUCATION to applicantEducation,
        FIELD_APPLICANT_EXPERIENCE_SUMMARY to applicantExperienceSummary,
        FIELD_APPLICANT_SKILLS to applicantSkills,
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
                is StorageException -> return throwable.toApplicationError()
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

    private fun StorageException.toApplicationError(): ApplicationError = when (errorCode) {
        StorageException.ERROR_NOT_AUTHENTICATED -> ApplicationError.NOT_AUTHENTICATED
        StorageException.ERROR_NOT_AUTHORIZED -> ApplicationError.PERMISSION_DENIED
        StorageException.ERROR_OBJECT_NOT_FOUND -> ApplicationError.CV_NOT_AVAILABLE
        StorageException.ERROR_RETRY_LIMIT_EXCEEDED -> ApplicationError.NETWORK
        else -> ApplicationError.CV_DOWNLOAD_FAILED
    }

    private class ApplicationRepositoryException(
        val error: ApplicationError,
    ) : Exception()

    private companion object {
        const val USERS_COLLECTION = "users"
        const val JOBS_COLLECTION = "jobs"
        const val APPLICATIONS_COLLECTION = "applications"
        const val CV_ACCESS_COLLECTION = "cvAccess"
        const val CV_CACHE_DIRECTORY = "applicant_cvs"
        const val FIELD_ROLE = "role"
        const val FIELD_CV_STORAGE_PATH = "cv.storagePath"
        const val FIELD_CV_FILE_NAME = "cv.fileName"
        const val FIELD_FULL_NAME = "fullName"
        const val FIELD_EMAIL = "email"
        const val FIELD_HEADLINE = "professionalHeadline"
        const val FIELD_LOCATION = "location"
        const val FIELD_PHONE = "phone"
        const val FIELD_BIO = "bio"
        const val FIELD_EDUCATION = "education"
        const val FIELD_EXPERIENCE_SUMMARY = "experienceSummary"
        const val FIELD_SKILLS = "skills"
        const val FIELD_APPLICATION_ID = "applicationId"
        const val FIELD_JOB_ID = "jobId"
        const val FIELD_EMPLOYER_ID = "employerId"
        const val FIELD_APPLICANT_ID = "applicantId"
        const val FIELD_JOB_TITLE = "jobTitle"
        const val FIELD_COMPANY_NAME = "companyName"
        const val FIELD_APPLICANT_FULL_NAME = "applicantFullName"
        const val FIELD_APPLICANT_EMAIL = "applicantEmail"
        const val FIELD_APPLICANT_HEADLINE = "applicantHeadline"
        const val FIELD_APPLICANT_LOCATION = "applicantLocation"
        const val FIELD_APPLICANT_PHONE = "applicantPhone"
        const val FIELD_APPLICANT_BIO = "applicantBio"
        const val FIELD_APPLICANT_EDUCATION = "applicantEducation"
        const val FIELD_APPLICANT_EXPERIENCE_SUMMARY = "applicantExperienceSummary"
        const val FIELD_APPLICANT_SKILLS = "applicantSkills"
        const val FIELD_SUBMITTED_AT = "submittedAt"
        const val FIELD_STATUS = "status"
        const val FIELD_COVER_MESSAGE = "coverMessage"
        const val FIELD_CV_REFERENCE = "cvReference"
        const val FIELD_APPLICANT_COUNT = "applicantCount"
        const val FIELD_GRANTED_AT = "grantedAt"
    }
}
