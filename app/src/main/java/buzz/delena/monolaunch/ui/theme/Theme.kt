package buzz.delena.monolaunch.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * Single, deliberately fixed AMOLED-black scheme. Monolaunch does not
 * follow system light/dark — pure black is the point, on every device.
 */
private val MonolaunchColorScheme = darkColorScheme(
    primary = PureWhite,
    onPrimary = AmoledBlack,
    secondary = OutlineDim,
    onSecondary = PureWhite,
    background = AmoledBlack,
    onBackground = PureWhite,
    surface = AmoledBlack,
    onSurface = PureWhite,
    surfaceVariant = SurfacePill,
    onSurfaceVariant = TextSecondary,
    outline = OutlineDim,
)

@Composable
fun MonolaunchTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MonolaunchColorScheme,
        typography = MonolaunchTypography,
        content = content,
    )
}
