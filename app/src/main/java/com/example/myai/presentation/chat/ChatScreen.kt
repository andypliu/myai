package com.example.myai.presentation.chat

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myai.domain.model.ChatMessage
import com.example.myai.domain.model.FileAttachment
import com.example.myai.presentation.profile.ProfileViewModel
import com.example.myai.domain.util.FileProcessor
import com.example.myai.ui.theme.GreenDark
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

    // Detect if keyboard is open to scroll to bottom
    val isKeyboardOpen = WindowInsets.ime.asPaddingValues().calculateBottomPadding() > 0.dp
    LaunchedEffect(isKeyboardOpen) {
        if (isKeyboardOpen && messages.isNotEmpty()) {
            delay(100)
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Monitor for 403 error and mark model as unauthorized
    LaunchedEffect(uiState) {
        if (uiState is ChatUiState.Error) {
            val error = (uiState as ChatUiState.Error).message
            if (error.contains("subscription", ignoreCase = true)) {
                profileViewModel.markModelAsUnauthorized(selectedModel)
            }
        }
    }

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            val fileInfo = FileProcessor.getFileInfo(context, it)
            fileInfo?.let { attachment ->
                viewModel.addAttachment(attachment)
            }
        }
    }

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Scroll to bottom when a new message is added (for long responses)
    LaunchedEffect(newMessageId) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                })
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header with colored background that includes status bar area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Chat with AI",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    // Display selected model name in dark green
                    if (selectedModel.isNotEmpty()) {
                        Text(
                            text = selectedModel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = GreenDark
                        )
                    }
                }
            }

            // Messages list
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }
                items(messages) { message ->
                    MessageBubble(message = message)
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

        // Error message
        if (uiState is ChatUiState.Error) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = {
                            viewModel.clearError()
                        })
                    },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.wrapContentWidth()
                ) {
                    Text(
                        text = (uiState as ChatUiState.Error).message,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
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

    // Animated dots for typing indicator
    var dotCount by remember { mutableIntStateOf(1) }
    LaunchedEffect(message.isTyping) {
        if (message.isTyping) {
            while (true) {
                delay(500)
                dotCount = (dotCount % 3) + 1
            }
        }
    }

    val displayContent = if (message.isTyping) {
        ".".repeat(dotCount)
    } else {
        message.content
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Card(
            shape = MessageBubbleShape(message.isUser),
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            modifier = Modifier
                .widthIn(max = (configuration.screenWidthDp.dp * 0.8f))
                .wrapContentWidth()
                .padding(horizontal = 8.dp)
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Attachment button
            IconButton(
                onClick = onPickFile,
                enabled = !isLoading
            ) {
                Icon(
                    imageVector = Icons.Default.AttachFile,
                    contentDescription = "Attach file",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message...") },
                enabled = !isLoading,
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
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (text.isNotBlank() || selectedAttachments.isNotEmpty()) {
                        onSendMessage(text)
                        text = ""
                    }
                },
                enabled = !isLoading && (text.isNotBlank() || selectedAttachments.isNotEmpty())
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if ((text.isNotBlank() || selectedAttachments.isNotEmpty()) && !isLoading) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    }
                )
            }
        }
    }
}
