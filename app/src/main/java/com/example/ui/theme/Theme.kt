package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.unit.dp

/**
 * Pure-Compose [TextToolbar] implementation that avoids invoking Android's
 * `DecorView.startActionMode(..., ActionMode.TYPE_FLOATING)`, preventing
 * `Destroying unexpected ActionMode instance of TYPE_FLOATING` errors when
 * TextFields gain/lose focus across rapid screen or dialog transitions.
 */
class SafeComposeTextToolbar : TextToolbar {
    private var _status by mutableStateOf(TextToolbarStatus.Hidden)
    override val status: TextToolbarStatus
        get() = _status

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?
    ) {
        _status = TextToolbarStatus.Shown
    }

    override fun hide() {
        _status = TextToolbarStatus.Hidden
    }
}

@Composable
fun ProvideSafeTextToolbar(content: @Composable () -> Unit) {
    val safeToolbar = remember { SafeComposeTextToolbar() }
    CompositionLocalProvider(
        LocalTextToolbar provides safeToolbar,
        content = content
    )
}

private val NotedDarkColorScheme = darkColorScheme(
    primary = AmberPrimaryDark,
    onPrimary = AmberOnPrimaryDark,
    primaryContainer = AmberContainerDark,
    onPrimaryContainer = AmberOnContainerDark,
    secondary = TealSecondaryDark,
    onSecondary = Color(0xFF00201C),
    secondaryContainer = TealContainerDark,
    onSecondaryContainer = Color(0xFF99F6E4),
    tertiary = RoseTertiaryDark,
    background = ObsidianBg,
    onBackground = TextPrimaryDark,
    surface = ObsidianSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    outline = ObsidianOutline
)

private val NotedLightColorScheme = lightColorScheme(
    primary = AmberPrimaryLight,
    onPrimary = AmberOnPrimaryLight,
    primaryContainer = AmberContainerLight,
    onPrimaryContainer = AmberOnContainerLight,
    secondary = TealSecondaryLight,
    onSecondary = Color.White,
    secondaryContainer = TealContainerLight,
    onSecondaryContainer = Color(0xFF134E4A),
    tertiary = RoseTertiaryLight,
    background = AlabasterBg,
    onBackground = TextPrimaryLight,
    surface = AlabasterSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = AlabasterSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = AlabasterOutline
)

val NotedShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) NotedDarkColorScheme else NotedLightColorScheme

    ProvideSafeTextToolbar {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = NotedShapes,
            content = content
        )
    }
}
