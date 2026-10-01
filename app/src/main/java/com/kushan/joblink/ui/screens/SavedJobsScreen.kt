package com.kushan.joblink.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
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
import com.kushan.joblink.data.repository.JobError
import com.kushan.joblink.ui.components.JobCard
import com.kushan.joblink.ui.theme.JobLinkSpacing
import com.kushan.joblink.viewmodel.SavedJobsUiState
import com.kushan.joblink.viewmodel.SavedJobsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedJobsScreen(
    viewModel: SavedJobsViewModel,
    onJobClick: (String) -> Unit,
    onBack: () -> Unit,
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
        SavedJobsHeader(onBack = onBack)
        when {
            uiState.isLoading -> SavedJobsLoading()
            error != null && uiState.jobs.isEmpty() -> SavedJobsError(
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
                SavedJobsList(
                    uiState = uiState,
                    onJobClick = onJobClick,
                    onRetry = viewModel::refresh,
                )
            }
        }
    }
}

@Composable
private fun SavedJobsHeader(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .widthIn(max = 760.dp)
            .fillMaxWidth()
            .padding(horizontal = JobLinkSpacing.large),
    ) {
        TextButton(onClick = onBack) {
            Text(text = stringResource(R.string.back))
        }
        Text(
            text = stringResource(R.string.saved_jobs),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(R.string.saved_jobs_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(JobLinkSpacing.small))
    }
}

@Composable
private fun SavedJobsList(
    uiState: SavedJobsUiState,
    onJobClick: (String) -> Unit,
    onRetry: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(JobLinkSpacing.large),
        verticalArrangement = Arrangement.spacedBy(JobLinkSpacing.medium),
    ) {
        uiState.error?.let { error ->
            item {
                SavedJobsErrorMessage(error = error)
                TextButton(onClick = onRetry) {
                    Text(text = stringResource(R.string.refresh))
                }
            }
        }
        if (uiState.jobs.isEmpty()) {
            item { SavedJobsEmpty() }
        } else {
            items(uiState.jobs, key = { it.id }) { job ->
                JobCard(
                    job = job,
                    onClick = { onJobClick(job.id) },
                    isSaved = true,
                )
            }
        }
    }
}

@Composable
private fun SavedJobsEmpty() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = JobLinkSpacing.section),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.no_saved_jobs_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(R.string.no_saved_jobs_message),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SavedJobsLoading() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(JobLinkSpacing.medium),
        ) {
            CircularProgressIndicator()
            Text(text = stringResource(R.string.loading_saved_jobs))
        }
    }
}

@Composable
private fun SavedJobsError(
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
            SavedJobsErrorMessage(error = error)
            Spacer(modifier = Modifier.height(JobLinkSpacing.medium))
            Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.retry))
            }
        }
    }
}

@Composable
private fun SavedJobsErrorMessage(error: JobError) {
    val message = when (error) {
        JobError.NOT_AUTHENTICATED -> R.string.save_job_error_auth
        JobError.ACCOUNT_NOT_FOUND -> R.string.saved_jobs_error_account
        JobError.WRONG_ROLE -> R.string.saved_jobs_error_role
        JobError.PERMISSION_DENIED -> R.string.save_job_error_permission
        JobError.NETWORK -> R.string.saved_jobs_error_network
        else -> R.string.saved_jobs_error_unknown
    }
    Surface(
        modifier = Modifier
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
