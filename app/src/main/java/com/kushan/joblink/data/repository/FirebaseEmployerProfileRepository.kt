package com.kushan.joblink.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import com.kushan.joblink.data.model.CompanyProfile
import com.kushan.joblink.data.model.UserRole
import kotlinx.coroutines.tasks.await

class FirebaseEmployerProfileRepository(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : EmployerProfileRepository {

    override suspend fun getCompanyProfile(): ProfileResult<CompanyProfile> {
        val currentUser = firebaseAuth.currentUser
            ?: return ProfileResult.Failure(ProfileError.NOT_AUTHENTICATED)

        return try {
            val userDocument = userDocument(currentUser.uid).get().await()
            val roleError = userDocument.employerRoleError()
            if (roleError != null) return ProfileResult.Failure(roleError)

            val companyDocument = companyDocument(currentUser.uid).get().await()
            if (companyDocument.exists()) {
                ProfileResult.Success(companyDocument.toCompanyProfile(currentUser.uid))
            } else {
                ProfileResult.Success(
                    CompanyProfile(
                        ownerUid = currentUser.uid,
                        companyName = "",
                        companyDescription = "",
                        industry = "",
                        companySize = "",
                        location = "",
                        website = "",
                        contactEmail = userDocument.getString(FIELD_EMAIL)
                            ?: currentUser.email.orEmpty(),
                    ),
                )
            }
        } catch (exception: Exception) {
            ProfileResult.Failure(exception.toProfileError())
        }
    }

    override suspend fun saveCompanyProfile(
        profile: CompanyProfile,
    ): ProfileResult<CompanyProfile> {
        val uid = firebaseAuth.currentUser?.uid
            ?: return ProfileResult.Failure(ProfileError.NOT_AUTHENTICATED)

        return try {
            val userDocument = userDocument(uid).get().await()
            val roleError = userDocument.employerRoleError()
            if (roleError != null) return ProfileResult.Failure(roleError)

            val documentReference = companyDocument(uid)
            val companyExists = documentReference.get().await().exists()
            val savedProfile = profile.copy(ownerUid = uid)
            val companyData = savedProfile.toFirestoreData().toMutableMap().apply {
                this[FIELD_UPDATED_AT] = FieldValue.serverTimestamp()
                if (!companyExists) {
                    this[FIELD_CREATED_AT] = FieldValue.serverTimestamp()
                }
            }

            documentReference.set(companyData, SetOptions.merge()).await()
            ProfileResult.Success(savedProfile)
        } catch (exception: Exception) {
            ProfileResult.Failure(exception.toProfileError())
        }
    }

    private fun userDocument(uid: String) = firestore
        .collection(USERS_COLLECTION)
        .document(uid)

    private fun companyDocument(uid: String) = firestore
        .collection(COMPANIES_COLLECTION)
        .document(uid)

    private fun DocumentSnapshot.employerRoleError(): ProfileError? = when {
        !exists() -> ProfileError.PROFILE_NOT_FOUND
        getString(FIELD_ROLE) != UserRole.EMPLOYER.name -> ProfileError.WRONG_ROLE
        else -> null
    }

    private fun DocumentSnapshot.toCompanyProfile(uid: String) = CompanyProfile(
        ownerUid = uid,
        companyName = getString(FIELD_COMPANY_NAME).orEmpty(),
        companyDescription = getString(FIELD_COMPANY_DESCRIPTION).orEmpty(),
        industry = getString(FIELD_INDUSTRY).orEmpty(),
        companySize = getString(FIELD_COMPANY_SIZE).orEmpty(),
        location = getString(FIELD_LOCATION).orEmpty(),
        website = getString(FIELD_WEBSITE).orEmpty(),
        contactEmail = getString(FIELD_CONTACT_EMAIL).orEmpty(),
    )

    private fun CompanyProfile.toFirestoreData(): Map<String, Any> = mapOf(
        FIELD_OWNER_UID to ownerUid,
        FIELD_COMPANY_NAME to companyName,
        FIELD_COMPANY_DESCRIPTION to companyDescription,
        FIELD_INDUSTRY to industry,
        FIELD_COMPANY_SIZE to companySize,
        FIELD_LOCATION to location,
        FIELD_WEBSITE to website,
        FIELD_CONTACT_EMAIL to contactEmail,
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
        const val COMPANIES_COLLECTION = "companies"
        const val FIELD_ROLE = "role"
        const val FIELD_EMAIL = "email"
        const val FIELD_OWNER_UID = "ownerUid"
        const val FIELD_COMPANY_NAME = "companyName"
        const val FIELD_COMPANY_DESCRIPTION = "companyDescription"
        const val FIELD_INDUSTRY = "industry"
        const val FIELD_COMPANY_SIZE = "companySize"
        const val FIELD_LOCATION = "location"
        const val FIELD_WEBSITE = "website"
        const val FIELD_CONTACT_EMAIL = "contactEmail"
        const val FIELD_CREATED_AT = "createdAt"
        const val FIELD_UPDATED_AT = "updatedAt"
    }
}
