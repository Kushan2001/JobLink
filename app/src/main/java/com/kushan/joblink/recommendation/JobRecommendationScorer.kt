package com.kushan.joblink.recommendation

import com.kushan.joblink.data.model.Job
import com.kushan.joblink.data.model.JobSeekerProfile
import kotlin.math.roundToInt

data class JobRecommendation(
    val score: Int,
    val matchedSkillCount: Int,
    val requiredSkillCount: Int,
    val jobTypeMatched: Boolean,
    val workModeMatched: Boolean,
    val locationMatched: Boolean,
    val experienceLevelMatched: Boolean,
)

object JobRecommendationScorer {
    fun score(
        profile: JobSeekerProfile,
        job: Job,
    ): JobRecommendation {
        val profileSkills = profile.skills.mapTo(mutableSetOf()) { it.normalized() }
        val requiredSkills = job.requiredSkills
            .map { it.normalized() }
            .filter(String::isNotBlank)
            .distinct()
        val matchedSkillCount = requiredSkills.count(profileSkills::contains)
        val skillPoints = if (requiredSkills.isEmpty()) {
            0.0
        } else {
            matchedSkillCount.toDouble() / requiredSkills.size * SKILL_WEIGHT
        }

        val preferredTypes = profile.preferredJobTypes
            .mapTo(mutableSetOf()) { it.normalizedEnumValue() }
        val jobTypeMatched = job.jobType.name.normalizedEnumValue() in preferredTypes
        val jobTypePoints = if (jobTypeMatched) JOB_TYPE_WEIGHT else 0

        val preferredWorkModes = profile.preferredWorkModes
            .mapTo(mutableSetOf()) { it.normalizedEnumValue() }
        val workModeMatched = job.workMode.name.normalizedEnumValue() in preferredWorkModes
        val workModePoints = if (workModeMatched) WORK_MODE_WEIGHT else 0

        val normalizedProfileLocations = (profile.preferredLocations + profile.location)
            .map { location -> location.normalized() }
            .filter(String::isNotBlank)
        val normalizedJobLocation = job.location.normalized()
        val locationMatched = normalizedJobLocation.isNotBlank() &&
            normalizedProfileLocations.any { profileLocation ->
                profileLocation.contains(normalizedJobLocation) ||
                    normalizedJobLocation.contains(profileLocation)
            }
        val locationPoints = if (locationMatched) LOCATION_WEIGHT else 0

        val experienceText = listOf(
            profile.experienceLevel,
            profile.professionalHeadline,
            profile.experienceSummary,
        ).joinToString(" ").normalized()
        val profileExperienceTokens = experienceText.split(" ").toSet()
        val jobExperienceLevel = job.experienceLevel.normalized()
        val experienceLevelMatched = jobExperienceLevel.isNotBlank() &&
            experienceKeywords(jobExperienceLevel).any(profileExperienceTokens::contains)
        val experiencePoints = if (experienceLevelMatched) EXPERIENCE_WEIGHT else 0

        val total = (
            skillPoints + jobTypePoints + workModePoints + locationPoints + experiencePoints
            )
            .roundToInt()
            .coerceIn(0, 100)
        return JobRecommendation(
            score = total,
            matchedSkillCount = matchedSkillCount,
            requiredSkillCount = requiredSkills.size,
            jobTypeMatched = jobTypeMatched,
            workModeMatched = workModeMatched,
            locationMatched = locationMatched,
            experienceLevelMatched = experienceLevelMatched,
        )
    }

    private fun experienceKeywords(value: String): Set<String> {
        val tokens = value.split(" ").filter(String::isNotBlank).toSet()
        return EXPERIENCE_KEYWORDS.filterTo(mutableSetOf()) { keyword ->
            keyword in tokens
        }
    }

    private fun String.normalized(): String = lowercase()
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()
        .replace(Regex("\\s+"), " ")

    private fun String.normalizedEnumValue(): String = normalized().replace(" ", "")

    private const val SKILL_WEIGHT = 50
    private const val JOB_TYPE_WEIGHT = 15
    private const val WORK_MODE_WEIGHT = 10
    private const val LOCATION_WEIGHT = 15
    private const val EXPERIENCE_WEIGHT = 10
    private val EXPERIENCE_KEYWORDS = setOf(
        "intern",
        "entry",
        "junior",
        "mid",
        "senior",
        "lead",
        "principal",
    )
}
