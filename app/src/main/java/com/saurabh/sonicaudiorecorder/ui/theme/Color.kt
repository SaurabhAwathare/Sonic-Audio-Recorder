@file:Suppress("unused", "unused", "RedundantSuppression", "RedundantSuppression")

package com.saurabh.sonicaudiorecorder.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Premium Modern Palette - Sonic Audio Recorder
 * Focus on Dark Mode depth and High-Contrast Readability.
 */

// Sonic Branding
val SonicPurple = Color(0xFF7F00FF)
val SonicPink = Color(0xFFE100FF)
val SonicGradient = Brush.linearGradient(listOf(SonicPurple, SonicPink))

// Dark Theme - Premium Deep Palette
val DeepDarkBg = Color(0xFF0D1117)
val DeepDarkSurface = Color(0xFF1A2233)
val DarkTextPrimary = Color(0xFFFFFFFF) 
val DarkTextSecondary = Color(0xFFB0B3B8)
val DarkTextMetadata = Color(0xFF9AA0A6)
val DarkBorder = Color(0xFF30363D)

// Light Theme - Clean Soft Palette
val LightBg = Color(0xFFF8FAFC)
val LightSurface = Color(0xFFFFFFFF)
val LightTextPrimary = Color(0xFF0F172A)
val LightTextSecondary = Color(0xFF64748B)

// Material 3 Mapping Colors - Dark (Premium Upgrade)
val md_theme_dark_primary = Color(0xFFD0BCFF)
val md_theme_dark_onPrimary = Color(0xFF381E72)
val md_theme_dark_primaryContainer = Color(0xFF4F378B)
val md_theme_dark_onPrimaryContainer = Color(0xFFEADDFF)
val md_theme_dark_secondary = Color(0xFFCCC2DC)
val md_theme_dark_onSecondary = Color(0xFF332D41)
val md_theme_dark_error = Color(0xFFF85149)
val md_theme_dark_background = DeepDarkBg
val md_theme_dark_onBackground = DarkTextPrimary
val md_theme_dark_surface = DeepDarkSurface
val md_theme_dark_onSurface = DarkTextPrimary
val md_theme_dark_surfaceVariant = Color(0xFF21262D)
val md_theme_dark_onSurfaceVariant = DarkTextSecondary
val md_theme_dark_outline = DarkBorder

// Material 3 Mapping Colors - Light
val md_theme_light_primary = Color(0xFF6750A4)
val md_theme_light_onPrimary = Color(0xFFFFFFFF)
val md_theme_light_primaryContainer = Color(0xFFEADDFF)
val md_theme_light_onPrimaryContainer = Color(0xFF21005D)
val md_theme_light_secondary = Color(0xFF625B71)
val md_theme_light_onSecondary = Color(0xFFFFFFFF)
val md_theme_light_error = Color(0xFFB3261E)
val md_theme_light_background = LightBg
val md_theme_light_onBackground = LightTextPrimary
val md_theme_light_surface = LightSurface
val md_theme_light_onSurface = LightTextPrimary
val md_theme_light_surfaceVariant = Color(0xFFE7E0EC)
val md_theme_light_onSurfaceVariant = LightTextSecondary
val md_theme_light_outline = Color(0xFF79747E)
