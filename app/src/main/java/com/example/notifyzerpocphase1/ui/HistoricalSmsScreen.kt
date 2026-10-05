package com.example.notifyzerpocphase1.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.compose.ui.tooling.preview.Preview
import com.example.notifyzerpocphase1.ui.theme.NotifyzerPOCPhase1Theme
import com.example.notifyzerpocphase1.model.HistoricalSms
import com.example.notifyzerpocphase1.util.ContactUtils
import com.example.notifyzerpocphase1.viewmodel.HistoricalSmsViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoricalSmsScreen(viewModel: HistoricalSmsViewModel) {
    val context = LocalContext.current
    val hasPermission by viewModel.hasPermission.collectAsState()
    val groupedSms by viewModel.groupedSms.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            viewModel.setPermissionGranted(isGranted)
        }
    )

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.setPermissionGranted(granted)
        if (!granted) {
            permissionLauncher.launch(Manifest.permission.READ_SMS)
        }
    }

    if (!hasPermission) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("SMS Permission is required to view historical SMS.")
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { permissionLauncher.launch(Manifest.permission.READ_SMS) }) {
                    Text("Request Permission")
                }
            }
        }
    } else if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(groupedSms.keys.toList()) { contact ->
                val displayName = remember(contact) {
                    ContactUtils.getContactName(context, contact)
                }
                ContactItem(contact = displayName, messages = groupedSms[contact] ?: emptyList())
            }
        }
    }
}

@Composable
fun ContactItem(contact: String, messages: List<HistoricalSms>) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(text = contact, style = MaterialTheme.typography.titleMedium)
            Text(text = "${messages.size} messages", style = MaterialTheme.typography.bodySmall)

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    messages.take(5).forEach { msg ->
                        SmsMessageItem(msg)
                    }
                    if (messages.size > 5) {
                        Text(
                            text = "And ${messages.size - 5} more...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SmsMessageItem(msg: HistoricalSms) {
    val formatter = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }
    val dateStr = formatter.format(Date(msg.date))
    val typeStr = if (msg.type == 1) "Received" else "Sent"

    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = typeStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            Text(text = dateStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        }
        Text(text = msg.body, style = MaterialTheme.typography.bodyMedium)
    }
}

@Preview(showBackground = true)
@Composable
fun HistoricalSmsPreview() {
    val sampleMessages = listOf(
        HistoricalSms(address = "+1234567890", body = "Hello, your verification code is 482910.", date = System.currentTimeMillis() - 3600000L, type = 1),
        HistoricalSms(address = "+1234567890", body = "Thank you!", date = System.currentTimeMillis() - 1800000L, type = 2)
    )
    NotifyzerPOCPhase1Theme {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                ContactItem(contact = "+1234567890", messages = sampleMessages)
            }
        }
    }
}
