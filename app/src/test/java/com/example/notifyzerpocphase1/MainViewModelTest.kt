package com.example.notifyzerpocphase1

import com.example.notifyzerpocphase1.model.CapturedNotification
import com.example.notifyzerpocphase1.repository.NotificationRepository
import com.example.notifyzerpocphase1.viewmodel.MainViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class MainViewModelTest {

    private lateinit var viewModel: MainViewModel

    @Before
    fun setUp() {
        NotificationRepository.clearNotifications()
        viewModel = MainViewModel()
    }

    @Test
    fun testClearLogs() = runTest {
        val notification = CapturedNotification(
            packageName = "com.test",
            title = "Title",
            text = "Text",
            timestamp = 1000L
        )
        NotificationRepository.addNotification(notification)
        assertEquals(1, NotificationRepository.notifications.first().size)

        viewModel.clearLogs()
        assertEquals(0, NotificationRepository.notifications.first().size)
    }
}
