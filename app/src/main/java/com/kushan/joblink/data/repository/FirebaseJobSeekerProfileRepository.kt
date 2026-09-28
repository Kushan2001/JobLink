package com.kushan.joblink.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import com.kushan.joblink.data.model.JobSeekerProfile
import com.kushan.joblink.data.model.UserRole
import kotlinx.coroutines.tasks.await

class FirebaseJobSeekerProfileRepository(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
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
            ),
        )
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
    }
}
