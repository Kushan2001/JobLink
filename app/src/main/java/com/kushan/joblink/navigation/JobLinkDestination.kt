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
data object EmployerHomeDestination
