package com.kushan.joblink

import android.app.Application
import com.kushan.joblink.data.repository.AuthRepository
import com.kushan.joblink.data.repository.EmployerProfileRepository
import com.kushan.joblink.data.repository.FirebaseAuthRepository
import com.kushan.joblink.data.repository.FirebaseEmployerProfileRepository
import com.kushan.joblink.data.repository.FirebaseJobSeekerProfileRepository
import com.kushan.joblink.data.repository.FirebaseJobRepository
import com.kushan.joblink.data.repository.JobRepository
import com.kushan.joblink.data.repository.JobSeekerProfileRepository

class JobLinkApplication : Application() {
    val authRepository: AuthRepository by lazy { FirebaseAuthRepository() }
    val employerProfileRepository: EmployerProfileRepository by lazy {
        FirebaseEmployerProfileRepository()
    }
    val jobSeekerProfileRepository: JobSeekerProfileRepository by lazy {
        FirebaseJobSeekerProfileRepository()
    }
    val jobRepository: JobRepository by lazy { FirebaseJobRepository() }
}
