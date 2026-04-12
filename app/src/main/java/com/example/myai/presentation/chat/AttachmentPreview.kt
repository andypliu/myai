package com.example.myai.presentation.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.myai.domain.model.FileAttachment
import com.example.myai.util.FileProcessor

@Composable
fun AttachmentPreview(
    attachments: List<FileAttachment>,
    onRemoveAttachment: (FileAttachment) -> Unit,
    modifier: Modifier = Modifier
) {
    if (attachments.isEmpty()) return

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(attachments) { attachment ->
            AttachmentItem(
                attachment = attachment,
                onRemove = { onRemoveAttachment(attachment) }
            )
        }
    }
}

@Composable
fun AttachmentItem(
    attachment: FileAttachment,
    onRemove: () -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .width(100.dp)
            .height(100.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        // Content based on file type
        if (FileProcessor.isImage(attachment.mimeType)) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(android.net.Uri.parse(attachment.uri))
                    .crossfade(true)
                    .build(),
                contentDescription = attachment.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(80.dp)
            )
        } else {
            // Document icon
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = "Document",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        // File name
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .align(Alignment.BottomCenter),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = attachment.name,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        // Remove button
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .size(24.dp)
                .align(Alignment.TopEnd)
                .padding(2.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
