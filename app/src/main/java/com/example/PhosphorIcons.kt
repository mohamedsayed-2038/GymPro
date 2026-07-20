package com.example

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object PhosphorIcons {

    private fun buildIcon(name: String, strokeWidth: Float = 1.35f, block: PathBuilder.() -> Unit): ImageVector {
        return ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = strokeWidth,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            block()
        }.build()
    }

    val Microphone = buildIcon("Microphone") {
        moveTo(12f, 2f)
        arcTo(4f, 4f, 0f, false, false, 8f, 6f)
        verticalLineTo(11f)
        arcTo(4f, 4f, 0f, false, false, 16f, 11f)
        verticalLineTo(6f)
        arcTo(4f, 4f, 0f, false, false, 12f, 2f)
        close()
        moveTo(5f, 10f)
        verticalLineTo(11f)
        arcToRelative(7f, 7f, 0f, false, false, 6f, 6.9f)
        verticalLineTo(21f)
        horizontalLineTo(9f)
        verticalLineTo(23f)
        horizontalLineTo(15f)
        verticalLineTo(21f)
        horizontalLineTo(13f)
        verticalLineTo(17.9f)
        arcToRelative(7f, 7f, 0f, false, false, 6f, -6.9f)
        verticalLineTo(10f)
        horizontalLineTo(17f)
        verticalLineTo(11f)
        arcToRelative(5f, 5f, 0f, false, true, -10f, 0f)
        verticalLineTo(10f)
        close()
    }

    // Workouts (Dumbbell)
    val Dumbbell = buildIcon("Dumbbell") {
        // Left plate
        moveTo(3f, 7f)
        verticalLineTo(17f)
        horizontalLineTo(5f)
        verticalLineTo(7f)
        close()
        // Right plate
        moveTo(19f, 7f)
        verticalLineTo(17f)
        horizontalLineTo(21f)
        verticalLineTo(7f)
        close()
        // Bar
        moveTo(5f, 12f)
        horizontalLineTo(19f)
        // Secondary plates for realism
        moveTo(2f, 9f)
        verticalLineTo(15f)
        moveTo(22f, 9f)
        verticalLineTo(15f)
    }

    // Timer
    val Timer = buildIcon("Timer") {
        // Outer face
        moveTo(12f, 13f)
        moveTo(12f, 4f)
        arcTo(9f, 9f, 0f, true, true, 11.99f, 4f)
        close()
        // Hands
        moveTo(12f, 13f)
        verticalLineTo(8f)
        moveTo(12f, 13f)
        lineTo(16f, 13f)
        // Chrono stem
        moveTo(12f, 4f)
        verticalLineTo(2f)
        moveTo(10f, 2f)
        horizontalLineTo(14f)
    }

    // Fork & Knife (Meal tab)
    val ForkKnife = buildIcon("ForkKnife") {
        // Fork
        moveTo(5f, 3f)
        verticalLineTo(10f)
        arcTo(2f, 2f, 0f, false, false, 9f, 10f)
        verticalLineTo(3f)
        // Fork prongs
        moveTo(7f, 3f)
        verticalLineTo(8f)
        // Fork handle
        moveTo(7f, 12f)
        verticalLineTo(21f)

        // Knife
        moveTo(17f, 21f)
        verticalLineTo(11f)
        moveTo(17f, 11f)
        arcTo(4f, 4f, 0f, false, false, 17f, 3f)
        verticalLineTo(11f)
        horizontalLineTo(15f)
        verticalLineTo(21f)
        horizontalLineTo(17f)
    }

    // Camera (Scan / AI Analyzer)
    val Camera = buildIcon("Camera") {
        // Body
        moveTo(3f, 7f)
        horizontalLineTo(7f)
        lineTo(9f, 4f)
        horizontalLineTo(15f)
        lineTo(17f, 7f)
        horizontalLineTo(21f)
        arcTo(2f, 2f, 0f, false, true, 23f, 9f)
        verticalLineTo(19f)
        arcTo(2f, 2f, 0f, false, true, 21f, 21f)
        horizontalLineTo(3f)
        arcTo(2f, 2f, 0f, false, true, 1f, 19f)
        verticalLineTo(9f)
        arcTo(2f, 2f, 0f, false, true, 3f, 7f)
        close()
        // Lens
        moveTo(12f, 14f)
        moveTo(12f, 10f)
        arcTo(4f, 4f, 0f, true, true, 11.99f, 10f)
        close()
    }

    // Info (About Dialog)
    val Info = buildIcon("Info") {
        // Outer boundary
        moveTo(12f, 12f)
        moveTo(12f, 3f)
        arcTo(9f, 9f, 0f, true, true, 11.99f, 3f)
        close()
        // Info lines
        moveTo(12f, 7f)
        verticalLineTo(8f)
        moveTo(12f, 11f)
        verticalLineTo(17f)
    }

    // Volume Up
    val VolumeUp = buildIcon("VolumeUp") {
        // Horn
        moveTo(4f, 9f)
        horizontalLineTo(8f)
        lineTo(13f, 4f)
        verticalLineTo(20f)
        lineTo(8f, 15f)
        horizontalLineTo(4f)
        close()
        // Audio Waves
        moveTo(16f, 8f)
        arcTo(4f, 4f, 0f, false, true, 16f, 16f)
        moveTo(19f, 5f)
        arcTo(7.5f, 7.5f, 0f, false, true, 19f, 19f)
    }

    // Volume Off
    val VolumeOff = buildIcon("VolumeOff") {
        // Horn
        moveTo(4f, 9f)
        horizontalLineTo(8f)
        lineTo(13f, 4f)
        verticalLineTo(20f)
        lineTo(8f, 15f)
        horizontalLineTo(4f)
        close()
        // Cross / Off action
        moveTo(16f, 10f)
        lineTo(20f, 14f)
        moveTo(20f, 10f)
        lineTo(16f, 14f)
    }

    // Vibration
    val Vibration = buildIcon("Vibration") {
        // Device body
        moveTo(8f, 3f)
        horizontalLineTo(16f)
        verticalLineTo(21f)
        horizontalLineTo(8f)
        close()
        // Signal waves
        moveTo(4f, 7f)
        arcTo(14f, 14f, 0f, false, false, 4f, 17f)
        moveTo(20f, 7f)
        arcTo(14f, 14f, 0f, false, true, 20f, 17f)
        // Home point
        moveTo(12f, 18f)
        lineTo(12.01f, 18f)
    }

    // Remove
    val Remove = buildIcon("Remove") {
        moveTo(5f, 12f)
        horizontalLineTo(19f)
    }

    // Add
    val Add = buildIcon("Add") {
        moveTo(12f, 5f)
        verticalLineTo(19f)
        moveTo(5f, 12f)
        horizontalLineTo(19f)
    }

    // Back Arrow
    val ArrowBack = buildIcon("ArrowBack") {
        moveTo(20f, 12f)
        horizontalLineTo(4f)
        moveTo(10f, 6f)
        lineTo(4f, 12f)
        lineTo(10f, 18f)
    }

    // Play
    val Play = buildIcon("Play") {
        moveTo(7f, 5f)
        lineTo(18f, 12f)
        lineTo(7f, 19f)
        close()
    }

    // Pause
    val Pause = buildIcon("Pause") {
        moveTo(8f, 5f)
        verticalLineTo(19f)
        moveTo(16f, 5f)
        verticalLineTo(19f)
    }

    // Skip Next
    val SkipNext = buildIcon("SkipNext") {
        moveTo(5f, 5f)
        lineTo(14f, 12f)
        lineTo(5f, 19f)
        close()
        moveTo(18f, 5f)
        verticalLineTo(19f)
    }

    // Trophy / Achievement Cup
    val Trophy = buildIcon("Trophy") {
        // Cup container
        moveTo(5f, 4f)
        horizontalLineTo(19f)
        verticalLineTo(11f)
        arcTo(7f, 7f, 0f, false, true, 12f, 18f)
        arcTo(7f, 7f, 0f, false, true, 5f, 11f)
        close()
        // Handles
        moveTo(5f, 6f)
        horizontalLineTo(3f)
        verticalLineTo(10f)
        horizontalLineTo(5f)
        moveTo(19f, 6f)
        horizontalLineTo(21f)
        verticalLineTo(10f)
        horizontalLineTo(19f)
        // Stem and pedestal
        moveTo(12f, 18f)
        verticalLineTo(21f)
        moveTo(8f, 21f)
        horizontalLineTo(16f)
    }

    // Chat
    val Chat = buildIcon("Chat") {
        // Smooth chat dynamic bubble
        moveTo(20f, 4f)
        horizontalLineTo(4f)
        arcTo(2f, 2f, 0f, false, false, 2f, 6f)
        verticalLineTo(16f)
        arcTo(2f, 2f, 0f, false, false, 4f, 18f)
        horizontalLineTo(8f)
        lineTo(12f, 21f)
        lineTo(16f, 18f)
        horizontalLineTo(20f)
        arcTo(2f, 2f, 0f, false, false, 22f, 16f)
        verticalLineTo(6f)
        arcTo(2f, 2f, 0f, false, false, 20f, 4f)
        close()
    }

    // Upload
    val Upload = buildIcon("Upload") {
        moveTo(12f, 19f)
        verticalLineTo(5f)
        moveTo(7f, 10f)
        lineTo(12f, 5f)
        lineTo(17f, 10f)
        // Pedestal line
        moveTo(4f, 21f)
        horizontalLineTo(20f)
    }

    // Check Action
    val Check = buildIcon("Check") {
        moveTo(4f, 12f)
        lineTo(9f, 17f)
        lineTo(20f, 6f)
    }

    // Flame (Calories burner)
    val Flame = buildIcon("Flame") {
        moveTo(12f, 21f)
        arcTo(7f, 7f, 0f, false, true, 5f, 14f)
        arcTo(7f, 7f, 0f, false, false, 12f, 5f)
        arcTo(7f, 7f, 0f, false, true, 12f, 14f)
        arcTo(3f, 3f, 0f, false, false, 15f, 11f)
        arcTo(5f, 5f, 0f, false, true, 19f, 15f)
        arcTo(7f, 7f, 0f, false, true, 12f, 21f)
        close()
    }

    // Leaf (Rest / Healthy vibe)
    val Leaf = buildIcon("Leaf") {
        moveTo(2f, 22f)
        lineTo(11f, 13f)
        moveTo(11f, 13f)
        arcTo(6.5f, 6.5f, 0f, false, true, 21.5f, 3.5f)
        arcTo(6.5f, 6.5f, 0f, false, true, 11f, 13f)
        close()
        // leaf veins
        moveTo(14f, 10f)
        lineTo(18f, 9f)
        moveTo(8f, 16f)
        lineTo(11f, 18f)
    }

    // Dumbbell variant / Weight (For muscle indicator list context)
    val Weight = buildIcon("Weight") {
        moveTo(12f, 3f)
        horizontalLineTo(10f)
        arcTo(2f, 2f, 0f, false, false, 8f, 5f)
        verticalLineTo(7f)
        horizontalLineTo(16f)
        verticalLineTo(5f)
        arcTo(2f, 2f, 0f, false, false, 14f, 3f)
        close()
        moveTo(5f, 7f)
        horizontalLineTo(19f)
        lineTo(21f, 19f)
        arcTo(2f, 2f, 0f, false, true, 19f, 21f)
        horizontalLineTo(5f)
        arcTo(2f, 2f, 0f, false, true, 3f, 19f)
        close()
    }

    // CaretDown for collapsible accordions
    val CaretDown = buildIcon("CaretDown") {
        moveTo(6f, 9f)
        lineTo(12f, 15f)
        lineTo(18f, 9f)
    }

    // CaretUp for collapsible accordions
    val CaretUp = buildIcon("CaretUp") {
        moveTo(6f, 15f)
        lineTo(12f, 9f)
        lineTo(18f, 15f)
    }

    // Copy icon / Double papers overlapping
    val Copy = buildIcon("Copy") {
        moveTo(4f, 8f)
        horizontalLineTo(14f)
        verticalLineTo(18f)
        horizontalLineTo(4f)
        close()
        moveTo(8f, 4f)
        horizontalLineTo(18f)
        verticalLineTo(14f)
    }

    // Glass Material isometric Layers icon
    val Layers = buildIcon("Layers") {
        // Top layer:
        moveTo(12f, 2f)
        lineTo(21f, 6.5f)
        lineTo(12f, 11f)
        lineTo(3f, 6.5f)
        close()
        // Middle layer:
        moveTo(3f, 11f)
        lineTo(12f, 15.5f)
        lineTo(21f, 11f)
        // Bottom layer:
        moveTo(3f, 15.5f)
        lineTo(12f, 20f)
        lineTo(21f, 15.5f)
    }
}
