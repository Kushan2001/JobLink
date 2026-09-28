package com.kushan.joblink.data.model

data class UserProfile(
    val uid: String,
    val fullName: String,
    val email: String,
    val role: UserRole,
)
