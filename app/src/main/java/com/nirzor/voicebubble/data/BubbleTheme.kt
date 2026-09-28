package com.nirzor.voicebubble.data

import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.Color as ComposeColor

data class BubbleTheme(
    val id: String,
    val name: String,
    val nativeName: String,
    val primaryColorInt: Int,
    val secondaryColorInt: Int,
    val accentColorInt: Int,
    val composePrimaryColor: ComposeColor,
    val composeAccentColor: ComposeColor
)

object BubbleThemes {
    val themes = listOf(
        BubbleTheme(
            id = "indigo_ocean",
            name = "Indigo Ocean",
            nativeName = "ইন্ডিগো ওশান (ডিফল্ট)",
            primaryColorInt = AndroidColor.parseColor("#4F46E5"),
            secondaryColorInt = AndroidColor.parseColor("#3730A3"),
            accentColorInt = AndroidColor.parseColor("#818CF8"),
            composePrimaryColor = ComposeColor(0xFF4F46E5),
            composeAccentColor = ComposeColor(0xFF818CF8)
        ),
        BubbleTheme(
            id = "emerald_mint",
            name = "Emerald Mint",
            nativeName = "এমারেল্ড মিন্ট",
            primaryColorInt = AndroidColor.parseColor("#059669"),
            secondaryColorInt = AndroidColor.parseColor("#065F46"),
            accentColorInt = AndroidColor.parseColor("#34D399"),
            composePrimaryColor = ComposeColor(0xFF059669),
            composeAccentColor = ComposeColor(0xFF34D399)
        ),
        BubbleTheme(
            id = "crimson_ruby",
            name = "Crimson Ruby",
            nativeName = "ক্রিমসন রুবি",
            primaryColorInt = AndroidColor.parseColor("#DC2626"),
            secondaryColorInt = AndroidColor.parseColor("#991B1B"),
            accentColorInt = AndroidColor.parseColor("#F87171"),
            composePrimaryColor = ComposeColor(0xFFDC2626),
            composeAccentColor = ComposeColor(0xFFF87171)
        ),
        BubbleTheme(
            id = "amethyst_purple",
            name = "Amethyst Purple",
            nativeName = "অ্যামেথিস্ট পার্পল",
            primaryColorInt = AndroidColor.parseColor("#7C3AED"),
            secondaryColorInt = AndroidColor.parseColor("#5B21B6"),
            accentColorInt = AndroidColor.parseColor("#A78BFA"),
            composePrimaryColor = ComposeColor(0xFF7C3AED),
            composeAccentColor = ComposeColor(0xFFA78BFA)
        ),
        BubbleTheme(
            id = "sunset_amber",
            name = "Sunset Amber",
            nativeName = "সানসেট অ্যাম্বার",
            primaryColorInt = AndroidColor.parseColor("#D97706"),
            secondaryColorInt = AndroidColor.parseColor("#92400E"),
            accentColorInt = AndroidColor.parseColor("#FBBF24"),
            composePrimaryColor = ComposeColor(0xFFD97706),
            composeAccentColor = ComposeColor(0xFFFBBF24)
        ),
        BubbleTheme(
            id = "midnight_dark",
            name = "Midnight Dark",
            nativeName = "মিডনাইট ডার্ক",
            primaryColorInt = AndroidColor.parseColor("#1F2937"),
            secondaryColorInt = AndroidColor.parseColor("#111827"),
            accentColorInt = AndroidColor.parseColor("#60A5FA"),
            composePrimaryColor = ComposeColor(0xFF1F2937),
            composeAccentColor = ComposeColor(0xFF60A5FA)
        )
    )

    fun getThemeById(id: String): BubbleTheme {
        return themes.find { it.id == id } ?: themes[0]
    }
}
