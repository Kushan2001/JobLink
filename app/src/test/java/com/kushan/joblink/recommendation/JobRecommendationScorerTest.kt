package com.kushan.joblink.recommendation

import com.kushan.joblink.data.model.Job
import com.kushan.joblink.data.model.JobSeekerProfile
import com.kushan.joblink.data.model.JobType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JobRecommendationScorerTest {
    @Test
    fun completeProfileMatchScoresOneHundred() {
        val recommendation = JobRecommendationScorer.score(
            profile = profile(
                location = "Colombo, Sri Lanka",
                headline = "Mid-level Android developer",
                skills = listOf("Kotlin", "Compose"),
                preferredJobTypes = listOf("Full time"),
            ),
            job = job(
                requiredSkills = listOf("Kotlin", "Jetpack Compose"),
                location = "Colombo",
                experienceLevel = "Mid level",
                jobType = JobType.FULL_TIME,
            ),
        )

        assertEquals(100, recommendation.score)
        assertEquals(listOf("Kotlin", "Jetpack Compose"), recommendation.matchedSkills)
        assertTrue(recommendation.preferredJobTypeMatched)
        assertTrue(recommendation.locationMatched)
        assertTrue(recommendation.experienceLevelMatched)
    }

    @Test
    fun partialSkillAndJobTypeMatchHavePredictableWeights() {
        val recommendation = JobRecommendationScorer.score(
            profile = profile(
                location = "Kandy",
                headline = "Senior developer",
                skills = listOf("Kotlin", "Compose"),
                preferredJobTypes = listOf("FULL_TIME"),
            ),
            job = job(
                requiredSkills = listOf("Kotlin", "Compose", "SQL", "Git"),
                location = "Colombo",
                experienceLevel = "Entry level",
                jobType = JobType.FULL_TIME,
            ),
        )

        // Two of four skills = 28 of 55 points after rounding, plus 20 for job type.
        assertEquals(48, recommendation.score)
        assertEquals(2, recommendation.matchedSkills.size)
        assertFalse(recommendation.locationMatched)
        assertFalse(recommendation.experienceLevelMatched)
    }

    @Test
    fun scoreIsZeroWhenNothingMatches() {
        val recommendation = JobRecommendationScorer.score(
            profile = profile(
                location = "Galle",
                headline = "Junior designer",
                skills = listOf("Figma"),
                preferredJobTypes = listOf("Part time"),
            ),
            job = job(
                requiredSkills = listOf("Kotlin"),
                location = "Colombo",
                experienceLevel = "Senior",
                jobType = JobType.FULL_TIME,
            ),
        )

        assertEquals(0, recommendation.score)
        assertTrue(recommendation.matchedSkills.isEmpty())
    }

    @Test
    fun skillAndJobTypeMatchingIgnoresCaseAndSeparators() {
        val recommendation = JobRecommendationScorer.score(
            profile = profile(
                skills = listOf("JETPACK-COMPOSE"),
                preferredJobTypes = listOf("full-time"),
            ),
            job = job(
                requiredSkills = listOf("Jetpack Compose"),
                jobType = JobType.FULL_TIME,
            ),
        )

        assertEquals(75, recommendation.score)
        assertEquals(1, recommendation.matchedSkills.size)
        assertTrue(recommendation.preferredJobTypeMatched)
    }

    @Test
    fun explicitYearsCanMatchBroadExperienceLevel() {
        val recommendation = JobRecommendationScorer.score(
            profile = profile(experienceSummary = "3 years of Android development"),
            job = job(experienceLevel = "Intermediate"),
        )

        assertEquals(10, recommendation.score)
        assertTrue(recommendation.experienceLevelMatched)
    }

    @Test
    fun missingAndBlankDataAreHandledSafely() {
        val recommendation = JobRecommendationScorer.score(
            profile = profile(skills = emptyList()),
            job = job(requiredSkills = listOf("", "  ")),
        )

        assertEquals(0, recommendation.score)
        assertTrue(recommendation.matchedSkills.isEmpty())
    }

    private fun profile(
        location: String = "",
        headline: String = "",
        experienceSummary: String = "",
        skills: List<String> = emptyList(),
        preferredJobTypes: List<String> = emptyList(),
    ) = JobSeekerProfile(
        uid = "user-1",
        fullName = "Alex Silva",
        professionalHeadline = headline,
        location = location,
        phone = "",
        bio = "",
        education = "",
        experienceSummary = experienceSummary,
        skills = skills,
        preferredJobTypes = preferredJobTypes,
    )

    private fun job(
        requiredSkills: List<String> = emptyList(),
        location: String = "",
        experienceLevel: String = "",
        jobType: JobType = JobType.CONTRACT,
    ) = Job(
        id = "job-1",
        requiredSkills = requiredSkills,
        location = location,
        experienceLevel = experienceLevel,
        jobType = jobType,
    )
}
