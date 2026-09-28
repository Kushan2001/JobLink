package com.kushan.joblink.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

@Keep
data class Job(
    val id: String = "",
    val employerId: String = "",
    val companyName: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val location: String = "",
    val workMode: WorkMode = WorkMode.ONSITE,
    val jobType: JobType = JobType.FULL_TIME,
    val salaryMin: Long? = null,
    val salaryMax: Long? = null,
    val currency: String = "",
    val experienceLevel: String = "",
    val requiredSkills: List<String> = emptyList(),
    val requirements: List<String> = emptyList(),
    val benefits: List<String> = emptyList(),
    val applicationDeadline: Timestamp? = null,
    @get:ServerTimestamp
    var createdAt: Timestamp? = null,
    @get:ServerTimestamp
    var updatedAt: Timestamp? = null,
    val active: Boolean = true,
)
