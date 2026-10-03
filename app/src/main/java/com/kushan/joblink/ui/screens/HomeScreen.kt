package com.kushan.joblink.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kushan.joblink.R
import com.kushan.joblink.data.model.JobType
import com.kushan.joblink.data.model.WorkMode
import com.kushan.joblink.data.repository.JobError
import com.kushan.joblink.ui.components.JobCard
import com.kushan.joblink.ui.components.DashboardShortcutCard
import com.kushan.joblink.ui.theme.JobLinkSpacing
import com.kushan.joblink.viewmodel.HomeUiState
import com.kushan.joblink.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    userName: String,
    onJobClick: (String) -> Unit,
    onApplications: () -> Unit,
    onSavedJobs: () -> Unit,
    onProfile: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val error = uiState.error
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HomeHeader(
            userName = userName,
            onApplications = onApplications,
            onSavedJobs = onSavedJobs,
            onProfile = onProfile,
            onLogout = onLogout,
        )
        SearchAndFilterBar(
            uiState = uiState,
            onSearchQueryChanged = viewModel::onSearchQueryChanged,
            onCategoryChanged = viewModel::onCategoryFilterChanged,
            onLocationChanged = viewModel::onLocationFilterChanged,
            onExperienceLevelChanged = viewModel::onExperienceLevelFilterChanged,
            onJobTypeChanged = viewModel::onJobTypeFilterChanged,
            onWorkModeChanged = viewModel::onWorkModeFilterChanged,
            onClearFilters = viewModel::clearFilters,
        )
        JobCategoryRow(
            categories = uiState.categories,
            selectedCategory = uiState.categoryFilter,
            onCategorySelected = viewModel::onCategorySelected,
        )
        when {
            uiState.isLoading -> JobFeedLoading()
            error != null && uiState.jobs.isEmpty() -> JobFeedError(
                error = error,
                onRetry = viewModel::retry,
            )

            else -> PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier
                    .widthIn(max = 760.dp)
                    .fillMaxSize(),
            ) {
                JobFeed(
                    uiState = uiState,
                    onJobClick = onJobClick,
                    onRetry = viewModel::refresh,
                    onClearFilters = viewModel::clearFilters,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchAndFilterBar(
    uiState: HomeUiState,
    onSearchQueryChanged: (String) -> Unit,
    onCategoryChanged: (String) -> Unit,
    onLocationChanged: (String) -> Unit,
    onExperienceLevelChanged: (String) -> Unit,
    onJobTypeChanged: (JobType) -> Unit,
    onWorkModeChanged: (WorkMode) -> Unit,
    onClearFilters: () -> Unit,
) {
    var showFilters by rememberSaveable { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .widthIn(max = 760.dp)
            .fillMaxWidth()
            .padding(horizontal = JobLinkSpacing.large),
        horizontalArrangement = Arrangement.spacedBy(JobLinkSpacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onSearchQueryChanged,
            modifier = Modifier.weight(1f),
            label = { Text(text = stringResource(R.string.search_jobs)) },
            placeholder = { Text(text = stringResource(R.string.search_jobs_hint)) },
            singleLine = true,
        )
        OutlinedButton(onClick = { showFilters = true }) {
            Text(
                text = if (uiState.activeFilterCount == 0) {
                    stringResource(R.string.filters)
                } else {
                    stringResource(R.string.filters_count, uiState.activeFilterCount)
                },
            )
        }
    }

    if (showFilters) {
        ModalBottomSheet(onDismissRequest = { showFilters = false }) {
            FilterSheetContent(
                uiState = uiState,
                onCategoryChanged = onCategoryChanged,
                onLocationChanged = onLocationChanged,
                onExperienceLevelChanged = onExperienceLevelChanged,
                onJobTypeChanged = onJobTypeChanged,
                onWorkModeChanged = onWorkModeChanged,
                onClearFilters = onClearFilters,
                onDone = { showFilters = false },
            )
        }
    }
}

@Composable
private fun FilterSheetContent(
    uiState: HomeUiState,
    onCategoryChanged: (String) -> Unit,
    onLocationChanged: (String) -> Unit,
    onExperienceLevelChanged: (String) -> Unit,
    onJobTypeChanged: (JobType) -> Unit,
    onWorkModeChanged: (WorkMode) -> Unit,
    onClearFilters: () -> Unit,
    onDone: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(
                start = JobLinkSpacing.large,
                end = JobLinkSpacing.large,
                bottom = JobLinkSpacing.large,
            ),
        verticalArrangement = Arrangement.spacedBy(JobLinkSpacing.medium),
    ) {
        Text(
            text = stringResource(R.string.filter_jobs),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        OutlinedTextField(
            value = uiState.categoryFilter,
            onValueChange = onCategoryChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = stringResource(R.string.job_category)) },
            singleLine = true,
        )
        OutlinedTextField(
            value = uiState.locationFilter,
            onValueChange = onLocationChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = stringResource(R.string.location)) },
            singleLine = true,
        )
        OutlinedTextField(
            value = uiState.experienceLevelFilter,
            onValueChange = onExperienceLevelChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = stringResource(R.string.experience_level)) },
            singleLine = true,
        )
        FilterChipGroup(
            title = stringResource(R.string.job_type),
            options = JobType.entries,
            selectedOption = uiState.jobTypeFilter,
            optionLabel = { it.displayLabel() },
            onOptionSelected = onJobTypeChanged,
        )
        FilterChipGroup(
            title = stringResource(R.string.work_mode),
            options = WorkMode.entries,
            selectedOption = uiState.workModeFilter,
            optionLabel = { it.displayLabel() },
            onOptionSelected = onWorkModeChanged,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(JobLinkSpacing.small),
        ) {
            TextButton(
                onClick = onClearFilters,
                enabled = uiState.hasActiveFilters,
                modifier = Modifier.weight(1f),
            ) {
                Text(text = stringResource(R.string.clear_filters))
            }
            Button(
                onClick = onDone,
                modifier = Modifier.weight(1f),
            ) {
                Text(text = stringResource(R.string.done))
            }
        }
    }
}

@Composable
private fun <T> FilterChipGroup(
    title: String,
    options: List<T>,
    selectedOption: T?,
    optionLabel: @Composable (T) -> String,
    onOptionSelected: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(JobLinkSpacing.small)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(JobLinkSpacing.small),
            verticalArrangement = Arrangement.spacedBy(JobLinkSpacing.small),
        ) {
            options.forEach { option ->
                FilterChip(
                    selected = option == selectedOption,
                    onClick = { onOptionSelected(option) },
                    label = { Text(text = optionLabel(option)) },
                )
            }
        }
    }
}

@Composable
private fun JobType.displayLabel(): String = stringResource(
    when (this) {
        JobType.FULL_TIME -> R.string.job_type_full_time
        JobType.PART_TIME -> R.string.job_type_part_time
        JobType.CONTRACT -> R.string.job_type_contract
        JobType.INTERNSHIP -> R.string.job_type_internship
        JobType.FREELANCE -> R.string.job_type_freelance
        JobType.TEMPORARY -> R.string.job_type_temporary
    },
)

@Composable
private fun WorkMode.displayLabel(): String = stringResource(
    when (this) {
        WorkMode.ONSITE -> R.string.work_mode_onsite
        WorkMode.REMOTE -> R.string.work_mode_remote
        WorkMode.HYBRID -> R.string.work_mode_hybrid
    },
)

@Composable
private fun HomeHeader(
    userName: String,
    onApplications: () -> Unit,
    onSavedJobs: () -> Unit,
    onProfile: () -> Unit,
    onLogout: () -> Unit,
) {
    Column(
        modifier = Modifier
            .widthIn(max = 760.dp)
            .fillMaxWidth()
            .padding(
                horizontal = JobLinkSpacing.large,
                vertical = JobLinkSpacing.medium,
            ),
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onProfile) {
                Text(text = stringResource(R.string.profile))
            }
            TextButton(onClick = onLogout) {
                Text(text = stringResource(R.string.logout))
            }
        }
        Text(
            text = stringResource(
                R.string.job_seeker_home_greeting,
                userName.substringBefore(" ").ifBlank { stringResource(R.string.job_seeker) },
            ),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(R.string.home_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(JobLinkSpacing.medium))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(JobLinkSpacing.medium),
        ) {
            DashboardShortcutCard(
                title = stringResource(R.string.saved_jobs),
                supportingText = stringResource(R.string.saved_jobs_shortcut),
                onClick = onSavedJobs,
                modifier = Modifier.weight(1f),
            )
            DashboardShortcutCard(
                title = stringResource(R.string.my_applications),
                supportingText = stringResource(R.string.applications_shortcut),
                onClick = onApplications,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun JobCategoryRow(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
) {
    if (categories.isEmpty()) return
    Column(
        modifier = Modifier.widthIn(max = 760.dp).fillMaxWidth(),
    ) {
        Text(
            text = stringResource(R.string.job_categories),
            modifier = Modifier.padding(
                start = JobLinkSpacing.large,
                end = JobLinkSpacing.large,
                top = JobLinkSpacing.medium,
            ),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = JobLinkSpacing.large),
            horizontalArrangement = Arrangement.spacedBy(JobLinkSpacing.small),
        ) {
            categories.forEach { category ->
                FilterChip(
                    selected = category.equals(selectedCategory, ignoreCase = true),
                    onClick = { onCategorySelected(category) },
                    label = { Text(category) },
                )
            }
        }
    }
}

@Composable
private fun JobFeed(
    uiState: HomeUiState,
    onJobClick: (String) -> Unit,
    onRetry: () -> Unit,
    onClearFilters: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(JobLinkSpacing.large),
        verticalArrangement = Arrangement.spacedBy(JobLinkSpacing.medium),
    ) {
        if (uiState.error != null) {
            item {
                JobFeedErrorMessage(error = uiState.error)
                TextButton(onClick = onRetry) {
                    Text(text = stringResource(R.string.refresh))
                }
            }
        }
        if (uiState.jobs.isEmpty()) {
            item {
                EmptyJobFeed(
                    isFiltered = uiState.hasActiveFilters,
                    onClearFilters = onClearFilters,
                )
            }
        } else {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(JobLinkSpacing.extraSmall)) {
                    Text(
                        text = stringResource(R.string.recommended_for_you),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(R.string.recommendation_explanation),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(uiState.jobs, key = { it.id }) { job ->
                JobCard(
                    job = job,
                    onClick = { onJobClick(job.id) },
                    isSaved = job.id in uiState.savedJobIds,
                    recommendation = uiState.recommendations[job.id],
                )
            }
        }
    }
}

@Composable
private fun JobFeedLoading() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(JobLinkSpacing.medium),
        ) {
            CircularProgressIndicator()
            Text(text = stringResource(R.string.loading_jobs))
        }
    }
}

@Composable
private fun EmptyJobFeed(
    isFiltered: Boolean,
    onClearFilters: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = JobLinkSpacing.section),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(
                if (isFiltered) R.string.no_matching_jobs_title else R.string.no_jobs_title,
            ),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(
                if (isFiltered) R.string.no_matching_jobs_message else R.string.no_jobs_message,
            ),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (isFiltered) {
            TextButton(onClick = onClearFilters) {
                Text(text = stringResource(R.string.clear_filters))
            }
        }
    }
}

@Composable
private fun JobFeedError(
    error: JobError,
    onRetry: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(JobLinkSpacing.large),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 560.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            JobFeedErrorMessage(error = error)
            Button(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(R.string.retry))
            }
        }
    }
}

@Composable
internal fun JobFeedErrorMessage(
    error: JobError,
    modifier: Modifier = Modifier,
) {
    val message = when (error) {
        JobError.PERMISSION_DENIED -> R.string.job_feed_error_permission
        JobError.NETWORK -> R.string.job_feed_error_network
        JobError.JOB_NOT_FOUND -> R.string.job_details_not_found
        else -> R.string.job_feed_error_unknown
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Assertive },
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.medium,
    ) {
        Text(
            text = stringResource(message),
            modifier = Modifier.padding(JobLinkSpacing.medium),
        )
    }
}
