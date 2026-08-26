package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.VoiceBubbleApp
import com.example.data.SupportedLanguages
import com.example.data.VoiceHistoryEntity
import com.example.service.FloatingBubbleService
import com.example.speech.SpeechEngine
import com.example.speech.SpeechState
import com.example.ui.components.LanguageSelectorDialog
import com.example.ui.components.LiveWaveformVisualizer
import com.example.ui.components.PermissionCard
import com.example.ui.components.SwiftKeyTipCard
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    isServiceRunning: Boolean,
    hasOverlayPermission: Boolean,
    hasMicPermission: Boolean,
    hasNotificationPermission: Boolean,
    onToggleService: (Boolean) -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onRequestMicPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = VoiceBubbleApp.instance
    val prefs = app.preferences
    val repo = app.repository
    val scope = rememberCoroutineScope()

    val selectedLangCode by prefs.selectedLanguage.collectAsState()
    val currentLang = remember(selectedLangCode) { SupportedLanguages.getLanguageByCode(selectedLangCode) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    // In-App Test Pad State
    val testSpeechEngine = remember { SpeechEngine(context) }
    val testSpeechState by testSpeechEngine.speechState.collectAsState()
    val testRms by testSpeechEngine.currentRms.collectAsState()
    var inAppTranscription by remember { mutableStateOf("") }
    var justCopiedInApp by remember { mutableStateOf(false) }

    val isTestListening = testSpeechState is SpeechState.Listening

    DisposableEffect(Unit) {
        onDispose {
            testSpeechEngine.stopListening()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Card: Service Master Control
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("service_master_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = if (isServiceRunning) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                }
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Animated Bubble Preview Circle
                        val bubbleColor by animateColorAsState(
                            targetValue = if (isServiceRunning) Color(0xFF06B6D4) else MaterialTheme.colorScheme.outline,
                            label = "bubble_color"
                        )
                        Surface(
                            shape = CircleShape,
                            color = bubbleColor.copy(alpha = 0.2f),
                            border = BorderStroke(2.dp, bubbleColor),
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isServiceRunning) Icons.Default.Mic else Icons.Default.MicOff,
                                    contentDescription = "Bubble Status",
                                    tint = if (isServiceRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = "Floating Voice Bubble",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FiberManualRecord,
                                    contentDescription = null,
                                    tint = if (isServiceRunning) Color(0xFF10B981) else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(10.dp)
                                )
                                Text(
                                    text = if (isServiceRunning) "স্ক্রিনে ভাসমান বাবল সক্রিয়" else "বাবল বন্ধ আছে (Inactive)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isServiceRunning) Color(0xFF047857) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Switch(
                        checked = isServiceRunning,
                        onCheckedChange = { enabled ->
                            if (enabled && (!hasOverlayPermission || !hasMicPermission)) {
                                Toast.makeText(context, "প্রথমে প্রয়োজনীয় পারমিশনগুলো চালু করুন", Toast.LENGTH_LONG).show()
                            } else {
                                onToggleService(enabled)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("service_master_switch")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Language quick badge
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { showLanguageDialog = true }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = currentLang.flag, fontSize = 20.sp)
                        Text(
                            text = "ভয়েস ভাষা: ${currentLang.displayName}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = "পরিবর্তন করুন ➔",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Permission Card
        PermissionCard(
            hasOverlayPermission = hasOverlayPermission,
            hasMicPermission = hasMicPermission,
            hasNotificationPermission = hasNotificationPermission,
            onRequestOverlay = onRequestOverlayPermission,
            onRequestMic = onRequestMicPermission,
            onRequestNotification = onRequestNotificationPermission
        )

        // In-App Voice Testing Pad
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("in_app_voice_pad"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ভয়েস টাইপিং টেস্ট প্যাড",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "এখানে কথা বলে সরাসরি টেস্ট করুন",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (inAppTranscription.isNotBlank()) {
                        IconButton(
                            onClick = {
                                inAppTranscription = ""
                                justCopiedInApp = false
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Waveform Visualizer
                LiveWaveformVisualizer(
                    isListening = isTestListening,
                    rmsLevel = testRms,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Transcription Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .padding(12.dp)
                ) {
                    if (inAppTranscription.isBlank()) {
                        Text(
                            text = if (isTestListening) "শুনছি... আপনার কথা বলুন..." else "কথা বলতে নিচের মাইক বাটনে প্রেস করুন। কথা শেষ হলে লেখাটি সাথে সাথে কপি হয়ে যাবে।",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isTestListening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            lineHeight = 20.sp
                        )
                    } else {
                        Text(
                            text = inAppTranscription,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 22.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            if (isTestListening) {
                                testSpeechEngine.stopListening()
                            } else {
                                inAppTranscription = ""
                                justCopiedInApp = false
                                testSpeechEngine.startListening(
                                    languageCode = selectedLangCode,
                                    preferOffline = prefs.preferOffline.value,
                                    playTone = prefs.soundFeedback.value,
                                    enableHaptic = prefs.hapticFeedback.value,
                                    onPartialResult = { partial ->
                                        inAppTranscription = partial
                                    },
                                    onFinalResult = { finalResult, lang, durationMs ->
                                        inAppTranscription = finalResult
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Voice Transcription", finalResult)
                                        clipboard.setPrimaryClip(clip)
                                        justCopiedInApp = true

                                        scope.launch {
                                            repo.insert(
                                                VoiceHistoryEntity(
                                                    text = finalResult,
                                                    language = lang,
                                                    durationMs = durationMs
                                                )
                                            )
                                        }
                                        Toast.makeText(context, "ক্লিপবোর্ডে কপি হয়েছে! ✓", Toast.LENGTH_SHORT).show()
                                    },
                                    onErrorOccurred = { errorMsg, _ ->
                                        inAppTranscription = "ত্রুটি: $errorMsg"
                                    }
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isTestListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("in_app_mic_button")
                    ) {
                        Icon(
                            imageVector = if (isTestListening) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = if (isTestListening) "Stop" else "Start",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isTestListening) "থামুন (Stop)" else "কথা বলুন (Tap to Speak)",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (inAppTranscription.isNotBlank()) {
                        FilledTonalButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Voice Transcription", inAppTranscription)
                                clipboard.setPrimaryClip(clip)
                                justCopiedInApp = true
                                Toast.makeText(context, "কপি করা হয়েছে!", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.testTag("in_app_copy_button")
                        ) {
                            Icon(
                                imageVector = if (justCopiedInApp) Icons.Default.Check else Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                                tint = if (justCopiedInApp) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = if (justCopiedInApp) "Copied!" else "Copy")
                        }
                    }
                }
            }
        }

        // SwiftKey & Gboard Integration Tip
        SwiftKeyTipCard()

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
