package com.kushan.joblink.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import com.kushan.joblink.R
import com.kushan.joblink.data.model.Job
import com.kushan.joblink.data.model.JobType
import com.kushan.joblink.data.model.WorkMode
import com.kushan.joblink.ui.theme.JobLinkSpacing
import java.text.DateFormat
import java.text.NumberFormat

@Composable
fun JobCard(
    job: Job,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSaved: Boolean = false,
    recommendationScore: Int? = null,
    matchedSkillCount: Int = 0,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(modifier = Modifier.padding(JobLinkSpacing.large)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(JobLinkSpacing.small),
            ) {
                Text(
                    text = job.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                if (isSaved) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Text(
                            text = stringResource(R.string.job_saved),
                            modifier = Modifier.padding(
                                horizontal = JobLinkSpacing.small,
                                vertical = JobLinkSpacing.extraSmall,
                            ),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(JobLinkSpacing.extraSmall))
            Text(
                text = job.companyName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            recommendationScore?.let { score ->
                Spacer(modifier = Modifier.height(JobLinkSpacing.small))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(
                        text = stringResource(R.string.profile_match_score, score),
                        modifier = Modifier.padding(
                            horizontal = JobLinkSpacing.small,
                            vertical = JobLinkSpacing.extraSmall,
                        ),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                if (matchedSkillCount > 0) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.matched_skills_reason,
                            matchedSkillCount,
                            matchedSkillCount,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(modifier = Modifier.height(JobLinkSpacing.small))
            Text(
                text = job.location,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(JobLinkSpacing.small))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(JobLinkSpacing.small)) {
                JobAttributeChip(label = stringResource(job.jobType.labelResource()))
                JobAttributeChip(label = stringResource(job.workMode.labelResource()))
            }

            job.formattedSalary()?.let { salary ->
                Spacer(modifier = Modifier.height(JobLinkSpacing.small))
                Text(
                    text = salary,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
            }

            job.createdAt?.let { createdAt ->
                Spacer(modifier = Modifier.height(JobLinkSpacing.medium))
                Text(
                    text = stringResource(
                        R.string.posted_on,
                        DateFormat.getDateInstance(DateFormat.MEDIUM).format(createdAt.toDate()),
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun JobAttributeChip(label: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(
                horizontal = JobLinkSpacing.small,
                vertical = JobLinkSpacing.extraSmall,
            ),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun Job.formattedSalary(): String? {
    if (salaryMin == null && salaryMax == null) return null
    val numberFormat = NumberFormat.getNumberInstance()
    val displayCurrency = currency.ifBlank { "" }
    return when {
        salaryMin != null && salaryMax != null -> stringResource(
            R.string.salary_range,
            displayCurrency,
            numberFormat.format(salaryMin),
            numberFormat.format(salaryMax),
        )

        salaryMin != null -> stringResource(
            R.string.salary_from,
            displayCurrency,
            numberFormat.format(salaryMin),
        )

        else -> stringResource(
            R.string.salary_up_to,
            displayCurrency,
            numberFormat.format(salaryMax),
        )
    }
}

@androidx.annotation.StringRes
private fun JobType.labelResource(): Int = when (this) {
    JobType.FULL_TIME -> R.string.job_type_full_time
    JobType.PART_TIME -> R.string.job_type_part_time
    JobType.CONTRACT -> R.string.job_type_contract
    JobType.INTERNSHIP -> R.string.job_type_internship
    JobType.FREELANCE -> R.string.job_type_freelance
    JobType.TEMPORARY -> R.string.job_type_temporary
}

@androidx.annotation.StringRes
private fun WorkMode.labelResource(): Int = when (this) {
    WorkMode.ONSITE -> R.string.work_mode_onsite
    WorkMode.REMOTE -> R.string.work_mode_remote
    WorkMode.HYBRID -> R.string.work_mode_hybrid
}
