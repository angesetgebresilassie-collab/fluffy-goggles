package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.ui.theme.DarkGlassBorder
import com.example.ui.theme.DarkGlassBorderAlpha
import com.example.ui.theme.DarkGlassTint
import com.example.ui.theme.DarkGlassTintAlpha
import com.example.ui.theme.LightGlassBorder
import com.example.ui.theme.LightGlassBorderAlpha
import com.example.ui.theme.LightGlassTint
import com.example.ui.theme.LightGlassTintAlpha
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import dev.chrisbanes.haze.materials.HazeMaterials

/**
 * Shared blur state for a screen: mark whatever sits behind the glass (album art,
 * gradients, scrolling content) with [glassBackdropSource], and any [frostedGlass]
 * panel drawn on top of it will show that content blurred through, like real frosted
 * glass. One HazeState per screen/dialog is enough - every glass panel on that screen
 * can share it.
 */
@Composable
fun rememberGlassState(): HazeState = rememberHazeState()

/** Marks a composable (album art, backdrop gradient, etc.) as the content to be blurred. */
fun Modifier.glassBackdropSource(state: HazeState): Modifier = this.hazeSource(state = state)

enum class GlassThickness { THIN, REGULAR, THICK }

/**
 * Applies the frosted-glass blur + tint + hairline border to a composable. Use this on a
 * Box/Surface that sits visually on top of whatever was marked with [glassBackdropSource].
 * Haze falls back to a flat tint (no blur) on devices/API levels it can't hardware-blur on,
 * so this still looks intentional even on older phones (minSdk 24).
 */
@Composable
fun Modifier.frostedGlass(
    state: HazeState,
    shape: Shape = RoundedCornerShape(20.dp),
    thickness: GlassThickness = GlassThickness.REGULAR
): Modifier {
    val isDark = isSystemInDarkTheme()
    val style = when (thickness) {
        GlassThickness.THIN -> HazeMaterials.thin()
        GlassThickness.REGULAR -> HazeMaterials.regular()
        GlassThickness.THICK -> HazeMaterials.thick()
    }
    val borderColor = if (isDark) {
        DarkGlassBorder.copy(alpha = DarkGlassBorderAlpha)
    } else {
        LightGlassBorder.copy(alpha = LightGlassBorderAlpha)
    }
    val tintColor = if (isDark) {
        DarkGlassTint.copy(alpha = DarkGlassTintAlpha)
    } else {
        LightGlassTint.copy(alpha = LightGlassTintAlpha)
    }

    return this
        .clip(shape)
        .hazeEffect(state = state, style = style)
        .background(tintColor, shape = shape)
        .border(width = 1.dp, color = borderColor, shape = shape)
}

/**
 * Same tint/border look as [frostedGlass] but without a Haze blur source behind it -
 * for panels (like a floating bottom nav) that don't have meaningful content rendered
 * underneath them to blur. Still reads as "glass" via translucency + a soft rim border,
 * just without the backdrop distortion.
 */
@Composable
fun Modifier.staticGlass(
    shape: Shape = RoundedCornerShape(20.dp)
): Modifier {
    val isDark = isSystemInDarkTheme()
    val borderColor = if (isDark) {
        DarkGlassBorder.copy(alpha = DarkGlassBorderAlpha)
    } else {
        LightGlassBorder.copy(alpha = LightGlassBorderAlpha)
    }
    val tintColor = if (isDark) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
    }

    return this
        .clip(shape)
        .background(tintColor, shape = shape)
        .border(width = 1.dp, color = borderColor, shape = shape)
}

/**
 * Full-bleed backdrop: the given artwork, heavily blurred and darkened, filling the
 * available space. Marks itself as the blur source for [state] so glass panels drawn on
 * top of it (mini player, control cluster, top bar) show it blurred through.
 */
@Composable
fun BlurredArtworkBackdrop(
    artworkUrl: String?,
    state: HazeState,
    modifier: Modifier = Modifier,
    scrimAlpha: Float = 0.55f
) {
    Box(modifier = modifier.fillMaxSize().glassBackdropSource(state)) {
        if (!artworkUrl.isNullOrBlank()) {
            AsyncImage(
                model = artworkUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            )
        }
        // Darken so text/controls placed on top of the glass stay legible regardless
        // of how bright the underlying artwork is.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = (scrimAlpha * 0.6f).coerceIn(0f, 1f)),
                        0.5f to Color.Black.copy(alpha = scrimAlpha.coerceIn(0f, 1f)),
                        1f to Color.Black.copy(alpha = (scrimAlpha * 1.3f).coerceIn(0f, 1f))
                    )
                )
        )
    }
}
