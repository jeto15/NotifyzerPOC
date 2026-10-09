package com.example.notifyzerpocphase1.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.notifyzerpocphase1.model.CapturedNotification
import com.example.notifyzerpocphase1.ui.theme.CrucibleIntelligenceTheme
import com.example.notifyzerpocphase1.util.ApiKeyManager
import com.example.notifyzerpocphase1.util.ContactUtils
import com.example.notifyzerpocphase1.util.PermissionUtils
import com.example.notifyzerpocphase1.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val notifications by viewModel.notifications.collectAsState()
    val isPermissionGranted by viewModel.isPermissionGranted.collectAsState()
    val dossierState by viewModel.dossierState.collectAsState()
    val isLoadingDossier by viewModel.isLoadingDossier.collectAsState()
    val dossierError by viewModel.dossierError.collectAsState()

    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showJsonImportDialog by remember { mutableStateOf(false) }
    var hardLoadingContact by remember { mutableStateOf<String?>(null) }
    var bottomSheetContact by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val smsGranted = permissions[Manifest.permission.READ_SMS] ?: false
        if (smsGranted) {
            viewModel.autoSyncTargetNumber(context)
        }
    }

    LaunchedEffect(Unit) {
        val smsGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED

        if (smsGranted) {
            viewModel.autoSyncTargetNumber(context)
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_SMS,
                    Manifest.permission.READ_CONTACTS
                )
            )
        }
    }

    val filteredNotifications = remember(notifications) {
        viewModel.getFilteredNotifications(context, notifications)
    }

    MainScreenContent(
        notifications = filteredNotifications,
        isPermissionGranted = isPermissionGranted,
        dossierState = dossierState,
        isLoadingDossier = isLoadingDossier,
        dossierError = dossierError,
        onClearLogs = { viewModel.clearLogs() },
        onLoadMockData = { viewModel.loadMockData(context) },
        onOpenJsonImportDialog = { showJsonImportDialog = true },
        onTriggerHardDossierLoading = { contactName -> hardLoadingContact = contactName },
        onGrantPermissions = {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_SMS,
                    Manifest.permission.READ_CONTACTS
                )
            )
        },
        onOpenApiKeyDialog = { showApiKeyDialog = true },
        onGenerateDossier = { senderKey, groupLogs ->
            if (!ApiKeyManager.hasApiKey(context)) {
                showApiKeyDialog = true
            } else {
                viewModel.generateDossierForContact(context, senderKey, groupLogs)
            }
        },
        modifier = modifier.fillMaxSize()
    )

    // Hard Loading Overlay & Bottom Sheet Animations
    if (hardLoadingContact != null) {
        HardDossierLoadingOverlay(
            contactName = hardLoadingContact!!,
            onLoadingComplete = {
                bottomSheetContact = hardLoadingContact
                hardLoadingContact = null
            }
        )
    }

    if (bottomSheetContact != null) {
        DossierModalBottomSheet(
            contactName = bottomSheetContact!!,
            onDismiss = { bottomSheetContact = null }
        )
    }

    if (showApiKeyDialog) {
        ApiKeyDialog(
            onDismiss = { showApiKeyDialog = false },
            onSaveKey = { key ->
                ApiKeyManager.saveApiKey(context, key)
                showApiKeyDialog = false
            }
        )
    }

    if (showJsonImportDialog) {
        ImportJsonDialog(
            onDismiss = { showJsonImportDialog = false },
            onImportJson = { jsonString ->
                viewModel.importJsonConversation(context, jsonString) { result ->
                    val message = result.getOrElse { it.localizedMessage ?: "Failed to import JSON" }
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
                showJsonImportDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenContent(
    notifications: List<CapturedNotification>,
    isPermissionGranted: Boolean,
    dossierState: Map<String, String> = emptyMap(),
    isLoadingDossier: Map<String, Boolean> = emptyMap(),
    dossierError: Map<String, String> = emptyMap(),
    onClearLogs: () -> Unit,
    onLoadMockData: () -> Unit = {},
    onOpenJsonImportDialog: () -> Unit = {},
    onTriggerHardDossierLoading: (String) -> Unit = {},
    onGrantPermissions: () -> Unit = {},
    onOpenApiKeyDialog: () -> Unit = {},
    onGenerateDossier: (String, List<CapturedNotification>) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    val hasKey = if (isPreview) true else ApiKeyManager.hasApiKey(context)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Notifications,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Crucible Intelligence Live Logs",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Targets: ${MainViewModel.TARGET_POC_NUMBERS.joinToString(", ")}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                },
                actions = {
                    // BYOK Gemini API Key button
                    IconButton(onClick = onOpenApiKeyDialog) {
                        Icon(
                            imageVector = Icons.Rounded.Key,
                            contentDescription = "Gemini API Key (BYOK)",
                            tint = if (hasKey) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }

                    // Import JSON Conversation button
                    IconButton(onClick = onOpenJsonImportDialog) {
                        Icon(
                            imageVector = Icons.Rounded.Code,
                            contentDescription = "Import JSON Conversation",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Debug: Load Mock Data button
                    IconButton(onClick = onLoadMockData) {
                        Icon(
                            imageVector = Icons.Rounded.BugReport,
                            contentDescription = "Load Mock Data",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (notifications.isNotEmpty()) {
                        IconButton(onClick = onClearLogs) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = "Clear Logs",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Permission Banner
            AnimatedVisibility(
                visible = !isPermissionGranted,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Notification & SMS Permissions Required",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Crucible Intelligence needs notification access and SMS read permission to automatically fetch historical SMS and capture live messages.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                PermissionUtils.openNotificationListenerSettings(context)
                                onGrantPermissions()
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(text = "Grant Access", fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Notification List / Grouped Table View
            if (notifications.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(28.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Notifications,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "No logs captured yet for targets (${MainViewModel.TARGET_POC_NUMBERS.joinToString(", ")})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Grant permissions above to automatically fetch historical SMS and capture live messages in context.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                GroupedNotificationSection(
                    notifications = notifications,
                    dossierState = dossierState,
                    isLoadingDossier = isLoadingDossier,
                    dossierError = dossierError,
                    onGenerateDossier = onGenerateDossier,
                    onTriggerHardDossierLoading = onTriggerHardDossierLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

fun getNormalizedSenderKey(notification: CapturedNotification, context: Context): String {
    val rawTitle = notification.title ?: ""
    if (rawTitle == "Me") return MainViewModel.TARGET_POC_NUMBERS.first()

    MainViewModel.TARGET_POC_NUMBERS.forEach { targetNumber ->
        val contactName = ContactUtils.getContactName(context, targetNumber)
        val targetDigits = targetNumber.replace(Regex("[^0-9]"), "").takeLast(9)
        val titleDigits = rawTitle.replace(Regex("[^0-9]"), "")

        val matchesDigits = targetDigits.isNotEmpty() && titleDigits.endsWith(targetDigits)
        val matchesRaw = rawTitle.contains(targetNumber)
        val matchesName = contactName.isNotBlank() && (
            rawTitle.contains(contactName, ignoreCase = true) || contactName.contains(rawTitle, ignoreCase = true)
        )

        if (matchesDigits || matchesRaw || matchesName) {
            return targetNumber
        }
    }
    return rawTitle
}

@Composable
fun GroupedNotificationSection(
    notifications: List<CapturedNotification>,
    dossierState: Map<String, String> = emptyMap(),
    isLoadingDossier: Map<String, Boolean> = emptyMap(),
    dossierError: Map<String, String> = emptyMap(),
    onGenerateDossier: (String, List<CapturedNotification>) -> Unit = { _, _ -> },
    onTriggerHardDossierLoading: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val grouped = remember(notifications) {
        notifications.groupBy { getNormalizedSenderKey(it, context) }
    }

    val summaryExpandedMap = remember { mutableStateMapOf<String, Boolean>() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        grouped.forEach { (senderKey, groupLogs) ->
            val displayName = remember(senderKey) {
                ContactUtils.getContactName(context, senderKey)
            }
            val isSummaryExpanded = summaryExpandedMap[senderKey] ?: false

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Phone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Context per # $displayName",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = { onTriggerHardDossierLoading(displayName) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Assessment,
                                    contentDescription = "Simulate Hard Dossier Flow",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "${groupLogs.size} logs",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = {
                                    val newExpanded = !isSummaryExpanded
                                    summaryExpandedMap[senderKey] = newExpanded
                                    if (newExpanded && dossierState[senderKey] == null) {
                                        onGenerateDossier(senderKey, groupLogs)
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                ),
                                contentPadding = PaddingValues(
                                    horizontal = 10.dp,
                                    vertical = 6.dp
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Star,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isSummaryExpanded) "Hide Summary" else "✨ Summary",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Expandable AI Summary & Dossier Card
                    AnimatedVisibility(visible = isSummaryExpanded) {
                        AiSummaryCard(
                            displayName = displayName,
                            senderKey = senderKey,
                            groupLogs = groupLogs,
                            dossierText = dossierState[senderKey],
                            isLoading = isLoadingDossier[senderKey] == true,
                            errorMessage = dossierError[senderKey],
                            onRetry = { onGenerateDossier(senderKey, groupLogs) }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(8.dp))

                    NotificationTable(
                        notifications = groupLogs,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun AiSummaryCard(
    displayName: String,
    senderKey: String,
    groupLogs: List<CapturedNotification>,
    dossierText: String?,
    isLoading: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Tactical Coaching Dossier (Gemini AI)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }

                if (!isLoading && dossierText != null) {
                    IconButton(
                        onClick = onRetry,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Regenerate Dossier",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Generating Tactical Coaching Dossier from Gemini AI...",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else if (errorMessage != null) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary
                        )
                    ) {
                        Text("Retry Dossier Generation")
                    }
                }
            } else if (dossierText != null) {
                SelectionContainer {
                    Text(
                        text = dossierText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            } else {
                Text(
                    text = "Tap '✨ Summary' above to generate a tactical coaching dossier for this conversation using Gemini AI.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}

@Composable
fun ApiKeyDialog(
    onDismiss: () -> Unit,
    onSaveKey: (String) -> Unit
) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    val initialKey = if (isPreview) "" else ApiKeyManager.getApiKey(context)
    var keyText by remember { mutableStateOf(initialKey) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = @Composable {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Key,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Gemini API Key (BYOK)")
            }
        },
        text = @Composable {
            Column {
                Text(
                    text = "Supply your own Gemini API Key (Bring Your Own Key) to generate AI Coaching Dossiers offloading compute costs.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = keyText,
                    onValueChange = { newValue: String -> keyText = newValue },
                    label = { Text("Gemini API Key") },
                    placeholder = { Text("AIzaSy...") },
                    singleLine = true,
                    isError = keyText.isNotBlank() && !keyText.startsWith("AIzaSy"),
                    modifier = Modifier.fillMaxWidth()
                )
                if (keyText.isNotBlank() && !keyText.startsWith("AIzaSy")) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "⚠️ Warning: Google AI Studio keys start with 'AIzaSy...'. Ensure you copied the API Key from aistudio.google.com/app/apikey (not an OAuth token).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Get a free key at aistudio.google.com",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = @Composable {
            Button(
                onClick = { onSaveKey(keyText) }
            ) {
                Text("Save Key")
            }
        },
        dismissButton = @Composable {
            Row {
                if (ApiKeyManager.hasApiKey(context)) {
                    TextButton(
                        onClick = {
                            ApiKeyManager.clearApiKey(context)
                            keyText = ""
                            onDismiss()
                        }
                    ) {
                        Text("Clear Key", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
fun ImportJsonDialog(
    onDismiss: () -> Unit,
    onImportJson: (String) -> Unit
) {
    val context = LocalContext.current
    var jsonText by remember { mutableStateOf("") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val content = inputStream?.bufferedReader()?.use { reader -> reader.readText() } ?: ""
                if (content.isNotBlank()) {
                    jsonText = content
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = @Composable {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Code,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Import JSON Conversation")
            }
        },
        text = @Composable {
            Column {
                Text(
                    text = "Paste your JSON conversation below or pick a .json file from storage:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { filePickerLauncher.launch("*/*") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("📁 Select .json File from Storage")
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = jsonText,
                    onValueChange = { newValue: String -> jsonText = newValue },
                    label = { Text("JSON Conversation Payload") },
                    placeholder = { Text("{\n  \"phoneNumber\": \"+15550198888\",\n  \"messages\": [...]\n}") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )
            }
        },
        confirmButton = @Composable {
            Button(
                onClick = {
                    if (jsonText.isNotBlank()) {
                        onImportJson(jsonText)
                    }
                }
            ) {
                Text("Import Conversation")
            }
        },
        dismissButton = @Composable {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun NotificationTable(
    notifications: List<CapturedNotification>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val horizontalScrollState = rememberScrollState()
    val encryptedMap = remember { mutableStateMapOf<Long, Boolean>() }
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(horizontalScrollState)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TableCell(text = "ID", width = 60.dp, isHeader = true)
                    TableCell(text = "App Package", width = 140.dp, isHeader = true)
                    TableCell(text = "Sender", width = 130.dp, isHeader = true)
                    TableCell(text = "Content", width = 260.dp, isHeader = true)
                    TableCell(text = "Timestamp", width = 150.dp, isHeader = true)
                    TableCell(text = "Encryption", width = 90.dp, isHeader = true)
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Rows
                notifications.forEachIndexed { index, notification ->
                    val isEncrypted = encryptedMap[notification.id] ?: false
                    val formattedTime = dateFormat.format(Date(notification.timestamp))
                    val displayText = if (isEncrypted) {
                        simulateEncryption(notification.text)
                    } else {
                        notification.text ?: "(No Content)"
                    }

                    val senderLabel = when {
                        notification.packageName == "historical.sms.sync.sent" -> "Me (Sent)"
                        notification.title == "Me" -> "Me (Sent)"
                        else -> ContactUtils.getContactName(context, notification.title ?: "Contact")
                    }

                    Row(
                        modifier = Modifier
                            .background(
                                if (index % 2 == 0) MaterialTheme.colorScheme.surface
                                else MaterialTheme.colorScheme.surfaceContainerLow
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TableCell(text = notification.id.toString(), width = 60.dp)
                        TableCell(text = notification.packageName, width = 140.dp)
                        TableCell(text = senderLabel, width = 130.dp)
                        TableCell(
                            text = displayText,
                            width = 260.dp,
                            isEncrypted = isEncrypted
                        )
                        TableCell(text = formattedTime, width = 150.dp)

                        Box(
                            modifier = Modifier.width(90.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(
                                onClick = {
                                    encryptedMap[notification.id] = !isEncrypted
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (isEncrypted) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                                    contentDescription = if (isEncrypted) "Decrypt" else "Encrypt",
                                    tint = if (isEncrypted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    if (index < notifications.size - 1) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            thickness = 0.5.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TableCell(
    text: String,
    width: Dp,
    isHeader: Boolean = false,
    isEncrypted: Boolean = false
) {
    Text(
        text = text,
        modifier = Modifier.width(width),
        style = if (isHeader) {
            MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
        } else {
            MaterialTheme.typography.bodyMedium
        },
        color = if (isHeader) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else if (isEncrypted) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        maxLines = 3,
        overflow = TextOverflow.Ellipsis
    )
}

fun simulateEncryption(text: String?): String {
    if (text.isNullOrEmpty()) return "U2FsdGVkX1+..."
    return try {
        val encoded = Base64.encodeToString(
            text.toByteArray(Charsets.UTF_8),
            Base64.NO_WRAP
        )
        "U2FsdGVkX1+$encoded"
    } catch (e: Throwable) {
        val fallbackHex = text.toCharArray().joinToString("") { "%02x".format(it.code.toByte()) }
        "U2FsdGVkX1+$fallbackHex"
    }
}

@Preview(showBackground = true, device = "id:pixel_6")
@Composable
fun MainScreenEmptyPreview() {
    CrucibleIntelligenceTheme {
        MainScreenContent(
            notifications = emptyList(),
            isPermissionGranted = false,
            onClearLogs = {}
        )
    }
}

@Preview(showBackground = true, device = "id:pixel_6")
@Composable
fun MainScreenPopulatedPreview() {
    val sampleNotifications = listOf(
        CapturedNotification(
            id = 1L,
            packageName = "historical.sms.sync",
            title = "09634255141",
            text = "Hey Maah, are we meeting today?",
            timestamp = System.currentTimeMillis() - 7200000L
        ),
        CapturedNotification(
            id = 2L,
            packageName = "historical.sms.sync.sent",
            title = "09634255141",
            text = "Yes! I will be there at 2 PM.",
            timestamp = System.currentTimeMillis() - 5400000L
        ),
        CapturedNotification(
            id = 3L,
            packageName = "com.google.android.apps.messaging",
            title = "09634255141",
            text = "Great! See you at the coffee shop.",
            timestamp = System.currentTimeMillis() - 3600000L
        ),
        CapturedNotification(
            id = 4L,
            packageName = "historical.sms.sync.sent",
            title = "09634255141",
            text = "Sounds good!",
            timestamp = System.currentTimeMillis() - 1800000L
        )
    )
    CrucibleIntelligenceTheme {
        MainScreenContent(
            notifications = sampleNotifications,
            isPermissionGranted = true,
            onClearLogs = {}
        )
    }
}
