package com.example.notifyzerpocphase1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.notifyzerpocphase1.repository.NotificationRepository
import com.example.notifyzerpocphase1.ui.MainScreen
import com.example.notifyzerpocphase1.ui.theme.NotifyzerPOCPhase1Theme
import com.example.notifyzerpocphase1.viewmodel.MainViewModel
import com.example.notifyzerpocphase1.viewmodel.HistoricalSmsViewModel
import com.example.notifyzerpocphase1.viewmodel.SyncSmsViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private val historicalSmsViewModel: HistoricalSmsViewModel by viewModels()
    private val syncSmsViewModel: SyncSmsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationRepository.initialize(applicationContext)
        enableEdgeToEdge()
        setContent {
            NotifyzerPOCPhase1Theme {
                MainScreen(
                    viewModel = viewModel, 
                    historicalSmsViewModel = historicalSmsViewModel,
                    syncSmsViewModel = syncSmsViewModel
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkPermission(this)
    }
}
