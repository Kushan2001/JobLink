package com.kushan.joblink.data.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageMetadata
import com.kushan.joblink.data.model.CvMetadata
import com.kushan.joblink.data.model.CvUploadFile
import com.kushan.joblink.data.model.JobSeekerProfile
import com.kushan.joblink.data.model.PDF_CONTENT_TYPE
import com.kushan.joblink.data.model.UserRole
import java.io.FileNotFoundException
import kotlinx.coroutines.tasks.await

class FirebaseJobSeekerProfileRepository(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance(),
) : JobSeekerProfileRepository {

    override suspend fun getProfile(): ProfileResult<JobSeekerProfile> {
        val uid = firebaseAuth.currentUser?.uid
            ?: return ProfileResult.Failure(ProfileError.NOT_AUTHENTICATED)

        return try {
            val document = userDocument(uid).get().await()
            document.toJobSeekerProfile(uid)
        } catch (exception: Exception) {
            ProfileResult.Failure(exception.toProfileError())
        }
    }

    override suspend fun saveProfile(
        profile: JobSeekerProfile,
    ): ProfileResult<JobSeekerProfile> {
        val uid = firebaseAuth.currentUser?.uid
            ?: return ProfileResult.Failure(ProfileError.NOT_AUTHENTICATED)

        return try {
            val documentReference = userDocument(uid)
            val existingDocument = documentReference.get().await()

            if (!existingDocument.exists()) {
                return ProfileResult.Failure(ProfileError.PROFILE_NOT_FOUND)
            }
            if (existingDocument.getString(FIELD_ROLE) != UserRole.JOB_SEEKER.name) {
                return ProfileResult.Failure(ProfileError.WRONG_ROLE)
            }

            val savedProfile = profile.copy(uid = uid)
            documentReference
                .set(savedProfile.toFirestoreData(), SetOptions.merge())
                .await()
            ProfileResult.Success(savedProfile)
        } catch (exception: Exception) {
            ProfileResult.Failure(exception.toProfileError())
        }
    }

    override suspend fun uploadCv(
        file: CvUploadFile,
        onProgress: (Float) -> Unit,
    ): ProfileResult<CvMetadata> {
        val uid = firebaseAuth.currentUser?.uid
            ?: return ProfileResult.Failure(ProfileError.NOT_AUTHENTICATED)
        if (!file.isPdf) return ProfileResult.Failure(ProfileError.INVALID_CV_FILE)

        return try {
            val profileDocument = userDocument(uid).get().await()
            val profileError = profileDocument.jobSeekerAccountError()
            if (profileError != null) return ProfileResult.Failure(profileError)

            val storagePath = "users/$uid/cv/current.pdf"
            val storageReference = storage.reference.child(storagePath)
            val storageMetadata = StorageMetadata.Builder()
                .setContentType(PDF_CONTENT_TYPE)
                .build()
            val uploadTask = storageReference.putFile(Uri.parse(file.uri), storageMetadata)
            uploadTask.addOnProgressListener { snapshot ->
                val totalBytes = snapshot.totalByteCount
                if (totalBytes > 0L) {
                    onProgress(
                        (snapshot.bytesTransferred.toFloat() / totalBytes.toFloat())
                            .coerceIn(0f, 1f),
                    )
                }
            }
            val uploadSnapshot = uploadTask.await()
            val cvMetadata = CvMetadata(
                fileName = file.fileName.trim(),
                storagePath = storagePath,
                contentType = PDF_CONTENT_TYPE,
                sizeBytes = uploadSnapshot.totalByteCount,
            )
            userDocument(uid).update(
                FIELD_CV,
                mapOf(
                    FIELD_CV_FILE_NAME to cvMetadata.fileName,
                    FIELD_CV_STORAGE_PATH to cvMetadata.storagePath,
                    FIELD_CV_CONTENT_TYPE to cvMetadata.contentType,
                    FIELD_CV_SIZE_BYTES to cvMetadata.sizeBytes,
                    FIELD_CV_UPLOADED_AT to FieldValue.serverTimestamp(),
                ),
            ).await()
            onProgress(1f)
            ProfileResult.Success(cvMetadata)
        } catch (exception: Exception) {
            ProfileResult.Failure(exception.toProfileError())
        }
    }

    private fun userDocument(uid: String) = firestore
        .collection(USERS_COLLECTION)
        .document(uid)

    private fun DocumentSnapshot.toJobSeekerProfile(
        uid: String,
    ): ProfileResult<JobSeekerProfile> {
        if (!exists()) {
            return ProfileResult.Failure(ProfileError.PROFILE_NOT_FOUND)
        }
        if (getString(FIELD_ROLE) != UserRole.JOB_SEEKER.name) {
            return ProfileResult.Failure(ProfileError.WRONG_ROLE)
        }

        val fullName = getString(FIELD_FULL_NAME)?.trim().orEmpty()
        if (fullName.isEmpty()) {
            return ProfileResult.Failure(ProfileError.PROFILE_NOT_FOUND)
        }

        return ProfileResult.Success(
            JobSeekerProfile(
                uid = uid,
                fullName = fullName,
                professionalHeadline = getString(FIELD_PROFESSIONAL_HEADLINE).orEmpty(),
                location = getString(FIELD_LOCATION).orEmpty(),
                phone = getString(FIELD_PHONE).orEmpty(),
                bio = getString(FIELD_BIO).orEmpty(),
                education = getString(FIELD_EDUCATION).orEmpty(),
                experienceSummary = getString(FIELD_EXPERIENCE_SUMMARY).orEmpty(),
                skills = getStringList(FIELD_SKILLS),
                preferredJobTypes = getStringList(FIELD_PREFERRED_JOB_TYPES),
                cv = get(FIELD_CV, CvMetadata::class.java),
            ),
        )
    }

    private fun DocumentSnapshot.jobSeekerAccountError(): ProfileError? = when {
        !exists() -> ProfileError.PROFILE_NOT_FOUND
        getString(FIELD_ROLE) != UserRole.JOB_SEEKER.name -> ProfileError.WRONG_ROLE
        else -> null
    }

    private fun DocumentSnapshot.getStringList(field: String): List<String> =
        (get(field) as? List<*>)
            .orEmpty()
            .filterIsInstance<String>()

    private fun JobSeekerProfile.toFirestoreData(): Map<String, Any> = mapOf(
        FIELD_FULL_NAME to fullName,
        FIELD_PROFESSIONAL_HEADLINE to professionalHeadline,
        FIELD_LOCATION to location,
        FIELD_PHONE to phone,
        FIELD_BIO to bio,
        FIELD_EDUCATION to education,
        FIELD_EXPERIENCE_SUMMARY to experienceSummary,
        FIELD_SKILLS to skills,
        FIELD_PREFERRED_JOB_TYPES to preferredJobTypes,
    )

    private fun Exception.toProfileError(): ProfileError = when (this) {
        is FirebaseFirestoreException -> when (code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> ProfileError.PERMISSION_DENIED
            FirebaseFirestoreException.Code.UNAVAILABLE,
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
            -> ProfileError.NETWORK

            else -> ProfileError.UNKNOWN
        }

        is StorageException -> when (errorCode) {
            StorageException.ERROR_NOT_AUTHENTICATED -> ProfileError.NOT_AUTHENTICATED
            StorageException.ERROR_NOT_AUTHORIZED -> ProfileError.PERMISSION_DENIED
            StorageException.ERROR_RETRY_LIMIT_EXCEEDED -> ProfileError.NETWORK
            StorageException.ERROR_OBJECT_NOT_FOUND -> ProfileError.CV_FILE_UNAVAILABLE
            else -> ProfileError.CV_UPLOAD_FAILED
        }

        is FileNotFoundException -> ProfileError.CV_FILE_UNAVAILABLE

        else -> ProfileError.UNKNOWN
    }

    private companion object {
        const val USERS_COLLECTION = "users"
        const val FIELD_ROLE = "role"
        const val FIELD_FULL_NAME = "fullName"
        const val FIELD_PROFESSIONAL_HEADLINE = "professionalHeadline"
        const val FIELD_LOCATION = "location"
        const val FIELD_PHONE = "phone"
        const val FIELD_BIO = "bio"
        const val FIELD_EDUCATION = "education"
        const val FIELD_EXPERIENCE_SUMMARY = "experienceSummary"
        const val FIELD_SKILLS = "skills"
        const val FIELD_PREFERRED_JOB_TYPES = "preferredJobTypes"
        const val FIELD_CV = "cv"
        const val FIELD_CV_FILE_NAME = "fileName"
        const val FIELD_CV_STORAGE_PATH = "storagePath"
        const val FIELD_CV_CONTENT_TYPE = "contentType"
        const val FIELD_CV_SIZE_BYTES = "sizeBytes"
        const val FIELD_CV_UPLOADED_AT = "uploadedAt"
    }
}
