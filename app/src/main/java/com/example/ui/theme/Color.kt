package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Dark Editorial Obsidian Palette
val ObsidianBg = Color(0xFF0B0F19)
val ObsidianSurface = Color(0xFF131B2E)
val ObsidianSurfaceVariant = Color(0xFF1E293B)
val ObsidianOutline = Color(0xFF334155)
val AmberPrimaryDark = Color(0xFFF59E0B)
val AmberOnPrimaryDark = Color(0xFF1E1300)
val AmberContainerDark = Color(0xFF452B03)
val AmberOnContainerDark = Color(0xFFFDE68A)
val TealSecondaryDark = Color(0xFF14B8A6)
val TealContainerDark = Color(0xFF0F3D38)
val RoseTertiaryDark = Color(0xFFF43F5E)
val TextPrimaryDark = Color(0xFFF8FAFC)
val TextSecondaryDark = Color(0xFF94A3B8)

// Light Warm Alabaster Palette
val AlabasterBg = Color(0xFFFAF8F5)
val AlabasterSurface = Color(0xFFFFFFFF)
val AlabasterSurfaceVariant = Color(0xFFF1EDE6)
val AlabasterOutline = Color(0xFFD6D0C4)
val AmberPrimaryLight = Color(0xFFD97706)
val AmberOnPrimaryLight = Color(0xFFFFFFFF)
val AmberContainerLight = Color(0xFFFEF3C7)
val AmberOnContainerLight = Color(0xFF78350F)
val TealSecondaryLight = Color(0xFF0D9488)
val TealContainerLight = Color(0xFFCCFBF1)
val RoseTertiaryLight = Color(0xFFE11D48)
val TextPrimaryLight = Color(0xFF0F172A)
val TextSecondaryLight = Color(0xFF475569)

// Note Box Highlight Outline Colors
val HighlightYellowOutlineDark = Color(0xFFFACC15)
val HighlightYellowOutlineLight = Color(0xFFEAB308)

// Note Card Accent Swatches
data class NoteColorOption(
    val hex: String,
    val label: String,
    val darkSurface: Color,
    val lightSurface: Color,
    val accent: Color
)

val NoteColorPalette = listOf(
    NoteColorOption("#1E293B", "Slate", Color(0xFF162032), Color(0xFFFFFFFF), Color(0xFF64748B)),
    NoteColorOption("#F59E0B", "Amber", Color(0xFF291D0A), Color(0xFFFFFBEB), Color(0xFFF59E0B)),
    NoteColorOption("#10B981", "Emerald", Color(0xFF0B2620), Color(0xFFECFDF5), Color(0xFF10B981)),
    NoteColorOption("#3B82F6", "Cobalt", Color(0xFF0F213C), Color(0xFFEFF6FF), Color(0xFF3B82F6)),
    NoteColorOption("#EC4899", "Rose", Color(0xFF2B1224), Color(0xFFFDF2F8), Color(0xFFEC4899)),
    NoteColorOption("#8B5CF6", "Violet", Color(0xFF1F1638), Color(0xFFF5F3FF), Color(0xFF8B5CF6))
)
