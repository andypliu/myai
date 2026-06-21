package com.example.myai.presentation.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HeartBroken
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.HeartBroken
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myai.domain.model.ChatMessage
import com.example.myai.domain.model.FileAttachment
import com.example.myai.domain.model.AiServiceType
import com.example.myai.presentation.profile.ProfileViewModel
import com.example.myai.domain.util.FileProcessor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    profileViewModel: ProfileViewModel,
    viewModel: ChatViewModel
) {
    val selectedModel by profileViewModel.selectedModel.collectAsState()
    val selectedService by profileViewModel.selectedService.collectAsState()
    // Update viewModel when selectedModel or selectedService changes
    androidx.compose.runtime.LaunchedEffect(selectedModel, selectedService) {
        viewModel.setSelectedModel(selectedModel)
        viewModel.setSelectedService(selectedService)
    }
    val context = LocalContext.current
    val messages by viewModel.messages.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val selectedAttachments by viewModel.selectedAttachments.collectAsState()
    val listState = rememberLazyListState()
    val newMessageId by viewModel.newMessageId.collectAsState()
    val focusManager = LocalFocusManager.current

    // Detect if keyboard is open to scroll to bottom without excessive recompositions
    val density = LocalDensity.current
    val ime = WindowInsets.ime
    val isKeyboardOpen by remember(ime, density) {
        derivedStateOf { ime.getBottom(density) > 0 }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val attachment = FileProcessor.getFileInfo(context, it)
            attachment?.let { att -> viewModel.addAttachment(att) }
        }
    }

    // Scroll to bottom when a new message is added or keyboard is opened
    LaunchedEffect(messages.size, newMessageId, isKeyboardOpen) {
        if (messages.isNotEmpty()) {
            delay(100) // Give layout a moment to settle
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Sticky Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text = "Chat with AI",
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(16.dp),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimary,
                    textAlign = TextAlign.Start
                )
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }
                items(messages, key = { it.id }) { message ->
                    MessageBubble(
                        message = message,
                        isLiked = message.feedback,
                        onFeedback = { isLiked -> 
                            if (isLiked == null) viewModel.removeFeedback(message.id)
                            else viewModel.toggleFeedback(message.id, isLiked)
                        }
                    )
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }

            // Input area
            ChatInput(
                onSendMessage = { viewModel.sendMessage(it, context) },
                isLoading = uiState is ChatUiState.Loading,
                selectedAttachments = selectedAttachments,
                onAddAttachment = { viewModel.addAttachment(it) },
                onRemoveAttachment = { viewModel.removeAttachment(it) },
                onPickFile = { filePickerLauncher.launch("*/*") },
                modifier = Modifier.imePadding()
            )
        }
    }
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    isLiked: Boolean? = null,
    onFeedback: (Boolean?) -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val alignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    val backgroundColor = if (message.isUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val textColor = if (message.isUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }

    var showFeedbackPopup by remember { mutableStateOf(false) }

    // Animated dots for typing indicator
    var dotCount by remember { mutableIntStateOf(1) }
    LaunchedEffect(message.isTyping) {
        if (message.isTyping) {
            while (true) {
                delay(400)
                dotCount = (dotCount % 3) + 1
            }
        }
    }

    val displayContent = if (message.isTyping) {
        "Typing" + ".".repeat(dotCount)
    } else {
        message.content
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = (configuration.screenWidthDp.dp * 0.85f))
                .padding(horizontal = 8.dp),
            horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
        ) {
            Card(
                shape = MessageBubbleShape(message.isUser),
                colors = CardDefaults.cardColors(containerColor = backgroundColor),
                modifier = Modifier
                    .widthIn(max = (configuration.screenWidthDp.dp * 0.8f))
                    .wrapContentWidth()
                    .pointerInput(message.id) {
                        detectTapGestures(
                            onLongPress = {
                                if (!message.isUser && !message.isTyping) {
                                    showFeedbackPopup = !showFeedbackPopup
                                }
                            }
                        )
                    }
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Display attachments
                    message.attachments?.let { attachments ->
                        AttachmentPreview(
                            attachments = attachments,
                            onRemoveAttachment = {},
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    // Display message content
                    if (displayContent.isNotBlank()) {
                        Text(
                            text = displayContent,
                            color = textColor,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Feedback selection popup
            if (showFeedbackPopup) {
                Row(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = {
                            onFeedback(true)
                            showFeedbackPopup = false
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isLiked == true) Icons.Default.Favorite else Icons.Outlined.Favorite,
                            contentDescription = "Love",
                            tint = if (isLiked == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = {
                            onFeedback(false)
                            showFeedbackPopup = false
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isLiked == false) Icons.Default.HeartBroken else Icons.Outlined.HeartBroken,
                            contentDescription = "Dislove",
                            tint = if (isLiked == false) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = {
                            onFeedback(null)
                            showFeedbackPopup = false
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Display current feedback icon and timestamp below bubble
            Row(
                modifier = Modifier
                    .padding(top = 2.dp, start = 4.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
            ) {
                val sdf = remember { SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault()) }
                Text(
                    text = sdf.format(Date(message.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )

                if (isLiked != null && !showFeedbackPopup) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.HeartBroken,
                        contentDescription = if (isLiked) "Loved" else "Disloved",
                        tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ChatInput(
    onSendMessage: (String) -> Unit,
    isLoading: Boolean,
    selectedAttachments: List<FileAttachment>,
    onAddAttachment: (FileAttachment) -> Unit,
    onRemoveAttachment: (FileAttachment) -> Unit,
    onPickFile: () -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        // Attachment preview
        AttachmentPreview(
            attachments = selectedAttachments,
            onRemoveAttachment = onRemoveAttachment
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPickFile,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AttachFile,
                    contentDescription = "Attach File",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
                placeholder = { Text("Type a message...") },
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (text.isNotBlank() || selectedAttachments.isNotEmpty()) {
                            onSendMessage(text)
                            text = ""
                        }
                    }
                ),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            )

            IconButton(
                onClick = {
                    if (text.isNotBlank() || selectedAttachments.isNotEmpty()) {
                        onSendMessage(text)
                        text = ""
                    }
                },
                enabled = !isLoading && (text.isNotBlank() || selectedAttachments.isNotEmpty()),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if ((text.isNotBlank() || selectedAttachments.isNotEmpty())) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    }
                )
            }
        }
    }
}
