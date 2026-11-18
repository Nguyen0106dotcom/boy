package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val MidnightGuardianColorScheme = darkColorScheme(
    primary = AmberGlow,
    onPrimary = MidnightVoid,
    primaryContainer = AmberContainer,
    onPrimaryContainer = OnAmberContainer,
    secondary = ElectricViolet,
    onSecondary = LavenderWhite,
    secondaryContainer = ElectricVioletContainer,
    onSecondaryContainer = LavenderWhite,
    tertiary = NeonMint,
    onTertiary = MidnightVoid,
    tertiaryContainer = NeonMintContainer,
    onTertiaryContainer = OnNeonMintContainer,
    error = CrimsonAlert,
    onError = LavenderWhite,
    errorContainer = CrimsonContainer,
    onErrorContainer = OnCrimsonContainer,
    background = MidnightVoid,
    onBackground = LavenderWhite,
    surface = MidnightSurface,
    onSurface = LavenderWhite,
    surfaceVariant = MidnightCard,
    onSurfaceVariant = LavenderMuted,
    surfaceContainerHigh = MidnightCardElevated,
    outline = MidnightOutline
)

private val DaylightGuardianColorScheme = lightColorScheme(
    primary = LightAmberPrimary,
    onPrimary = Color.White,
    primaryContainer = LightAmberContainer,
    onPrimaryContainer = LightOnAmberContainer,
    secondary = LightVioletSecondary,
    onSecondary = Color.White,
    secondaryContainer = LightVioletContainer,
    onSecondaryContainer = LightOnVioletContainer,
    tertiary = LightMintTertiary,
    onTertiary = Color.White,
    tertiaryContainer = LightMintContainer,
    onTertiaryContainer = LightOnMintContainer,
    error = LightCrimsonError,
    onError = Color.White,
    errorContainer = LightCrimsonContainer,
    onErrorContainer = LightOnCrimsonContainer,
    background = DaylightVoid,
    onBackground = DaylightInk,
    surface = DaylightSurface,
    onSurface = DaylightInk,
    surfaceVariant = DaylightCard,
    onSurfaceVariant = DaylightMuted,
    surfaceContainerHigh = DaylightCardElevated,
    outline = DaylightOutline
)

val KidGuardShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun KidGuardTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) MidnightGuardianColorScheme else DaylightGuardianColorScheme,
        typography = KidGuardTypography,
        shapes = KidGuardShapes,
        content = content
    )
}
