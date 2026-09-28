package com.kushan.joblink

import android.app.Application
import com.kushan.joblink.data.repository.AuthRepository
import com.kushan.joblink.data.repository.FirebaseAuthRepository

class JobLinkApplication : Application() {
    val authRepository: AuthRepository by lazy { FirebaseAuthRepository() }
}
