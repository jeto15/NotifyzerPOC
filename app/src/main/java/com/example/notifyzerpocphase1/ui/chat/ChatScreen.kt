package com.example.notifyzerpocphase1.ui.chat

import android.provider.Telephony
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Assignment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.notifyzerpocphase1.ui.DossierModalBottomSheet
import com.example.notifyzerpocphase1.ui.HardDossierLoadingOverlay
import com.example.notifyzerpocphase1.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    targetContactNumber: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showHardLoading by remember { mutableStateOf(false) }
    var showDossierSheet by remember { mutableStateOf(false) }
    val assetLiabilityScore: Int? = null // Can be bound to EntityProfile score in ChatViewModel

    val conversation by viewModel.conversation.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(targetContactNumber) {
        viewModel.loadConversation(context, targetContactNumber)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(text = "Chat with $targetContactNumber") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    actions = {
                        // Trigger A: Manual Dossier Generation
                        IconButton(
                            onClick = { showHardLoading = true }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Assignment,
                                contentDescription = "Generate Dossier",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )
                // Pinned Asset/Liability HUD below TopAppBar
                AssetLiabilityHud(score = assetLiabilityScore)
            }
        },
        bottomBar = {
            CrucibleMessageComposer(
                viewModel = viewModel,
                targetContactNumber = targetContactNumber,
                onTypingStarted = {
                    // Trigger C: User started typing -> initiate silent baseline analysis if unanalyzed
                }
            )
        }
    ) { innerPadding ->
        if (conversation.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Active Conversation Thread",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Start messaging now. MMS attachments and SMS dispatch ready.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                reverseLayout = true
            ) {
                items(conversation.reversed()) { sms ->
                    val isSent = sms.type == Telephony.Sms.MESSAGE_TYPE_SENT
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isSent) Arrangement.End else Arrangement.Start
                    ) {
                        Surface(
                            shape = RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (isSent) 16.dp else 4.dp,
                                bottomEnd = if (isSent) 4.dp else 16.dp
                            ),
                            color = if (isSent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = sms.body,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                color = if (isSent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    // Hard Loading Overlay Sequence (Trigger A)
    if (showHardLoading) {
        HardDossierLoadingOverlay(
            contactName = targetContactNumber,
            onLoadingComplete = {
                showHardLoading = false
                showDossierSheet = true
            }
        )
    }

    // Dossier Modal Bottom Sheet Presentation
    if (showDossierSheet) {
        DossierModalBottomSheet(
            contactName = targetContactNumber,
            onDismiss = { showDossierSheet = false }
        )
    }
}
