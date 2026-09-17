package com.example.model

import androidx.compose.ui.graphics.Color

enum class ReaderTheme(
    val displayName: String,
    val background: Color,
    val surface: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val isDark: Boolean
) {
    PAPER_LIGHT(
        displayName = "Paper Light",
        background = Color(0xFFF9F6EE),
        surface = Color(0xFFFFFFFF),
        textPrimary = Color(0xFF2C241E),
        textSecondary = Color(0xFF786C60),
        accent = Color(0xFFC05621),
        isDark = false
    ),
    SLATE_DARK(
        displayName = "Slate Dark",
        background = Color(0xFF1E222B),
        surface = Color(0xFF282E3A),
        textPrimary = Color(0xFFE2E8F0),
        textSecondary = Color(0xFF94A3B8),
        accent = Color(0xFF38BDF8),
        isDark = true
    ),
    MIDNIGHT_BLACK(
        displayName = "Midnight Black",
        background = Color(0xFF000000),
        surface = Color(0xFF121212),
        textPrimary = Color(0xFFF8FAFC),
        textSecondary = Color(0xFF64748B),
        accent = Color(0xFFF59E0B),
        isDark = true
    )
}
