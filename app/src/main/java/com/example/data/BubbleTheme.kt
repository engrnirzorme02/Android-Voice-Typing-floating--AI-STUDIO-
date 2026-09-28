package com.example.data

import android.graphics.Color
import androidx.compose.ui.graphics.Color as ComposeColor

data class BubbleTheme(
    val id: String,
    val nameEn: String,
    val nameBn: String,
    val startColorHex: String,
    val endColorHex: String,
    val accentColorHex: String,
    val ringColorHex: String
) {
    val startColorInt: Int get() = Color.parseColor(startColorHex)
    val endColorInt: Int get() = Color.parseColor(endColorHex)
    val accentColorInt: Int get() = Color.parseColor(accentColorHex)
    val ringColorInt: Int get() = Color.parseColor(ringColorHex)

    val composeStartColor: ComposeColor get() = ComposeColor(startColorInt)
    val composeEndColor: ComposeColor get() = ComposeColor(endColorInt)
    val composeAccentColor: ComposeColor get() = ComposeColor(accentColorInt)
}

object BubbleThemes {
    val THEMES = listOf(
        BubbleTheme(
            id = "indigo_ocean",
            nameEn = "Indigo Ocean",
            nameBn = "সমুদ্র নীল (ডিফল্ট)",
            startColorHex = "#4F46E5",
            endColorHex = "#06B6D4",
            accentColorHex = "#38BDF8",
            ringColorHex = "#8038BDF8"
        ),
        BubbleTheme(
            id = "emerald_mint",
            nameEn = "Emerald Mint",
            nameBn = "পান্না সবুজ",
            startColorHex = "#059669",
            endColorHex = "#10B981",
            accentColorHex = "#34D399",
            ringColorHex = "#8034D399"
        ),
        BubbleTheme(
            id = "sunset_coral",
            nameEn = "Sunset Coral",
            nameBn = "সূর্যাস্ত প্রবাল",
            startColorHex = "#E11D48",
            endColorHex = "#F97316",
            accentColorHex = "#FB923C",
            ringColorHex = "#80FB923C"
        ),
        BubbleTheme(
            id = "royal_violet",
            nameEn = "Royal Violet",
            nameBn = "রাজকীয় বেগুনি",
            startColorHex = "#7C3AED",
            endColorHex = "#EC4899",
            accentColorHex = "#C084FC",
            ringColorHex = "#80C084FC"
        ),
        BubbleTheme(
            id = "cyber_neon",
            nameEn = "Cyber Neon",
            nameBn = "সাইবার স্কাই",
            startColorHex = "#0284C7",
            endColorHex = "#06B6D4",
            accentColorHex = "#67E8F9",
            ringColorHex = "#8067E8F9"
        ),
        BubbleTheme(
            id = "dark_onyx",
            nameEn = "Dark Onyx",
            nameBn = "অনিক্স গোল্ড",
            startColorHex = "#0F172A",
            endColorHex = "#334155",
            accentColorHex = "#F59E0B",
            ringColorHex = "#80F59E0B"
        )
    )

    fun getThemeById(id: String): BubbleTheme {
        return THEMES.find { it.id == id } ?: THEMES[0]
    }
}
