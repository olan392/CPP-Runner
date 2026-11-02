package com.example.ui.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

object IconHelper {
    val availableIcons = listOf(
        "terminal" to Icons.Default.Terminal,
        "sports_esports" to Icons.Default.Gamepad,
        "calculate" to Icons.Default.Calculate,
        "shield" to Icons.Default.Shield,
        "school" to Icons.Default.School,
        "swap_horiz" to Icons.Default.SwapHoriz,
        "functions" to Icons.Default.Functions,
        "code" to Icons.Default.Code,
        "rocket_launch" to Icons.Default.RocketLaunch,
        "memory" to Icons.Default.Memory,
        "build" to Icons.Default.Build,
        "widgets" to Icons.Default.Widgets
    )

    val availableColors = listOf(
        "#00D4FF", // Cyan
        "#10B981", // Emerald
        "#8B5CF6", // Purple
        "#F59E0B", // Amber
        "#EC4899", // Pink
        "#3B82F6", // Blue
        "#EF4444"  // Red
    )

    fun getIcon(key: String): ImageVector {
        return availableIcons.find { it.first == key }?.second ?: Icons.Default.Terminal
    }

    fun parseColor(hex: String, defaultColor: Color = Color(0xFF00D4FF)): Color {
        return try {
            val cleanHex = hex.replace("#", "")
            val colorInt = cleanHex.toLong(16)
            if (cleanHex.length == 6) {
                Color(0xFF000000 or colorInt)
            } else {
                Color(colorInt)
            }
        } catch (e: Exception) {
            defaultColor
        }
    }
}
