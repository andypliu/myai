package com.example.myai.presentation.chat

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

class MessageBubbleShape(
    private val isUser: Boolean
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val cornerRadius = with(density) { 16.dp.toPx() }
        val width = size.width
        val height = size.height

        val path = Path().apply {
            val topLeft = cornerRadius
            val topRight = cornerRadius
            val bottomRight = if (isUser) 0f else cornerRadius
            val bottomLeft = if (isUser) cornerRadius else 0f

            // Start from top-left corner
            moveTo(0f, topLeft)
            arcTo(
                rect = Rect(0f, 0f, topLeft * 2, topLeft * 2),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )

            // Top edge
            lineTo(width - topRight, 0f)
            // Top-right corner
            arcTo(
                rect = Rect(width - topRight * 2, 0f, width, topRight * 2),
                startAngleDegrees = -90f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )

            // Right edge
            lineTo(width, height - bottomRight)
            // Bottom-right corner
            if (bottomRight > 0f) {
                arcTo(
                    rect = Rect(width - bottomRight * 2, height - bottomRight * 2, width, height),
                    startAngleDegrees = 0f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
            } else {
                lineTo(width, height)
            }

            // Bottom edge
            lineTo(bottomLeft, height)
            // Bottom-left corner
            if (bottomLeft > 0f) {
                arcTo(
                    rect = Rect(0f, height - bottomLeft * 2, bottomLeft * 2, height),
                    startAngleDegrees = 90f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
            } else {
                lineTo(0f, height)
            }

            // Left edge
            lineTo(0f, topLeft)

            close()
        }

        return Outline.Generic(path)
    }
}
