package com.kushan.joblink.data.model

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
enum class UserRole {
    JOB_SEEKER,
    EMPLOYER,
}
