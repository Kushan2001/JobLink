package com.kushan.joblink.data.model

data class JobSeekerProfile(
    val uid: String,
    val fullName: String,
    val professionalHeadline: String,
    val location: String,
    val phone: String,
    val bio: String,
    val education: String,
    val experienceSummary: String,
    val skills: List<String>,
    val preferredJobTypes: List<String>,
)
