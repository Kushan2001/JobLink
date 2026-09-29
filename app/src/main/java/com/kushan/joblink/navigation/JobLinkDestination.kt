package com.kushan.joblink.navigation

import com.kushan.joblink.data.model.UserRole
import kotlinx.serialization.Serializable

@Serializable
data object WelcomeDestination

@Serializable
data object RoleSelectionDestination

@Serializable
data object LoginDestination

@Serializable
data class RegisterDestination(val role: UserRole)

@Serializable
data object JobSeekerHomeDestination

@Serializable
data object JobSeekerProfileDestination

@Serializable
data class JobDetailsDestination(val jobId: String)

@Serializable
data object EmployerHomeDestination

@Serializable
data object PostJobDestination
