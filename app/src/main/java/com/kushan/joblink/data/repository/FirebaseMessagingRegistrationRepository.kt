package com.kushan.joblink.data.repository

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import java.security.MessageDigest
import kotlinx.coroutines.tasks.await

class FirebaseMessagingRegistrationRepository(
    context: Context,
    private val firebaseMessaging: FirebaseMessaging = FirebaseMessaging.getInstance(),
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : MessagingRegistrationRepository {
    private val preferences = context.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    override suspend fun registerCurrentInstallation(): Result<Unit> = runCatching {
        pendingInstallationId()?.let { saveForAuthenticatedUser(it) }
        firebaseMessaging.register().await()
        Unit
    }.onFailure { exception ->
        Log.e(TAG, "Unable to register this app instance with FCM.", exception)
    }

    override suspend fun saveInstallationId(installationId: String): Result<Unit> = runCatching {
        require(installationId.isNotBlank())

        if (firebaseAuth.currentUser == null) {
            cachePendingInstallationId(installationId)
        } else {
            saveForAuthenticatedUser(installationId)
        }
    }.onFailure { exception ->
        Log.e(TAG, "Unable to persist the FCM installation registration.", exception)
        cachePendingInstallationId(installationId)
    }

    private suspend fun saveForAuthenticatedUser(installationId: String) {
        val uid = firebaseAuth.currentUser?.uid
        if (uid == null) {
            cachePendingInstallationId(installationId)
            return
        }

        firestore.collection(USERS_COLLECTION)
            .document(uid)
            .collection(REGISTRATIONS_COLLECTION)
            .document(installationId.sha256())
            .set(
                mapOf(
                    INSTALLATION_ID_FIELD to installationId,
                    PLATFORM_FIELD to PLATFORM_ANDROID,
                    UPDATED_AT_FIELD to FieldValue.serverTimestamp(),
                ),
            )
            .await()

        if (pendingInstallationId() == installationId) {
            preferences.edit { remove(PENDING_INSTALLATION_ID_KEY) }
        }
    }

    private fun pendingInstallationId(): String? = preferences
        .getString(PENDING_INSTALLATION_ID_KEY, null)
        ?.takeIf(String::isNotBlank)

    private fun cachePendingInstallationId(installationId: String) {
        if (installationId.isBlank()) return
        preferences.edit { putString(PENDING_INSTALLATION_ID_KEY, installationId) }
    }

    private fun String.sha256(): String = MessageDigest
        .getInstance("SHA-256")
        .digest(toByteArray(Charsets.UTF_8))
        .joinToString(separator = "") { byte -> "%02x".format(byte) }

    private companion object {
        const val TAG = "MessagingRegistration"
        const val PREFERENCES_NAME = "joblink_messaging"
        const val PENDING_INSTALLATION_ID_KEY = "pending_installation_id"
        const val USERS_COLLECTION = "users"
        const val REGISTRATIONS_COLLECTION = "messagingRegistrations"
        const val INSTALLATION_ID_FIELD = "installationId"
        const val PLATFORM_FIELD = "platform"
        const val UPDATED_AT_FIELD = "updatedAt"
        const val PLATFORM_ANDROID = "ANDROID"
    }
}
