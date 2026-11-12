package app.ioutracker.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// GENERATED — do not hand-edit individual roles.
//
// Both schemes are derived from four brand seeds by Material's tonal palette
// construction (CAM16/HCT): the seeds are the judgement, the ~36 roles are
// arithmetic. Hand-picking a role breaks the fixed tonal relationship it holds
// with the rest of its family, and the failure shows up as a contrast bug on one
// screen out of thirty rather than as a visible mistake here.
//
//   primary   #4B55A6  — brand: filled buttons, FAB, selected tab
//   secondary #666A82  — quiet containers: chips, tonal buttons, selected segment
//   tertiary  #B07F2A  — accents and caution markers, used sparingly
//   neutral   #E1E0E8  — seeds every surface, so it decides warm or cool overall
//
// Error stays M3 baseline (hue 25, chroma 84): a safety signal, not a brand choice.
//
// To change the palette, edit the seeds in scripts/gen_scheme.py and re-run it.
// Colours come from MaterialTheme, never literals, and dynamic colour defaults
// off (ADR-0006) so this file is what a user actually sees.

private val AppLightColors =
    lightColorScheme(
        primary = Color(0xFF4D57A8),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFDEE0FF),
        onPrimaryContainer = Color(0xFF000A64),
        inversePrimary = Color(0xFFBBC2FF),
        secondary = Color(0xFF595D74),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFDEE1FD),
        onSecondaryContainer = Color(0xFF161A2E),
        tertiary = Color(0xFF805600),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFDDAA),
        onTertiaryContainer = Color(0xFF281800),
        error = Color(0xFFBA1B1B),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD4),
        onErrorContainer = Color(0xFF410001),
        background = Color(0xFFFAF8FF),
        onBackground = Color(0xFF1A1B21),
        surface = Color(0xFFFAF8FF),
        onSurface = Color(0xFF1A1B21),
        surfaceVariant = Color(0xFFE1E1EC),
        onSurfaceVariant = Color(0xFF44464E),
        surfaceDim = Color(0xFFDAD9E1),
        surfaceBright = Color(0xFFFAF8FF),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF4F3FB),
        surfaceContainer = Color(0xFFEEEDF5),
        surfaceContainerHigh = Color(0xFFE8E7EF),
        surfaceContainerHighest = Color(0xFFE2E1E9),
        inverseSurface = Color(0xFF2F3036),
        inverseOnSurface = Color(0xFFF1F0F8),
        outline = Color(0xFF75767F),
        outlineVariant = Color(0xFFC6C6D0),
        scrim = Color(0xFF000000),
    )

private val AppDarkColors =
    darkColorScheme(
        primary = Color(0xFFBBC2FF),
        onPrimary = Color(0xFF1C2677),
        primaryContainer = Color(0xFF353F8F),
        onPrimaryContainer = Color(0xFFDEE0FF),
        inversePrimary = Color(0xFF4D57A8),
        secondary = Color(0xFFC2C5E0),
        onSecondary = Color(0xFF2B2F44),
        secondaryContainer = Color(0xFF42465C),
        onSecondaryContainer = Color(0xFFDEE1FD),
        tertiary = Color(0xFFF6BD62),
        onTertiary = Color(0xFF442B00),
        tertiaryContainer = Color(0xFF614000),
        onTertiaryContainer = Color(0xFFFFDDAA),
        error = Color(0xFFFFB4A9),
        onError = Color(0xFF680003),
        errorContainer = Color(0xFF930006),
        onErrorContainer = Color(0xFFFFDAD4),
        background = Color(0xFF121318),
        onBackground = Color(0xFFE2E1E9),
        surface = Color(0xFF121318),
        onSurface = Color(0xFFE2E1E9),
        surfaceVariant = Color(0xFF44464E),
        onSurfaceVariant = Color(0xFFC6C6D0),
        surfaceDim = Color(0xFF121318),
        surfaceBright = Color(0xFF38393F),
        surfaceContainerLowest = Color(0xFF0D0E13),
        surfaceContainerLow = Color(0xFF1A1B21),
        surfaceContainer = Color(0xFF1E1F25),
        surfaceContainerHigh = Color(0xFF292A30),
        surfaceContainerHighest = Color(0xFF33343A),
        inverseSurface = Color(0xFFE2E1E9),
        inverseOnSurface = Color(0xFF2F3036),
        outline = Color(0xFF8F909A),
        outlineVariant = Color(0xFF44464E),
        scrim = Color(0xFF000000),
    )

internal val LightColors = AppLightColors
internal val DarkColors = AppDarkColors
