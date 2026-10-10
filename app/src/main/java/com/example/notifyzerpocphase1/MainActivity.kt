package com.example.notifyzerpocphase1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.notifyzerpocphase1.repository.NotificationRepository
import com.example.notifyzerpocphase1.ui.MainScreen
import com.example.notifyzerpocphase1.ui.theme.CrucibleIntelligenceTheme
import com.example.notifyzerpocphase1.viewmodel.ChatViewModel
import com.example.notifyzerpocphase1.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private val chatViewModel: ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationRepository.initialize(applicationContext)
        enableEdgeToEdge()
        setContent {
            CrucibleIntelligenceTheme {
                MainScreen(mainViewModel = mainViewModel, chatViewModel = chatViewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mainViewModel.checkPermission(this)
    }
}
