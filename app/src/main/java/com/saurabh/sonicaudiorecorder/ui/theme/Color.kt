@file:Suppress("unused", "RedundantSuppression")

package com.saurabh.sonicaudiorecorder.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Deep Studio Palette - Sonic Audio Recorder
 * Modern Dark-first Audio Studio Palette with High-Contrast Readability (WCAG AA).
 */

// Sonic Branding
val SonicPurple = Color(0xFF9D4EDD)
val SonicPurpleLight = Color(0xFF7B2CBF)
val SonicViolet = Color(0xFFC77DFF)
val SonicPink = Color(0xFFF72585)
val SonicPinkLight = Color(0xFFD80032)
val SonicGradient = Brush.linearGradient(listOf(SonicPurple, SonicPink))

// Dark Theme - Deep Studio Palette
val StudioDarkBg = Color(0xFF0B0E14)
val StudioDarkSurface = Color(0xFF151B26)
val StudioDarkContainer = Color(0xFF1F2738)
val DarkTextPrimary = Color(0xFFF3F4F6)
val DarkTextSecondary = Color(0xFF9CA3AF)
val DarkBorder = Color(0xFF2A3447)

// Light Theme - Clean Studio Palette
val StudioLightBg = Color(0xFFF8FAFC)
val StudioLightSurface = Color(0xFFFFFFFF)
val StudioLightContainer = Color(0xFFF1F5F9)
val LightTextPrimary = Color(0xFF0F172A)
val LightTextSecondary = Color(0xFF64748B)
val LightBorder = Color(0xFFCBD5E1)

// Legacy aliases for backward compatibility
val DeepDarkBg = StudioDarkBg
val DeepDarkSurface = StudioDarkSurface
val DarkTextMetadata = DarkTextSecondary

// Material 3 Mapping Colors - Dark
val md_theme_dark_primary = SonicPurple
val md_theme_dark_onPrimary = Color(0xFFFFFFFF)
val md_theme_dark_primaryContainer = Color(0xFF381460)
val md_theme_dark_onPrimaryContainer = SonicViolet
val md_theme_dark_secondary = SonicViolet
val md_theme_dark_onSecondary = Color(0xFF1F004D)
val md_theme_dark_secondaryContainer = StudioDarkContainer
val md_theme_dark_onSecondaryContainer = DarkTextPrimary
val md_theme_dark_error = Color(0xFFFF3B30)
val md_theme_dark_onError = Color(0xFFFFFFFF)
val md_theme_dark_background = StudioDarkBg
val md_theme_dark_onBackground = DarkTextPrimary
val md_theme_dark_surface = StudioDarkSurface
val md_theme_dark_onSurface = DarkTextPrimary
val md_theme_dark_surfaceVariant = StudioDarkContainer
val md_theme_dark_onSurfaceVariant = DarkTextSecondary
val md_theme_dark_outline = DarkBorder
val md_theme_dark_surfaceContainerHigh = Color(0xFF252E42)

// Material 3 Mapping Colors - Light
val md_theme_light_primary = SonicPurpleLight
val md_theme_light_onPrimary = Color(0xFFFFFFFF)
val md_theme_light_primaryContainer = Color(0xFFF3E8FF)
val md_theme_light_onPrimaryContainer = Color(0xFF3B007D)
val md_theme_light_secondary = Color(0xFF6B21A8)
val md_theme_light_onSecondary = Color(0xFFFFFFFF)
val md_theme_light_secondaryContainer = StudioLightContainer
val md_theme_light_onSecondaryContainer = LightTextPrimary
val md_theme_light_error = Color(0xFFD32F2F)
val md_theme_light_onError = Color(0xFFFFFFFF)
val md_theme_light_background = StudioLightBg
val md_theme_light_onBackground = LightTextPrimary
val md_theme_light_surface = StudioLightSurface
val md_theme_light_onSurface = LightTextPrimary
val md_theme_light_surfaceVariant = StudioLightContainer
val md_theme_light_onSurfaceVariant = LightTextSecondary
val md_theme_light_outline = LightBorder
val md_theme_light_surfaceContainerHigh = Color(0xFFE2E8F0)

/**
 * Sonic Semantic Color Tokens
 * Type-safe custom color roles for audio recording and playback states.
 */
@Immutable
data class SonicColors(
    val recordingActive: Color,
    val recordingPaused: Color,
    val waveformPlayed: Color,
    val waveformUnplayed: Color,
    val playheadCursor: Color,
    val successState: Color,
    val errorState: Color
)

val DarkSonicColors = SonicColors(
    recordingActive = SonicPink,
    recordingPaused = Color(0xFFFF9500),
    waveformPlayed = SonicPurple,
    waveformUnplayed = DarkBorder,
    playheadCursor = SonicViolet,
    successState = Color(0xFF4CAF50),
    errorState = md_theme_dark_error
)

val LightSonicColors = SonicColors(
    recordingActive = SonicPinkLight,
    recordingPaused = Color(0xFFD97706),
    waveformPlayed = SonicPurpleLight,
    waveformUnplayed = LightBorder,
    playheadCursor = SonicPurpleLight,
    successState = Color(0xFF2E7D32),
    errorState = md_theme_light_error
)

val LocalSonicColors = staticCompositionLocalOf { LightSonicColors }

val MaterialTheme.sonicColors: SonicColors
    @Composable
    @ReadOnlyComposable
    get() = LocalSonicColors.current

