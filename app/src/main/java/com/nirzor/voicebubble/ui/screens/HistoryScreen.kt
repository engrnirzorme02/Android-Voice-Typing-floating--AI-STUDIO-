package com.nirzor.voicebubble.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.nirzor.voicebubble.R
import com.nirzor.voicebubble.VoiceBubbleApp
import com.nirzor.voicebubble.data.SupportedLanguages
import com.nirzor.voicebubble.data.VoiceHistoryEntity
import com.nirzor.voicebubble.data.VoiceHistoryPreviewDto
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = VoiceBubbleApp.instance
    val repo = app.repository
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var searchQuery by remember { mutableStateOf("") }
    var filterFavoritesOnly by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var copiedId by remember { mutableStateOf<Long?>(null) }
    var selectedDetailId by remember { mutableStateOf<Long?>(null) }

    // Stats
    val totalCount by repo.totalCount.collectAsState(initial = 0)
    val totalWords by repo.totalWords.collectAsState(initial = 0)
    val totalChars by repo.totalChars.collectAsState(initial = 0)

    // Paging 3 items
    val pagedItems = if (filterFavoritesOnly) {
        repo.pagedFavoriteHistory.collectAsLazyPagingItems()
    } else {
        repo.pagedHistory.collectAsLazyPagingItems()
    }

    // Search results (returns previews via LIKE)
    val searchResults by if (searchQuery.isNotBlank()) {
        repo.searchHistoryPreviews(searchQuery).collectAsState(initial = emptyList())
    } else {
        remember { mutableStateOf(emptyList<VoiceHistoryPreviewDto>()) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Stats summary card
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatItem(title = stringResource(R.string.history_stat_total), value = stringResource(R.string.history_times_unit, totalCount))
                    StatItem(title = stringResource(R.string.history_stat_words), value = stringResource(R.string.history_words_unit, totalWords ?: 0))
                    StatItem(title = stringResource(R.string.history_stat_chars), value = "${totalChars ?: 0}")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(stringResource(R.string.history_search_hint)) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("history_search_input"),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Filter Chips & Clear Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !filterFavoritesOnly,
                        onClick = { filterFavoritesOnly = false },
                        label = { Text(stringResource(R.string.history_filter_all, totalCount)) }
                    )
                    FilterChip(
                        selected = filterFavoritesOnly,
                        onClick = { filterFavoritesOnly = true },
                        label = { Text(stringResource(R.string.history_filter_fav, 0)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = if (filterFavoritesOnly) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }

                if (totalCount > 0) {
                    IconButton(
                        onClick = { showClearDialog = true },
                        modifier = Modifier.testTag("clear_history_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = stringResource(R.string.history_clear_tooltip),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // List Display
            if (searchQuery.isNotBlank()) {
                // Search list
                if (searchResults.isEmpty()) {
                    EmptyHistoryPlaceholder(isSearch = true)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(searchResults.size) { index ->
                            val item = searchResults[index]
                            HistoryPreviewCard(
                                item = item,
                                isJustCopied = copiedId == item.id,
                                onCopy = {
                                    copyTextToClipboard(context, item.polishedText ?: item.rawText)
                                    copiedId = item.id
                                },
                                onCardClick = { selectedDetailId = item.id },
                                onToggleFavorite = {
                                    scope.launch { repo.toggleFavorite(item.id, !item.isFavorite) }
                                },
                                onDelete = {
                                    deleteWithUndo(context, repo, scope, snackbarHostState, item.id)
                                }
                            )
                        }
                    }
                }
            } else {
                // Paging 3 list
                if (pagedItems.itemCount == 0 && pagedItems.loadState.refresh !is LoadState.Loading) {
                    EmptyHistoryPlaceholder(isSearch = false)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(pagedItems.itemCount) { index ->
                            val item = pagedItems[index]
                            if (item != null) {
                                HistoryPreviewCard(
                                    item = item,
                                    isJustCopied = copiedId == item.id,
                                    onCopy = {
                                        copyTextToClipboard(context, item.polishedText ?: item.rawText)
                                        copiedId = item.id
                                    },
                                    onCardClick = { selectedDetailId = item.id },
                                    onToggleFavorite = {
                                        scope.launch { repo.toggleFavorite(item.id, !item.isFavorite) }
                                    },
                                    onDelete = {
                                        deleteWithUndo(context, repo, scope, snackbarHostState, item.id)
                                    }
                                )
                            }
                        }

                        if (pagedItems.loadState.append is LoadState.Loading) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Detail Dialog (Phase D.4)
    selectedDetailId?.let { detailId ->
        HistoryDetailDialog(
            id = detailId,
            onDismiss = { selectedDetailId = null },
            onDelete = {
                selectedDetailId = null
                deleteWithUndo(context, repo, scope, snackbarHostState, detailId)
            }
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(R.string.history_clear_dialog_title)) },
            text = { Text(stringResource(R.string.history_clear_dialog_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch { repo.clearAll() }
                        showClearDialog = false
                    }
                ) {
                    Text(stringResource(R.string.history_clear_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(stringResource(R.string.history_clear_cancel))
                }
            }
        )
    }
}

private fun copyTextToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Voice Transcription", text))
    Toast.makeText(context, R.string.copied_toast, Toast.LENGTH_SHORT).show()
}

private fun deleteWithUndo(
    context: Context,
    repo: com.nirzor.voicebubble.data.VoiceHistoryRepository,
    scope: kotlinx.coroutines.CoroutineScope,
    snackbarHostState: SnackbarHostState,
    id: Long
) {
    scope.launch {
        val entity = repo.getById(id) ?: return@launch
        repo.deleteById(id)

        val result = snackbarHostState.showSnackbar(
            message = context.getString(R.string.history_deleted_toast),
            actionLabel = "Undo",
            duration = SnackbarDuration.Short
        )
        if (result == SnackbarResult.ActionPerformed) {
            repo.insert(entity)
            Toast.makeText(context, R.string.history_undo_toast, Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
private fun HistoryPreviewCard(
    item: VoiceHistoryPreviewDto,
    isJustCopied: Boolean,
    onCopy: () -> Unit,
    onCardClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    val lang = remember(item.language) { SupportedLanguages.getLanguageByCode(item.language) }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    val dateString = remember(item.createdAt) { dateFormat.format(Date(item.createdAt)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() }
            .testTag("history_item_${item.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = lang.flag, fontSize = 16.sp)
                    Text(
                        text = dateString,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onToggleFavorite, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (item.isFavorite) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Substring 200 Preview
            Text(
                text = item.previewText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${item.wordCount} শব্দ • ${item.charCount} অক্ষর",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isJustCopied) Color(0xFF10B981).copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.clickable { onCopy() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isJustCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = null,
                            tint = if (isJustCopied) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (isJustCopied) stringResource(R.string.history_item_copied) else stringResource(R.string.history_copy_action),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isJustCopied) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryDetailDialog(
    id: Long,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val repo = VoiceBubbleApp.instance.repository
    val scope = rememberCoroutineScope()

    var entity by remember { mutableStateOf<VoiceHistoryEntity?>(null) }
    var isEditing by remember { mutableStateOf(false) }
    var editedText by remember { mutableStateOf("") }

    androidx.compose.runtime.LaunchedEffect(id) {
        val loaded = repo.getById(id)
        entity = loaded
        editedText = loaded?.text ?: ""
    }

    if (entity == null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            text = { Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } },
            confirmButton = {}
        )
        return
    }

    val item = entity!!

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.history_detail_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Polished text if available
                if (item.polishedText != null) {
                    Text(
                        text = stringResource(R.string.history_detail_polished),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (isEditing) {
                        OutlinedTextField(
                            value = editedText,
                            onValueChange = { editedText = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            text = item.polishedText,
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 22.sp
                        )
                    }
                    HorizontalDivider()
                }

                // Raw transcript
                Text(
                    text = stringResource(R.string.history_detail_raw),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isEditing && item.polishedText == null) {
                    OutlinedTextField(
                        value = editedText,
                        onValueChange = { editedText = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        text = item.rawText.ifBlank { item.text },
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (isEditing) {
                    Button(
                        onClick = {
                            scope.launch {
                                val updated = item.copy(
                                    text = editedText,
                                    polishedText = if (item.polishedText != null) editedText else null,
                                    rawText = if (item.polishedText == null) editedText else item.rawText
                                )
                                repo.update(updated)
                                entity = updated
                                isEditing = false
                                Toast.makeText(context, "সংরক্ষণ করা হয়েছে!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.history_detail_save))
                    }
                } else {
                    OutlinedButton(onClick = { isEditing = true }) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.history_detail_edit))
                    }

                    if (item.polishedText != null) {
                        Button(onClick = { copyTextToClipboard(context, item.polishedText) }) {
                            Text(stringResource(R.string.history_detail_copy_polished))
                        }
                    }
                    OutlinedButton(onClick = { copyTextToClipboard(context, item.rawText.ifBlank { item.text }) }) {
                        Text(stringResource(R.string.history_detail_copy_raw))
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDelete) {
                Text(stringResource(R.string.history_detail_delete), color = MaterialTheme.colorScheme.error)
            }
        }
    )
}

@Composable
private fun EmptyHistoryPlaceholder(isSearch: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            Text(
                text = if (isSearch) "কোনো ফলাফল পাওয়া যায়নি" else stringResource(R.string.history_empty_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.history_empty_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun StatItem(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
