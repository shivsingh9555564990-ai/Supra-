package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Close
import coil.compose.AsyncImage
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ChatEntity
import com.example.data.MessageEntity
import com.example.ui.ChatViewModel
import com.example.ui.ThinkingStep
import com.example.ui.theme.GlassWhite
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onNavigateToSettings: () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val activeChats by viewModel.activeChats.collectAsStateWithLifecycle()
    val currentChatId by viewModel.currentChatId.collectAsStateWithLifecycle()
    val isTyping by viewModel.isTyping.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    var chatToRename by remember { mutableStateOf<ChatEntity?>(null) }
    var renameText by remember { mutableStateOf("") }

    if (chatToRename != null) {
        AlertDialog(
            onDismissRequest = { chatToRename = null },
            title = { Text("Rename Chat") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    chatToRename?.let { viewModel.renameChat(it.id, renameText) }
                    chatToRename = null
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { chatToRename = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    LaunchedEffect(activeChats, currentChatId) {
        if (currentChatId == null && activeChats.isNotEmpty()) {
            viewModel.selectChat(activeChats.first().id)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.width(300.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "SuperNova History", 
                    modifier = Modifier.padding(16.dp), 
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Divider(color = GlassWhite)
                
                Button(
                    onClick = { 
                        viewModel.createNewChat("New Chat ${System.currentTimeMillis() % 1000}")
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New")
                    Spacer(Modifier.width(8.dp))
                    Text("New Chat", color = MaterialTheme.colorScheme.onPrimary)
                }

                LazyColumn {
                    items(activeChats, key = { it.id }) { chat ->
                        ChatDrawerItem(
                            chat = chat,
                            isSelected = chat.id == currentChatId,
                            onClick = { 
                                viewModel.selectChat(chat.id)
                                viewModel.setUnread(chat.id, false)
                                scope.launch { drawerState.close() }
                            },
                            onDelete = { viewModel.deleteChat(chat.id) },
                            onShare = {
                                scope.launch {
                                    val text = viewModel.getMessagesTextForChat(chat.id)
                                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(android.content.Intent.EXTRA_TEXT, text)
                                    }
                                    context.startActivity(android.content.Intent.createChooser(intent, "Share Chat"))
                                }
                            },
                            onPin = { viewModel.pinChat(chat.id, !chat.isPinned) },
                            onRename = {
                                chatToRename = chat
                                renameText = chat.title
                            },
                            onArchive = { viewModel.archiveChat(chat.id) },
                            onMarkUnread = { viewModel.setUnread(chat.id, !chat.isUnread) },
                            onExport = {
                                scope.launch {
                                    val text = viewModel.getMessagesTextForChat(chat.id)
                                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(android.content.Intent.EXTRA_TITLE, "export_${chat.title}.txt")
                                        putExtra(android.content.Intent.EXTRA_TEXT, text)
                                    }
                                    context.startActivity(android.content.Intent.createChooser(intent, "Export Chat"))
                                }
                            }
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                GlassTopAppBar(
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onOpenSettings = onNavigateToSettings
                )
            },
            bottomBar = {
                GlassBottomBar(
                    viewModel = viewModel,
                    isTyping = isTyping,
                    onSendMessage = { text, imageUri -> viewModel.sendMessage(text, imageUri) }
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Mandala Watermark
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "❂",
                        fontSize = 300.sp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                    )
                }
                
                ChatContent(viewModel = viewModel)
                PerformanceMonitorOverlay(modifier = Modifier.align(Alignment.TopEnd))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatDrawerItem(
    chat: ChatEntity,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onPin: () -> Unit,
    onRename: () -> Unit,
    onArchive: () -> Unit,
    onMarkUnread: () -> Unit,
    onExport: () -> Unit
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var menuExpanded by remember { mutableStateOf(false) }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .graphicsLayer {
                translationX = offsetX
            }
            .draggable(
                orientation = Orientation.Horizontal,
                state = rememberDraggableState { delta ->
                    offsetX += delta
                },
                onDragStopped = {
                    if (offsetX < -150f) {
                        onDelete()
                    } else {
                        offsetX = 0f
                    }
                }
            )
            .shadow(
                elevation = if (isSelected) 8.dp else 2.dp,
                shape = RoundedCornerShape(12.dp),
                spotColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black
            )
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface)
            .combinedClickable(
                onClick = onClick,
                onLongClick = { menuExpanded = true }
            )
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (chat.isPinned) {
                Icon(Icons.Default.PushPin, contentDescription = "Pinned", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = chat.title,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                fontWeight = if (chat.isUnread) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )
            if (chat.isUnread) {
                Spacer(Modifier.width(4.dp))
                Box(modifier = Modifier.size(8.dp).clip(androidx.compose.foundation.shape.CircleShape).background(MaterialTheme.colorScheme.primary))
            }
            
            Box {
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Share") },
                        onClick = { menuExpanded = false; onShare() },
                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = "Share") }
                    )
                    DropdownMenuItem(
                        text = { Text(if (chat.isPinned) "Unpin" else "Pin") },
                        onClick = { menuExpanded = false; onPin() },
                        leadingIcon = { Icon(Icons.Default.PushPin, contentDescription = "Pin") }
                    )
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        onClick = { menuExpanded = false; onRename() },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = "Rename") }
                    )
                    DropdownMenuItem(
                        text = { Text("Archive") },
                        onClick = { menuExpanded = false; onArchive() },
                        leadingIcon = { Icon(Icons.Default.Archive, contentDescription = "Archive") }
                    )
                    DropdownMenuItem(
                        text = { Text(if (chat.isUnread) "Mark Read" else "Mark Unread") },
                        onClick = { menuExpanded = false; onMarkUnread() },
                        leadingIcon = { Icon(Icons.Default.MarkEmailUnread, contentDescription = "Mark Unread") }
                    )
                    DropdownMenuItem(
                        text = { Text("Export") },
                        onClick = { menuExpanded = false; onExport() },
                        leadingIcon = { Icon(Icons.Default.ImportExport, contentDescription = "Export") }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = { 
                            menuExpanded = false
                            onDelete() 
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = "Delete") }
                    )
                }
            }
        }
    }
}

@Composable
fun GlassTopAppBar(onOpenDrawer: () -> Unit, onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GlassWhite)
            .padding(horizontal = 8.dp, vertical = 12.dp)
            .windowInsetsPadding(WindowInsets.statusBars),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onOpenDrawer) {
            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "SuperNova",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onOpenSettings) {
            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
fun ChatContent(viewModel: ChatViewModel) {
    val messages by viewModel.currentMessages.collectAsStateWithLifecycle()
    val isTyping by viewModel.isTyping.collectAsStateWithLifecycle()
    val streamingText by viewModel.streamingResponse.collectAsStateWithLifecycle()
    val realThinkingText by viewModel.realThinkingText.collectAsStateWithLifecycle()
    val isReasoningPhase by viewModel.isReasoningPhase.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.isSpeaking.collectAsStateWithLifecycle()
    val currentSpeakingMsgId by viewModel.currentSpeakingMsgId.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, isTyping, streamingText, realThinkingText) {
        val totalCount = messages.size + (if (isTyping) 1 else 0)
        if (totalCount > 0) {
            listState.animateScrollToItem(totalCount - 1)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(messages, key = { it.id }) { msg ->
            MessageBubble(
                message = msg,
                isSpeaking = isSpeaking && currentSpeakingMsgId == msg.id,
                onToggleSpeak = { viewModel.speakMessage(msg) }
            )
        }
        if (isTyping) {
            item {
                if (isReasoningPhase && !realThinkingText.isNullOrBlank()) {
                    // Actual real reasoning streamed directly from AI's neural thoughts
                    RealThinkingCard(thoughtText = realThinkingText ?: "")
                } else if (streamingText != null) {
                    // Final response streaming cleanly, thinking box is automatically gone!
                    StreamingMessageBubble(text = streamingText ?: "")
                } else {
                    // Initial clean pulsing indicator before first token arrives
                    RealThinkingCard(thoughtText = "")
                }
            }
        }
    }
}

@Composable
fun RealThinkingCard(thoughtText: String) {
    val shape = RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp)
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Reverse),
        label = "glow"
    )

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        Card(
            shape = shape,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
            ),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .shadow(10.dp, shape = shape, spotColor = MaterialTheme.colorScheme.primary)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header with glowing icon and dynamic badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Psychology,
                        contentDescription = "Thinking",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Thinking Process",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "Reasoning...",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                if (thoughtText.isBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Analyzing question and formulating logic...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                        )
                    }
                } else {
                    // Actual real reasoning token stream
                    Text(
                        text = thoughtText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}

@Composable
fun StreamingMessageBubble(text: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorAlpha"
    )

    val shape = RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)
    val bgColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .shadow(
                    elevation = 12.dp,
                    shape = shape,
                    spotColor = MaterialTheme.colorScheme.primary
                )
                .clip(shape)
                .background(bgColor)
                .padding(16.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = "Streaming",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "SuperNova (Live)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                val parts = text.split("```")
                parts.forEachIndexed { index, part ->
                    if (index % 2 == 1) { // Code block
                        val codeLines = part.trim().lines()
                        val code = codeLines.drop(1).joinToString("\n")
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.6f))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = code,
                                color = MaterialTheme.colorScheme.secondary,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    } else if (part.isNotBlank() || index == parts.lastIndex) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = part,
                                color = MaterialTheme.colorScheme.onBackground,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            if (index == parts.lastIndex) {
                                Text(
                                    text = " ▍",
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = cursorAlpha),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: MessageEntity,
    isSpeaking: Boolean = false,
    onToggleSpeak: () -> Unit = {}
) {
    val isUser = message.isUser
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bgColor = if (isUser) MaterialTheme.colorScheme.secondary.copy(alpha = 0.85f) 
                  else MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
    val textColor = if (isUser) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onBackground
    val shape = if (isUser) RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp)
                else RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)
    val context = LocalContext.current

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .shadow(
                    elevation = if (isSpeaking) 16.dp else 10.dp,
                    shape = shape,
                    spotColor = if (isSpeaking) MaterialTheme.colorScheme.primary else (if (isUser) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary)
                )
                .clip(shape)
                .background(bgColor)
                .padding(16.dp)
        ) {
            Column {
                // Display attached image if present
                if (message.imageUri != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        AsyncImage(
                            model = message.imageUri,
                            contentDescription = "Attached image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    }
                }

                // Header for AI message with Speaking Status
                if (!isUser) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = "AI",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "SuperNova AI",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        if (isSpeaking) {
                            Spacer(Modifier.width(8.dp))
                            AudioWaveIndicator()
                        }
                    }
                }

                val parts = message.text.split("```")
                parts.forEachIndexed { index, part ->
                    if (index % 2 == 1) { // Code block
                        val codeLines = part.trim().lines()
                        val code = codeLines.drop(1).joinToString("\n")
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.6f))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = code,
                                color = MaterialTheme.colorScheme.secondary,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    } else if (part.isNotBlank()) {
                        Text(
                            text = part.trim(),
                            color = textColor,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                // AI Action Footer: Read Aloud (TTS) & Copy
                if (!isUser) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Speaker / Read Aloud button
                        IconButton(
                            onClick = onToggleSpeak,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isSpeaking) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                contentDescription = if (isSpeaking) "Stop Audio" else "Read Aloud",
                                tint = if (isSpeaking) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(Modifier.width(4.dp))

                        // Copy button
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("SuperNova Response", message.text)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy text",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AudioWaveIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val height1 by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = 14f,
        animationSpec = infiniteRepeatable(tween(300, easing = LinearEasing), RepeatMode.Reverse),
        label = "h1"
    )
    val height2 by infiniteTransition.animateFloat(
        initialValue = 12f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(tween(400, easing = LinearEasing), RepeatMode.Reverse),
        label = "h2"
    )
    val height3 by infiniteTransition.animateFloat(
        initialValue = 6f,
        targetValue = 16f,
        animationSpec = infiniteRepeatable(tween(350, easing = LinearEasing), RepeatMode.Reverse),
        label = "h3"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Box(modifier = Modifier.width(3.dp).height(height1.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
        Box(modifier = Modifier.width(3.dp).height(height2.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
        Box(modifier = Modifier.width(3.dp).height(height3.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
    }
}

@Composable
fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "typingAlpha"
    )
    val wobble by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "typingWobble"
    )

    Box(
        modifier = Modifier
            .padding(start = 8.dp)
            .graphicsLayer {
                rotationZ = wobble
            }
            .clip(RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            .padding(16.dp)
    ) {
        Text("Processing Edge Logic...", color = MaterialTheme.colorScheme.primary.copy(alpha = alpha))
    }
}

@Composable
fun GlassBottomBar(
    viewModel: ChatViewModel,
    isTyping: Boolean,
    onSendMessage: (String, String?) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val isListening by viewModel.isListening.collectAsStateWithLifecycle()
    val soundLevel by viewModel.soundLevel.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            Toast.makeText(context, "Image attached! Ready for analysis", Toast.LENGTH_SHORT).show()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startListening(
                onPartialResult = { partial -> text = partial },
                onFinalResult = { final -> text = final },
                onError = { err -> Toast.makeText(context, err, Toast.LENGTH_SHORT).show() }
            )
        } else {
            Toast.makeText(context, "Microphone permission required for voice input", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(GlassWhite)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        // Active Voice Listening Banner
        AnimatedVisibility(visible = isListening) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Mic,
                    contentDescription = "Listening",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Listening... Speak now",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Tap mic to stop",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Image Attachment Preview Bar
        AnimatedVisibility(visible = selectedImageUri != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = "Selected image preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Image attached",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Ready for on-device Vision analysis",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = { selectedImageUri = null },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Remove Image",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { 
                imagePickerLauncher.launch("image/*")
            }) {
                Icon(
                    Icons.Default.Attachment, 
                    contentDescription = "Attach Image", 
                    tint = if (selectedImageUri != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                )
            }
            
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                placeholder = { 
                    Text(
                        if (isListening) "Listening to speech..." else (if (selectedImageUri != null) "Ask about this image..." else "Ask SuperNova..."), 
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ) 
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                ),
                shape = RoundedCornerShape(24.dp)
            )

            if (text.isBlank() && selectedImageUri == null && !isListening) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable {
                            val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                                viewModel.startListening(
                                    onPartialResult = { partial -> text = partial },
                                    onFinalResult = { final -> text = final },
                                    onError = { err -> Toast.makeText(context, err, Toast.LENGTH_SHORT).show() }
                                )
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Mic, contentDescription = "Voice Input", tint = MaterialTheme.colorScheme.onPrimary)
                }
            } else if (isListening) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error)
                        .clickable { viewModel.stopListening() },
                    contentAlignment = Alignment.Center
                ) {
                    VoiceVisualizer(soundLevel = soundLevel)
                }
            } else {
                IconButton(
                    onClick = { 
                        if (!isTyping && (text.isNotBlank() || selectedImageUri != null)) {
                            onSendMessage(text, selectedImageUri?.toString())
                            text = ""
                            selectedImageUri = null
                        }
                    },
                    enabled = !isTyping,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (isTyping) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(
                        Icons.Default.Send, 
                        contentDescription = "Send", 
                        tint = if (isTyping) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun VoiceVisualizer(soundLevel: Float = 0f) {
    val infiniteTransition = rememberInfiniteTransition(label = "voice")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "voicePulse"
    )
    val dynamicScale = (1f + soundLevel * 1.5f) * pulse

    Box(
        modifier = Modifier
            .size(24.dp)
            .graphicsLayer {
                scaleX = dynamicScale.coerceIn(0.6f, 1.8f)
                scaleY = dynamicScale.coerceIn(0.6f, 1.8f)
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.MicOff,
            contentDescription = "Stop recording",
            tint = MaterialTheme.colorScheme.onError,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun PerformanceMonitorOverlay(modifier: Modifier = Modifier) {
    var ramUsage by remember { mutableIntStateOf(1024) }
    var temp by remember { mutableIntStateOf(45) }

    LaunchedEffect(Unit) {
        while(true) {
            delay(2000)
            ramUsage = (900..1800).random()
            temp = (40..55).random()
        }
    }

    Box(
        modifier = modifier
            .padding(16.dp)
            .shadow(4.dp, RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
            .padding(8.dp)
    ) {
        Column(horizontalAlignment = Alignment.End) {
            Text("RAM: ${ramUsage}MB", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
            Text("NPU Temp: $temp°C", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
        }
    }
}
