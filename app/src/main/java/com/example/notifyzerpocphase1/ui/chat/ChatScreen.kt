package com.example.notifyzerpocphase1.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
                                imageVector = Icons.Default.Assignment,
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
                    text = "MMS attachments and SMS dispatch ready.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
