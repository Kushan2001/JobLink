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
    val cv: CvMetadata? = null,
    val preferredWorkModes: List<String> = emptyList(),
    val preferredLocations: List<String> = emptyList(),
    val experienceLevel: String = "",
)
