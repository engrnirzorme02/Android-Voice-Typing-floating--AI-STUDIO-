package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.VoiceBubbleApp
import com.example.data.SupportedLanguages
import com.example.ui.components.LanguageSelectorDialog
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    onRequestOverlayPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = VoiceBubbleApp.instance
    val prefs = app.preferences

    val selectedLangCode by prefs.selectedLanguage.collectAsState()
    val bubbleSize by prefs.bubbleSizeDp.collectAsState()
    val bubbleOpacity by prefs.bubbleOpacity.collectAsState()
    val autoCopy by prefs.autoCopy.collectAsState()
    val haptic by prefs.hapticFeedback.collectAsState()
    val sound by prefs.soundFeedback.collectAsState()
    val preferOffline by prefs.preferOffline.collectAsState()
    val dockToEdge by prefs.dockToEdge.collectAsState()

    val currentLang = remember(selectedLangCode) { SupportedLanguages.getLanguageByCode(selectedLangCode) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 1: Floating Bubble Appearance & Physics
        SectionCard(title = "ফ্লোটিং বাবল কাস্টমাইজেশন (Bubble Display)") {
            // Bubble Size Slider
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Animation, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(text = "বাবলের আকার (Size)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                    Text(text = "${bubbleSize}dp", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = bubbleSize.toFloat(),
                    onValueChange = { prefs.setBubbleSize(it.roundToInt()) },
                    valueRange = 48f..80f,
                    steps = 7,
                    modifier = Modifier.testTag("bubble_size_slider")
                )
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // Bubble Opacity Slider
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Opacity, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(text = "স্বচ্ছতা (Opacity)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                    Text(text = "${(bubbleOpacity * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = bubbleOpacity,
                    onValueChange = { prefs.setBubbleOpacity(it) },
                    valueRange = 0.5f..1.0f,
                    steps = 9,
                    modifier = Modifier.testTag("bubble_opacity_slider")
                )
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // Snap to Edge Switch
            SettingsSwitchRow(
                title = "স্ক্রিনের পাশে স্বয়ংক্রিয়ভাবে লক করা (Snap to Edge)",
                description = "বাবল ছেড়ে দিলে স্বয়ংক্রিয়ভাবে ডান বা বাম পাশে লেগে থাকবে",
                icon = Icons.Default.Layers,
                checked = dockToEdge,
                onCheckedChange = { prefs.setDockToEdge(it) }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // Reset Position
            SettingsActionRow(
                title = "ডিফল্ট পজিশন রিসেট করুন",
                subtitle = "বাবলটি আগের জায়গায় ফিরে যাবে",
                icon = Icons.Default.Refresh,
                onClick = { 
                    prefs.setBubblePosition(-1, -1)
                }
            )
        }

        // Section 2: Voice Recognition & Clipboard
        SectionCard(title = "ভয়েস টাইপিং ও ক্লিপবোর্ড (Speech & Copy)") {
            // Language Selection
            SettingsActionRow(
                title = "ভয়েস ইনপুট ভাষা (Recognition Language)",
                subtitle = "${currentLang.flag} ${currentLang.displayName}",
                icon = Icons.Default.Language,
                onClick = { showLanguageDialog = true }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // Auto Copy Switch
            SettingsSwitchRow(
                title = "অটোমেটিক ক্লিপবোর্ডে কপি (Auto Copy)",
                description = "কথা বলা শেষ হলে নিজে থেকেই লেখা ক্লিপবোর্ডে কপি হবে",
                icon = Icons.Default.ContentCopy,
                checked = autoCopy,
                onCheckedChange = { prefs.setAutoCopy(it) }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // Prefer Offline Mode
            SettingsSwitchRow(
                title = "অফলাইন রিকগনিশন প্রাধান্য দিন (Prefer Offline)",
                description = "ওয়াইফাই বা ইন্টারনেট কানেকশন দুর্বল থাকলে লোকাল স্পিচ মডেল ব্যবহার করবে",
                icon = Icons.Default.WifiOff,
                checked = preferOffline,
                onCheckedChange = { prefs.setPreferOffline(it) }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // Haptic Feedback
            SettingsSwitchRow(
                title = "হ্যাপটিক ভাইব্রেশন (Vibration Feedback)",
                description = "রেকর্ডিং শুরু, শেষ এবং কপি হওয়ার সময় মৃদু ভাইব্রেশন হবে",
                icon = Icons.Default.Vibration,
                checked = haptic,
                onCheckedChange = { prefs.setHapticFeedback(it) }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // Sound Beeps
            SettingsSwitchRow(
                title = "অডিও সংকেত (Sound Beep)",
                description = "ভয়েস টাইপিং শুরুর সময় বিপ শব্দ শোনা যাবে",
                icon = Icons.Default.VolumeUp,
                checked = sound,
                onCheckedChange = { prefs.setSoundFeedback(it) }
            )
        }

        // Section 3: Android WiFi Voice Typing Bug Diagnostic
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("wifi_bug_guide_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Wifi,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Text(
                        text = "ওয়াইফাইতে ভয়েস টাইপিং না চলার কারণ ও সমাধান",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "কেন এমন হয়?\nকিছু অ্যান্ড্রয়েড ফোনে (বিশেষ করে Google Speech Services সার্ভার পিং করার সময়) ওয়াইফাই রাউটারের DNS বা IPv6 কনফিগারেশনের কারণে অনলাইন স্পিচ রিকগনিশন সেশন ব্লক হয়ে যায়, কিন্তু মোবাইল ডাটাতে পোর্ট ওপেন থাকায় সহজে কাজ করে।",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )

                Text(
                    text = "স্থায়ী সমাধান:\n১. সেটিংসে 'অফলাইন রিকগনিশন প্রাধান্য দিন' অপশনটি চালু রাখুন।\n২. ফোনের Settings > System > Languages & Input > Google Voice Typing এ গিয়ে বাংলা (বাংলাদেশ) অফলাইন স্পিচ ডাটা ডাউনলোড করে রাখুন।\n৩. এই ভয়েস বাবল অ্যাপটি সরাসরি নেটিভ স্পিচ সার্ভিস ট্রিগার করে ক্লিপবোর্ডে কপি করে দেয়, ফলে সুইফটকির অ্যারো-কি দিয়ে কার্সর কন্ট্রোল করে দ্রুত টাইপ করতে পারবেন।",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Section 4: System Overlays Permission Link
        SectionCard(title = "সিস্টেম সেটিংস ও পারমিশন") {
            SettingsActionRow(
                title = "ডিসপ্লে ওভারলে পারমিশন সেটিংস",
                subtitle = "অন্যান্য অ্যাপের উপর ভাসমান বাবলের অনুমতি পরিচালনা করুন",
                icon = Icons.Default.Settings,
                onClick = onRequestOverlayPermission
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showLanguageDialog) {
        LanguageSelectorDialog(
            currentLanguageCode = selectedLangCode,
            onLanguageSelected = { lang ->
                prefs.setLanguage(lang.code)
            },
            onDismiss = { showLanguageDialog = false }
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
            HorizontalDivider()
            content()
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    description: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.5.sp
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}

@Composable
private fun SettingsActionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.5.sp
                )
            }
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "Navigate",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
