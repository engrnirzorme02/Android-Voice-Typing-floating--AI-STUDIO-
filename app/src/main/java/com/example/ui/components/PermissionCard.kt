package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PermissionCard(
    hasOverlayPermission: Boolean,
    hasMicPermission: Boolean,
    hasNotificationPermission: Boolean,
    onRequestOverlay: () -> Unit,
    onRequestMic: () -> Unit,
    onRequestNotification: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allGranted = hasOverlayPermission && hasMicPermission && hasNotificationPermission

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("permission_status_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (allGranted) {
                MaterialTheme.colorScheme.surfaceContainerLow
            } else {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
            }
        ),
        border = BorderStroke(
            1.dp,
            if (allGranted) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
        )
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (allGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = "Permission Status",
                        tint = if (allGranted) Color(0xFF10B981) else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = if (allGranted) "প্রয়োজনীয় সব পারমিশন দেওয়া আছে" else "পারমিশন প্রয়োজন (Setup Permissions)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Permission Items
            PermissionItemRow(
                title = "স্ক্রিন ওভারলে বাবল (Display over other apps)",
                description = "মেসেঞ্জারের মতো স্ক্রিনের উপরে ভাসমান বাবল দেখানোর জন্য",
                icon = Icons.Default.Layers,
                isGranted = hasOverlayPermission,
                onRequest = onRequestOverlay,
                buttonTag = "grant_overlay_btn"
            )

            Spacer(modifier = Modifier.height(8.dp))

            PermissionItemRow(
                title = "মাইক্রোফোন (Record Audio)",
                description = "ভয়েস শুনে বাংলায় বা ইংরেজিতে রূপান্তর করার জন্য",
                icon = Icons.Default.Mic,
                isGranted = hasMicPermission,
                onRequest = onRequestMic,
                buttonTag = "grant_mic_btn"
            )

            Spacer(modifier = Modifier.height(8.dp))

            PermissionItemRow(
                title = "নোটিফিকেশন (Background Service)",
                description = "সার্ভিসটি ব্যাকগ্রাউন্ডে নিরবচ্ছিন্নভাবে চালু রাখার জন্য",
                icon = Icons.Default.Notifications,
                isGranted = hasNotificationPermission,
                onRequest = onRequestNotification,
                buttonTag = "grant_notification_btn"
            )
        }
    }
}

@Composable
private fun PermissionItemRow(
    title: String,
    description: String,
    icon: ImageVector,
    isGranted: Boolean,
    onRequest: () -> Unit,
    buttonTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = if (isGranted) Color(0xFF10B981).copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = if (isGranted) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (isGranted) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF10B981).copy(alpha = 0.15f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Granted",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "চালু আছে",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            Button(
                onClick = onRequest,
                shape = RoundedCornerShape(12.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag(buttonTag)
            ) {
                Text(
                    text = "Grant",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun Box(
    contentAlignment: Alignment,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.Box(
        contentAlignment = contentAlignment,
        modifier = Modifier.size(36.dp)
    ) {
        content()
    }
}
