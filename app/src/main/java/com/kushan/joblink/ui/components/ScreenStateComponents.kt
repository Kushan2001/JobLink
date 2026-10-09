package com.kushan.joblink.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kushan.joblink.ui.theme.JobLinkSpacing

/**
 * A quiet, content-shaped loading state for card lists.
 *
 * The placeholders are intentionally static: they communicate the expected layout without adding
 * distracting shimmer animations. Assistive technology receives only the supplied loading label.
 */
@Composable
fun ListLoadingState(
    label: String,
    modifier: Modifier = Modifier,
    placeholderCount: Int = 3,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .clearAndSetSemantics { contentDescription = label },
        contentPadding = PaddingValues(JobLinkSpacing.large),
        verticalArrangement = Arrangement.spacedBy(JobLinkSpacing.medium),
        userScrollEnabled = false,
    ) {
        item {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items((0 until placeholderCount).toList()) {
            LoadingCardPlaceholder()
        }
    }
}

@Composable
private fun LoadingCardPlaceholder() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.padding(JobLinkSpacing.large)) {
            PlaceholderLine(widthFraction = 0.7f, height = 22.dp)
            Spacer(Modifier.height(JobLinkSpacing.small))
            PlaceholderLine(widthFraction = 0.45f, height = 16.dp)
            Spacer(Modifier.height(JobLinkSpacing.large))
            PlaceholderLine(widthFraction = 0.9f, height = 14.dp)
            Spacer(Modifier.height(JobLinkSpacing.extraSmall))
            PlaceholderLine(widthFraction = 0.6f, height = 14.dp)
        }
    }
}

@Composable
private fun PlaceholderLine(widthFraction: Float, height: Dp) {
    Surface(
        modifier = Modifier
            .fillMaxWidth(widthFraction)
            .height(height),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
    ) {}
}
