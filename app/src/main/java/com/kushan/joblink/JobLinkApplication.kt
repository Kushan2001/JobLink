package com.kushan.joblink

import android.app.Application
import com.kushan.joblink.data.repository.ApplicationRepository
import com.kushan.joblink.data.repository.AuthRepository
import com.kushan.joblink.data.repository.EmployerProfileRepository
import com.kushan.joblink.data.repository.FirebaseAuthRepository
import com.kushan.joblink.data.repository.FirebaseApplicationRepository
import com.kushan.joblink.data.repository.FirebaseEmployerProfileRepository
import com.kushan.joblink.data.repository.FirebaseJobSeekerProfileRepository
import com.kushan.joblink.data.repository.FirebaseJobRepository
import com.kushan.joblink.data.repository.FirebaseMessagingRegistrationRepository
import com.kushan.joblink.data.repository.JobRepository
import com.kushan.joblink.data.repository.JobSeekerProfileRepository
import com.kushan.joblink.data.repository.MessagingRegistrationRepository
import com.kushan.joblink.notification.NotificationChannels
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class JobLinkApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val authRepository: AuthRepository by lazy { FirebaseAuthRepository() }
    val applicationRepository: ApplicationRepository by lazy {
        FirebaseApplicationRepository(cacheDirectory = cacheDir)
    }
    val employerProfileRepository: EmployerProfileRepository by lazy {
        FirebaseEmployerProfileRepository()
    }
    val jobSeekerProfileRepository: JobSeekerProfileRepository by lazy {
        FirebaseJobSeekerProfileRepository()
    }
    val jobRepository: JobRepository by lazy { FirebaseJobRepository() }
    val messagingRegistrationRepository: MessagingRegistrationRepository by lazy {
        FirebaseMessagingRegistrationRepository(this)
    }

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.create(this)
    }

    fun saveMessagingInstallationId(installationId: String) {
        applicationScope.launch {
            messagingRegistrationRepository.saveInstallationId(installationId)
        }
    }
}
