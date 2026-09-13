package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Warm, terracotta-and-parchment palette (the "Claude look") - deliberately avoids the
// cold blue/indigo most AI apps default to. Light mode reads like warm paper; dark mode
// reads like a warm charcoal, never a cold blue-black.

// Dark Palette
val DarkBackground = Color(0xFF262624)
val DarkSurface = Color(0xFF30302E)
val DarkSurfaceVariant = Color(0xFF3A3935)
val DarkSurfaceCard = Color(0xFF2C2B28)
val DarkPrimary = Color(0xFFD97757)      // warm terracotta, lightened for contrast on dark bg
val DarkPrimaryVariant = Color(0xFFC15F3C)
val DarkSecondary = Color(0xFFB1ADA1)    // warm stone gray
val DarkAccentPink = Color(0xFFE2725B)   // warm coral-red, reserved for favorites/hearts
val DarkOnBackground = Color(0xFFF4F0EA)
val DarkOnSurface = Color(0xFFF4F0EA)
val DarkOnSurfaceMuted = Color(0xFFB8B2A7)
val DarkOutline = Color(0xFF4A4945)

// Light Palette
val LightBackground = Color(0xFFF4F3EE)  // warm parchment ("Pampas")
val LightSurface = Color(0xFFFAF9F5)
val LightSurfaceVariant = Color(0xFFECE8DF)
val LightSurfaceCard = Color(0xFFFDFCFA)
val LightPrimary = Color(0xFFC15F3C)     // "Crail" terracotta - the signature accent
val LightPrimaryVariant = Color(0xFFA84E30)
val LightSecondary = Color(0xFF8C8577)   // warm stone gray, darkened for AA contrast on parchment
val LightAccentPink = Color(0xFFB23A2A)  // deeper warm red, reserved for favorites/hearts
val LightOnBackground = Color(0xFF231F1B)
val LightOnSurface = Color(0xFF231F1B)
val LightOnSurfaceMuted = Color(0xFF6B6459)
val LightOutline = Color(0xFFDDD8CC)

// YouTube Red (kept as-is - this one's a third-party brand marker, not our palette)
val YouTubeRed = Color(0xFFFF0000)

// Glass / frosted-glass tokens
// Dark mode glass sits on the warm charcoal AppBackdrop, so panels stay light &
// translucent with a warm off-white (not pure white) rim to catch light like real
// frosted glass/acrylic, without turning cold.
val DarkGlassTint = Color(0xFFF4F0EA)
val DarkGlassTintAlpha = 0.07f
val DarkGlassBorder = Color(0xFFF4F0EA)
val DarkGlassBorderAlpha = 0.16f

// Light mode glass sits on the warm parchment AppBackdrop, so panels use a warm-ink
// tint so the blur still reads as "glass" instead of disappearing into cream-on-cream.
val LightGlassTint = Color(0xFF231F1B)
val LightGlassTintAlpha = 0.045f
val LightGlassBorder = Color(0xFF231F1B)
val LightGlassBorderAlpha = 0.10f

// Gradient wash used behind blurred artwork backdrops in the full-screen player (keeps
// text/controls legible on top regardless of how bright the artwork is). Warmed
// slightly off pure black to stay consistent with the rest of the palette.
val GlassScrimTop = Color(0xFF1A1613)
val GlassScrimBottom = Color(0xFF120F0D)

// App-wide decorative backdrop: soft, slow-drifting terracotta/stone color blobs behind
// a warm gradient base. This is what every glass panel across the app (top bar, nav,
// search bar, song cards, settings rows) blurs through - without this, "frosted glass"
// over a flat color is indistinguishable from a flat color, so this backdrop is what
// makes the blur effect actually visible app-wide, not just on the now-playing screen.
val DarkBackdropBlobPrimary = Color(0xFFD97757)
val DarkBackdropBlobSecondary = Color(0xFF8C8577)
val LightBackdropBlobPrimary = Color(0xFFC15F3C)
val LightBackdropBlobSecondary = Color(0xFFB1ADA1)
