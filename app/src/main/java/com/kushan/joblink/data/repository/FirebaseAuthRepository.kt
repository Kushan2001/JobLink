package com.kushan.joblink.data.repository

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.kushan.joblink.data.model.UserProfile
import com.kushan.joblink.data.model.UserRole
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await

class FirebaseAuthRepository(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : AuthRepository {

    override val authenticationState: Flow<AuthenticationState> = callbackFlow {
        trySend(AuthenticationState.Loading)

        val listener = FirebaseAuth.AuthStateListener { auth ->
            val user = auth.currentUser
            val state = if (user == null) {
                AuthenticationState.Unauthenticated
            } else {
                AuthenticationState.Authenticated(
                    uid = user.uid,
                    email = user.email.orEmpty(),
                )
            }
            trySend(state)
        }

        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()

    override suspend fun register(
        fullName: String,
        email: String,
        password: String,
        role: UserRole,
    ): AuthResult<UserProfile> {
        return try {
            val authResult = firebaseAuth
                .createUserWithEmailAndPassword(email, password)
                .await()
            val user = authResult.user ?: return AuthResult.Failure(AuthError.UNKNOWN)
            val profile = UserProfile(
                uid = user.uid,
                fullName = fullName,
                email = user.email ?: email,
                role = role,
            )

            try {
                firestore.collection(USERS_COLLECTION)
                    .document(user.uid)
                    .set(profile.toFirestoreData())
                    .await()
            } catch (exception: Exception) {
                rollbackIncompleteRegistration()
                return AuthResult.Failure(
                    exception.toAuthError(default = AuthError.PROFILE_SAVE_FAILED),
                )
            }

            AuthResult.Success(profile)
        } catch (exception: Exception) {
            AuthResult.Failure(exception.toAuthError())
        }
    }

    override suspend fun login(email: String, password: String): AuthResult<UserProfile> {
        return try {
            firebaseAuth.signInWithEmailAndPassword(email, password).await()
            when (val profileResult = getCurrentUserProfile()) {
                is AuthResult.Success -> profileResult
                is AuthResult.Failure -> {
                    firebaseAuth.signOut()
                    profileResult
                }
            }
        } catch (exception: Exception) {
            AuthResult.Failure(exception.toAuthError())
        }
    }

    override suspend fun getCurrentUserProfile(): AuthResult<UserProfile> {
        val currentUser = firebaseAuth.currentUser
            ?: return AuthResult.Failure(AuthError.INVALID_CREDENTIALS)

        return try {
            val document = firestore.collection(USERS_COLLECTION)
                .document(currentUser.uid)
                .get()
                .await()

            if (!document.exists()) {
                return AuthResult.Failure(AuthError.PROFILE_NOT_FOUND)
            }

            val fullName = document.getString(FIELD_FULL_NAME)?.trim().orEmpty()
            val email = document.getString(FIELD_EMAIL)?.trim().orEmpty()
            val role = document.getString(FIELD_ROLE)?.let { storedRole ->
                UserRole.entries.firstOrNull { it.name == storedRole }
            }

            if (fullName.isEmpty() || email.isEmpty() || role == null) {
                AuthResult.Failure(AuthError.PROFILE_INVALID)
            } else {
                AuthResult.Success(
                    UserProfile(
                        uid = currentUser.uid,
                        fullName = fullName,
                        email = email,
                        role = role,
                    ),
                )
            }
        } catch (exception: Exception) {
            AuthResult.Failure(exception.toAuthError())
        }
    }

    override fun logout() {
        firebaseAuth.signOut()
    }

    private suspend fun rollbackIncompleteRegistration() {
        val user = firebaseAuth.currentUser
        if (user != null) {
            try {
                user.delete().await()
            } catch (_: Exception) {
                // Signing out still prevents a partially registered account from entering the app.
            }
        }
        firebaseAuth.signOut()
    }

    private fun UserProfile.toFirestoreData(): Map<String, Any> = mapOf(
        FIELD_UID to uid,
        FIELD_FULL_NAME to fullName,
        FIELD_EMAIL to email,
        FIELD_ROLE to role.name,
        FIELD_CREATED_AT to FieldValue.serverTimestamp(),
    )

    private fun Exception.toAuthError(default: AuthError = AuthError.UNKNOWN): AuthError = when (this) {
        is FirebaseAuthUserCollisionException -> AuthError.EMAIL_ALREADY_IN_USE
        is FirebaseAuthWeakPasswordException -> AuthError.WEAK_PASSWORD
        is FirebaseAuthInvalidUserException -> {
            if (errorCode == ERROR_USER_DISABLED) {
                AuthError.USER_DISABLED
            } else {
                AuthError.INVALID_CREDENTIALS
            }
        }

        is FirebaseAuthInvalidCredentialsException -> AuthError.INVALID_CREDENTIALS
        is FirebaseTooManyRequestsException -> AuthError.TOO_MANY_REQUESTS
        is FirebaseNetworkException -> AuthError.NETWORK
        is FirebaseFirestoreException -> when (code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> AuthError.PERMISSION_DENIED
            FirebaseFirestoreException.Code.UNAVAILABLE,
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
            -> AuthError.NETWORK

            else -> default
        }

        else -> default
    }

    private companion object {
        const val USERS_COLLECTION = "users"
        const val FIELD_UID = "uid"
        const val FIELD_FULL_NAME = "fullName"
        const val FIELD_EMAIL = "email"
        const val FIELD_ROLE = "role"
        const val FIELD_CREATED_AT = "createdAt"
        const val ERROR_USER_DISABLED = "ERROR_USER_DISABLED"
    }
}
