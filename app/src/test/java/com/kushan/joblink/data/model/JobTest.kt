package com.kushan.joblink.data.model

import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JobTest {
    @Test
    fun defaultJobIsFirestoreConstructible() {
        val job = Job::class.java.getDeclaredConstructor().newInstance()

        assertEquals("", job.id)
        assertEquals(WorkMode.ONSITE, job.workMode)
        assertEquals(JobType.FULL_TIME, job.jobType)
        assertTrue(job.active)
        assertTrue(job.requiredSkills.isEmpty())
        assertNull(job.createdAt)
        assertNull(job.updatedAt)
    }

    @Test
    fun jobTypeValuesMatchStoredContract() {
        assertEquals(
            listOf(
                "FULL_TIME",
                "PART_TIME",
                "CONTRACT",
                "INTERNSHIP",
                "FREELANCE",
                "TEMPORARY",
            ),
            JobType.entries.map(JobType::name),
        )
    }

    @Test
    fun workModeValuesMatchStoredContract() {
        assertEquals(
            listOf("ONSITE", "REMOTE", "HYBRID"),
            WorkMode.entries.map(WorkMode::name),
        )
    }

    @Test
    fun populatedJobRetainsDomainValues() {
        val deadline = Timestamp(1_800_000_000L, 0)
        val job = Job(
            id = "job-id",
            employerId = "employer-id",
            companyName = "JobLink Labs",
            title = "Android Developer",
            description = "Build accessible Android applications.",
            category = "Software Development",
            location = "Colombo",
            workMode = WorkMode.HYBRID,
            jobType = JobType.FULL_TIME,
            salaryMin = 150_000,
            salaryMax = 250_000,
            currency = "LKR",
            experienceLevel = "MID_LEVEL",
            requiredSkills = listOf("Kotlin", "Jetpack Compose"),
            requirements = listOf("Two years of Android experience"),
            benefits = listOf("Flexible hours"),
            applicationDeadline = deadline,
            active = true,
        )

        assertEquals(WorkMode.HYBRID, job.workMode)
        assertEquals(JobType.FULL_TIME, job.jobType)
        assertEquals(150_000L, job.salaryMin)
        assertEquals(250_000L, job.salaryMax)
        assertEquals(deadline, job.applicationDeadline)
        assertNotNull(job.requiredSkills.singleOrNull { it == "Kotlin" })
    }
}
