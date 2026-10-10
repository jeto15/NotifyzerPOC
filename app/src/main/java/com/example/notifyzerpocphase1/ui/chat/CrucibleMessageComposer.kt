package com.example.notifyzerpocphase1.ui.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.notifyzerpocphase1.model.StagedAttachment
import com.example.notifyzerpocphase1.util.FilePickerUtils
import com.example.notifyzerpocphase1.viewmodel.ChatViewModel

@Composable
fun CrucibleMessageComposer(
    viewModel: ChatViewModel,
    targetContactNumber: String,
    modifier: Modifier = Modifier,
    onTypingStarted: () -> Unit = {}
) {
    val context = LocalContext.current
    val messageText by viewModel.messageText.collectAsState()
    val stagedAttachments by viewModel.stagedAttachments.collectAsState()

    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var showAttachmentMenu by remember { mutableStateOf(false) }

    // --- Launchers ---
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            tempCameraUri?.let { uri ->
                val (name, size) = FilePickerUtils.getFileDetails(context, uri)
                viewModel.addAttachment(context, uri, "image/jpeg", name, size)
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        uris.forEach { uri ->
            val (name, size) = FilePickerUtils.getFileDetails(context, uri)
            val mime = FilePickerUtils.getMimeType(context, uri)
            viewModel.addAttachment(context, uri, mime, name, size)
        }
    }

    // SAF Picker for Google Drive and Cloud storage (Images, Videos, Audio, PDFs)
    val documentPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        uris.forEach { uri ->
            val (name, size) = FilePickerUtils.getFileDetails(context, uri)
            val mime = FilePickerUtils.getMimeType(context, uri)
            viewModel.addAttachment(context, uri, mime, name, size)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding()
            .imePadding()
            .padding(8.dp)
    ) {
        // --- Attachment Staging Carousel ---
        if (stagedAttachments.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(stagedAttachments) { attachment ->
                    StagedAttachmentCard(
                        attachment = attachment,
                        onRemove = { viewModel.removeAttachment(attachment) }
                    )
                }
            }
        }

        // --- Composer Input Bar ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Camera Button
            IconButton(
                onClick = {
                    val uri = FilePickerUtils.createTempImageUri(context)
                    tempCameraUri = uri
                    cameraLauncher.launch(uri)
                },
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                    .size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.CameraAlt,
                    contentDescription = "Camera",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Attachment / Plus Menu Button
            Box {
                IconButton(
                    onClick = { showAttachmentMenu = true },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                        .size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AttachFile,
                        contentDescription = "Attach Media or Cloud File",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                DropdownMenu(
                    expanded = showAttachmentMenu,
                    onDismissRequest = { showAttachmentMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Local Photos & Videos") },
                        leadingIcon = { Icon(Icons.Rounded.PhotoLibrary, null) },
                        onClick = {
                            showAttachmentMenu = false
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageAndVideo
                                )
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Google Drive / Cloud Files (PDF, TXT)") },
                        leadingIcon = { Icon(Icons.Rounded.CloudUpload, null) },
                        onClick = {
                            showAttachmentMenu = false
                            documentPickerLauncher.launch(
                                arrayOf(
                                    "image/*",
                                    "video/*",
                                    "audio/*",
                                    "application/pdf",
                                    "text/plain",
                                    "application/msword"
                                )
                            )
                        }
                    )
                }
            }

            // Text Input Field (Auto-expanding)
            TextField(
                value = messageText,
                onValueChange = {
                    viewModel.updateMessageText(it)
                    if (it.isNotBlank()) {
                        onTypingStarted() // Trigger C (Outgoing typing activity baseline sync)
                    }
                },
                placeholder = { Text("Text message...") },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp)),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                maxLines = 4
            )

            // Dynamic Send Button
            val canSend = messageText.isNotBlank() || stagedAttachments.isNotEmpty()
            IconButton(
                onClick = { viewModel.sendMessage(targetContactNumber) },
                enabled = canSend,
                modifier = Modifier
                    .background(
                        if (canSend) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                        CircleShape
                    )
                    .size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Send,
                    contentDescription = "Send",
                    tint = if (canSend) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun StagedAttachmentCard(
    attachment: StagedAttachment,
    onRemove: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier.width(130.dp)
    ) {
        Box {
            Column(
                modifier = Modifier.padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = when {
                        attachment.mimeType.startsWith("image/") -> Icons.Rounded.Image
                        attachment.mimeType.startsWith("video/") -> Icons.Rounded.Videocam
                        else -> Icons.Rounded.InsertDriveFile
                    },
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = attachment.name,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${attachment.size / 1024} KB",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Remove 'X' Button
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(20.dp)
                    .clickable { onRemove() }
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Remove Attachment",
                    tint = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.padding(2.dp)
                )
            }
        }
    }
}
