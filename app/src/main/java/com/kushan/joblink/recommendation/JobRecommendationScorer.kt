package com.kushan.joblink.recommendation

import com.kushan.joblink.data.model.Job
import com.kushan.joblink.data.model.JobSeekerProfile
import com.kushan.joblink.data.model.JobType
import kotlin.math.roundToInt

data class JobRecommendation(
    val score: Int,
    val matchedSkills: List<String>,
    val preferredJobTypeMatched: Boolean,
    val locationMatched: Boolean,
    val experienceLevelMatched: Boolean,
)

/**
 * Produces an explainable profile-match score. It is not a prediction of hiring success.
 *
 * The 100 available points are intentionally simple:
 * - required skills: 55
 * - preferred job type: 20
 * - location: 15
 * - experience level: 10
 *
 * JobLink does not currently collect a preferred work mode in the job-seeker profile, so
 * work mode is not guessed or included in the score.
 */
object JobRecommendationScorer {
    private const val SKILLS_WEIGHT = 55
    private const val JOB_TYPE_WEIGHT = 20
    private const val LOCATION_WEIGHT = 15
    private const val EXPERIENCE_WEIGHT = 10

    fun score(profile: JobSeekerProfile, job: Job): JobRecommendation {
        val matchedSkills = job.requiredSkills.filter { requiredSkill ->
            profile.skills.any { profileSkill -> skillsMatch(profileSkill, requiredSkill) }
        }.distinctBy(String::normalizedValue)

        val requiredSkillCount = job.requiredSkills.distinctNormalizedCount()
        val skillScore = if (requiredSkillCount == 0) {
            0
        } else {
            (matchedSkills.size.toFloat() / requiredSkillCount)
                .times(SKILLS_WEIGHT)
                .roundToInt()
                .coerceAtMost(SKILLS_WEIGHT)
        }

        val preferredJobTypeMatched = profile.preferredJobTypes.any { preference ->
            preference.normalizedValue() == job.jobType.normalizedValue()
        }
        val locationMatched = locationsMatch(profile.location, job.location)
        val experienceLevelMatched = experienceLevelsMatch(profile, job)

        val total = skillScore +
            JOB_TYPE_WEIGHT.takeIf { preferredJobTypeMatched }.orZero() +
            LOCATION_WEIGHT.takeIf { locationMatched }.orZero() +
            EXPERIENCE_WEIGHT.takeIf { experienceLevelMatched }.orZero()

        return JobRecommendation(
            score = total.coerceIn(0, 100),
            matchedSkills = matchedSkills,
            preferredJobTypeMatched = preferredJobTypeMatched,
            locationMatched = locationMatched,
            experienceLevelMatched = experienceLevelMatched,
        )
    }

    private fun skillsMatch(profileSkill: String, requiredSkill: String): Boolean {
        val profileTerms = profileSkill.normalizedTerms()
        val requiredTerms = requiredSkill.normalizedTerms()
        if (profileTerms.isEmpty() || requiredTerms.isEmpty()) return false
        return profileTerms == requiredTerms ||
            profileTerms.containsAll(requiredTerms) ||
            requiredTerms.containsAll(profileTerms)
    }

    private fun locationsMatch(profileLocation: String, jobLocation: String): Boolean {
        val profileValue = profileLocation.normalizedValue()
        val jobValue = jobLocation.normalizedValue()
        if (profileValue.isBlank() || jobValue.isBlank()) return false
        return profileValue == jobValue ||
            profileValue.contains(jobValue) ||
            jobValue.contains(profileValue)
    }

    private fun experienceLevelsMatch(profile: JobSeekerProfile, job: Job): Boolean {
        val profileText = listOf(
            profile.professionalHeadline,
            profile.experienceSummary,
        ).joinToString(" ")
        val profileLevel = ExperienceLevel.fromText(profileText) ?: return false
        val jobLevel = ExperienceLevel.fromText(job.experienceLevel) ?: return false
        return profileLevel == jobLevel
    }

    private enum class ExperienceLevel {
        ENTRY,
        MID,
        SENIOR;

        companion object {
            fun fromText(value: String): ExperienceLevel? {
                val normalized = value.normalizedValue()
                if (normalized.isBlank()) return null

                return when {
                    SENIOR_TERMS.any(normalized::contains) -> SENIOR
                    MID_TERMS.any(normalized::contains) -> MID
                    ENTRY_TERMS.any(normalized::contains) -> ENTRY
                    else -> YEARS_PATTERN.find(normalized)
                        ?.groupValues
                        ?.getOrNull(1)
                        ?.toIntOrNull()
                        ?.let { years ->
                            when {
                                years >= 5 -> SENIOR
                                years >= 2 -> MID
                                else -> ENTRY
                            }
                        }
                }
            }

            private val SENIOR_TERMS = listOf("senior", "lead", "principal", "expert")
            private val MID_TERMS = listOf("mid level", "midlevel", "intermediate")
            private val ENTRY_TERMS = listOf("entry level", "junior", "graduate", "intern")
            private val YEARS_PATTERN = Regex("(\\d+)\\s*\\+?\\s*years?")
        }
    }
}

private fun JobType.normalizedValue(): String = name.normalizedValue()

private fun List<String>.distinctNormalizedCount(): Int =
    map(String::normalizedValue).filter(String::isNotBlank).distinct().size

private fun String.normalizedTerms(): Set<String> =
    normalizedValue().split(" ").filter(String::isNotBlank).toSet()

private fun String.normalizedValue(): String =
    trim().lowercase().replace(Regex("[^a-z0-9]+"), " ").trim()

private fun Int?.orZero(): Int = this ?: 0
