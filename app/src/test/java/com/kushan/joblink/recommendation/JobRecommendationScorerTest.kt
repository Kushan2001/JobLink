package com.kushan.joblink.recommendation

import com.kushan.joblink.data.model.Job
import com.kushan.joblink.data.model.JobSeekerProfile
import com.kushan.joblink.data.model.JobType
import com.kushan.joblink.data.model.WorkMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JobRecommendationScorerTest {
    @Test
    fun completeProfileMatchScoresOneHundred() {
        val profile = profile(
            skills = listOf("Kotlin", "Jetpack Compose", "Git", "Firebase"),
            preferredJobTypes = listOf("Full-time"),
            preferredWorkModes = listOf("ONSITE"),
            location = "Colombo",
            professionalHeadline = "Senior Android Developer",
        )
        val job = job(
            requiredSkills = listOf("Kotlin", "Jetpack Compose", "Git", "Firebase"),
            jobType = JobType.FULL_TIME,
            location = "Colombo",
            experienceLevel = "Senior",
        )

        val result = JobRecommendationScorer.score(profile, job)

        assertEquals(100, result.score)
        assertEquals(4, result.matchedSkillCount)
        assertTrue(result.jobTypeMatched)
        assertTrue(result.workModeMatched)
        assertTrue(result.locationMatched)
        assertTrue(result.experienceLevelMatched)
    }

    @Test
    fun partialSkillMatchUsesProportionalSkillWeight() {
        val result = JobRecommendationScorer.score(
            profile = profile(
                skills = listOf("Kotlin", "Compose"),
                preferredJobTypes = listOf("Part-time"),
                location = "Kandy",
                professionalHeadline = "Junior Developer",
            ),
            job = job(
                requiredSkills = listOf("Kotlin", "Compose", "Firebase", "Git"),
                jobType = JobType.FULL_TIME,
                location = "Colombo",
                experienceLevel = "Senior",
            ),
        )

        assertEquals(25, result.score)
        assertEquals(2, result.matchedSkillCount)
        assertEquals(4, result.requiredSkillCount)
        assertFalse(result.jobTypeMatched)
        assertFalse(result.workModeMatched)
        assertFalse(result.locationMatched)
        assertFalse(result.experienceLevelMatched)
    }

    @Test
    fun comparisonsIgnoreCaseAndCommonSeparators() {
        val result = JobRecommendationScorer.score(
            profile = profile(
                skills = listOf("Jetpack-Compose"),
                preferredJobTypes = listOf("FULL TIME"),
                preferredWorkModes = listOf("on-site"),
                location = "Colombo, Sri Lanka",
                professionalHeadline = "Senior mobile engineer",
            ),
            job = job(
                requiredSkills = listOf("jetpack compose"),
                jobType = JobType.FULL_TIME,
                location = "Colombo",
                experienceLevel = "Senior level",
            ),
        )

        assertEquals(100, result.score)
    }

    @Test
    fun unrelatedProfileScoresZero() {
        val result = JobRecommendationScorer.score(
            profile = profile(
                skills = listOf("Accounting"),
                preferredJobTypes = listOf("Contract"),
                location = "Galle",
                professionalHeadline = "Junior Accountant",
            ),
            job = job(
                requiredSkills = listOf("Kotlin"),
                jobType = JobType.FULL_TIME,
                location = "Colombo",
                experienceLevel = "Senior",
            ),
        )

        assertEquals(0, result.score)
    }

    @Test
    fun jobWithoutRequiredSkillsCanStillMatchOtherProfilePreferences() {
        val result = JobRecommendationScorer.score(
            profile = profile(
                preferredJobTypes = listOf("Full time"),
                location = "Colombo",
                professionalHeadline = "Entry level candidate",
            ),
            job = job(
                requiredSkills = emptyList(),
                jobType = JobType.FULL_TIME,
                location = "Colombo",
                experienceLevel = "Entry level",
            ),
        )

        assertEquals(40, result.score)
        assertEquals(0, result.matchedSkillCount)
        assertEquals(0, result.requiredSkillCount)
    }

    @Test
    fun emptyProfileSafelyScoresZero() {
        val result = JobRecommendationScorer.score(
            profile = profile(),
            job = job(
                requiredSkills = listOf("Kotlin"),
                jobType = JobType.FULL_TIME,
                location = "Colombo",
                experienceLevel = "Senior",
            ),
        )

        assertEquals(0, result.score)
        assertEquals(0, result.matchedSkillCount)
    }

    @Test
    fun noSkillsDoesNotDivideByZeroOrMatchRequiredSkills() {
        val result = JobRecommendationScorer.score(
            profile = profile(skills = emptyList()),
            job = job(requiredSkills = listOf("Kotlin", "Compose")),
        )

        assertEquals(0, result.score)
        assertEquals(0, result.matchedSkillCount)
        assertEquals(2, result.requiredSkillCount)
    }

    @Test
    fun missingOptionalJobFieldsAreSafe() {
        val result = JobRecommendationScorer.score(profile = profile(), job = Job(id = "legacy"))

        assertEquals(0, result.score)
        assertEquals(0, result.requiredSkillCount)
    }

    @Test
    fun unknownPreferencesDoNotRequireUnsafeEnumConversion() {
        val result = JobRecommendationScorer.score(
            profile = profile(
                preferredJobTypes = listOf("SOMEDAY"),
                preferredWorkModes = listOf("SPACE_STATION"),
            ),
            job = job(jobType = JobType.FULL_TIME, workMode = WorkMode.ONSITE),
        )

        assertEquals(0, result.score)
        assertFalse(result.jobTypeMatched)
        assertFalse(result.workModeMatched)
    }

    @Test
    fun scoreAlwaysStaysBetweenZeroAndOneHundred() {
        val profiles = listOf(
            profile(),
            profile(
                skills = listOf("Kotlin", "Compose"),
                preferredJobTypes = listOf("FULL_TIME"),
                preferredWorkModes = listOf("REMOTE"),
                preferredLocations = listOf("Colombo"),
                experienceLevel = "Senior",
            ),
        )
        val jobs = listOf(
            Job(id = "empty"),
            job(
                requiredSkills = listOf("Kotlin", "Compose"),
                jobType = JobType.FULL_TIME,
                workMode = WorkMode.REMOTE,
                location = "Colombo",
                experienceLevel = "Senior",
            ),
        )

        profiles.forEach { candidate ->
            jobs.forEach { vacancy ->
                assertTrue(JobRecommendationScorer.score(candidate, vacancy).score in 0..100)
            }
        }
    }

    private fun profile(
        skills: List<String> = emptyList(),
        preferredJobTypes: List<String> = emptyList(),
        preferredWorkModes: List<String> = emptyList(),
        preferredLocations: List<String> = emptyList(),
        experienceLevel: String = "",
        location: String = "",
        professionalHeadline: String = "",
    ) = JobSeekerProfile(
        uid = "applicant-1",
        fullName = "Alex Silva",
        professionalHeadline = professionalHeadline,
        location = location,
        phone = "",
        bio = "",
        education = "",
        experienceSummary = professionalHeadline,
        skills = skills,
        preferredJobTypes = preferredJobTypes,
        preferredWorkModes = preferredWorkModes,
        preferredLocations = preferredLocations,
        experienceLevel = experienceLevel,
    )

    private fun job(
        requiredSkills: List<String> = emptyList(),
        jobType: JobType = JobType.FULL_TIME,
        workMode: WorkMode = WorkMode.ONSITE,
        location: String = "",
        experienceLevel: String = "",
    ) = Job(
        id = "job-1",
        requiredSkills = requiredSkills,
        jobType = jobType,
        workMode = workMode,
        location = location,
        experienceLevel = experienceLevel,
    )
}
