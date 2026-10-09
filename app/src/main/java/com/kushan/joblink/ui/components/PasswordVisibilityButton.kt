package com.kushan.joblink.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun PasswordVisibilityButton(
    passwordVisible: Boolean,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.semantics { this.contentDescription = contentDescription },
        enabled = enabled,
    ) {
        val color = LocalContentColor.current

        Canvas(
            modifier = Modifier.size(24.dp),
        ) {
            val strokeWidth = 2.dp.toPx()
            val eyePath = Path().apply {
                moveTo(size.width * 0.08f, size.height * 0.50f)
                cubicTo(
                    size.width * 0.27f,
                    size.height * 0.18f,
                    size.width * 0.73f,
                    size.height * 0.18f,
                    size.width * 0.92f,
                    size.height * 0.50f,
                )
                cubicTo(
                    size.width * 0.73f,
                    size.height * 0.82f,
                    size.width * 0.27f,
                    size.height * 0.82f,
                    size.width * 0.08f,
                    size.height * 0.50f,
                )
                close()
            }

            drawPath(
                path = eyePath,
                color = color,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
            drawCircle(
                color = color,
                radius = size.minDimension * 0.13f,
                center = center,
                style = Stroke(width = strokeWidth),
            )

            if (passwordVisible) {
                drawLine(
                    color = color,
                    start = Offset(size.width * 0.16f, size.height * 0.16f),
                    end = Offset(size.width * 0.84f, size.height * 0.84f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}
