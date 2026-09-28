package com.kushan.joblink.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kushan.joblink.R

@Composable
fun WelcomeScreen(
    onContinue: () -> Unit,
    onLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.app_name),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.welcome_tagline),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(32.dp))
            CareerVisual()
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text(text = stringResource(R.string.get_started))
            }
            TextButton(onClick = onLogin) {
                Text(text = stringResource(R.string.login))
            }
        }
    }
}

@Composable
private fun CareerVisual(modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val secondary = MaterialTheme.colorScheme.secondary
    val description = stringResource(R.string.welcome_visual_description)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 180.dp, max = 240.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        tonalElevation = 1.dp,
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(36.dp)
                .semantics { contentDescription = description },
        ) {
            val shortestSide = size.minDimension
            val strokeWidth = shortestSide * 0.055f
            val bodyTop = size.height * 0.30f
            val bodyLeft = size.width * 0.08f
            val bodyWidth = size.width * 0.84f
            val bodyHeight = size.height * 0.58f
            val handleWidth = size.width * 0.34f
            val handleHeight = size.height * 0.24f
            val handleLeft = (size.width - handleWidth) / 2f

            drawCircle(
                color = secondary.copy(alpha = 0.18f),
                radius = shortestSide * 0.25f,
                center = Offset(size.width * 0.82f, size.height * 0.20f),
            )
            drawRoundRect(
                color = primary,
                topLeft = Offset(handleLeft, size.height * 0.09f),
                size = Size(handleWidth, handleHeight),
                cornerRadius = CornerRadius(shortestSide * 0.06f),
                style = Stroke(width = strokeWidth),
            )
            drawRoundRect(
                color = primary,
                topLeft = Offset(bodyLeft, bodyTop),
                size = Size(bodyWidth, bodyHeight),
                cornerRadius = CornerRadius(shortestSide * 0.08f),
            )
            drawLine(
                color = onPrimary.copy(alpha = 0.65f),
                start = Offset(bodyLeft, bodyTop + bodyHeight * 0.42f),
                end = Offset(bodyLeft + bodyWidth, bodyTop + bodyHeight * 0.42f),
                strokeWidth = strokeWidth * 0.55f,
            )

            val latchSize = Size(shortestSide * 0.08f, shortestSide * 0.10f)
            val latchTop = bodyTop + bodyHeight * 0.36f
            listOf(size.width * 0.34f, size.width * 0.66f).forEach { centerX ->
                drawRoundRect(
                    color = onPrimary,
                    topLeft = Offset(centerX - latchSize.width / 2f, latchTop),
                    size = latchSize,
                    cornerRadius = CornerRadius(shortestSide * 0.02f),
                )
            }
        }
    }
}
