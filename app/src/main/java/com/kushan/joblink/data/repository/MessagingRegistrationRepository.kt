package com.kushan.joblink.data.repository

interface MessagingRegistrationRepository {
    suspend fun registerCurrentInstallation(): Result<Unit>

    suspend fun saveInstallationId(installationId: String): Result<Unit>
}
