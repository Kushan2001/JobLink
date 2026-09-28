package com.kushan.joblink

import android.app.Application
import com.kushan.joblink.data.repository.AuthRepository
import com.kushan.joblink.data.repository.FirebaseAuthRepository
import com.kushan.joblink.data.repository.FirebaseJobSeekerProfileRepository
import com.kushan.joblink.data.repository.JobSeekerProfileRepository

class JobLinkApplication : Application() {
    val authRepository: AuthRepository by lazy { FirebaseAuthRepository() }
    val jobSeekerProfileRepository: JobSeekerProfileRepository by lazy {
        FirebaseJobSeekerProfileRepository()
    }
}
