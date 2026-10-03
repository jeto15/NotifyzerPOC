package com.example.notifyzerpocphase1

import com.example.notifyzerpocphase1.model.CapturedNotification
import com.example.notifyzerpocphase1.repository.NotificationRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class NotificationRepositoryTest {

    @Before
    fun setUp() {
        NotificationRepository.clearNotifications()
    }

    @Test
    fun testAddNotification() = runTest {
        val notification = CapturedNotification(
            packageName = "com.example.test",
            title = "Test Title",
            text = "Test Text",
            timestamp = 123456789L
        )

        NotificationRepository.addNotification(notification)

        val notifications = NotificationRepository.notifications.first()
        assertEquals(1, notifications.size)
        assertEquals("com.example.test", notifications[0].packageName)
        assertEquals("Test Title", notifications[0].title)
        assertEquals("Test Text", notifications[0].text)
        assertEquals(123456789L, notifications[0].timestamp)
    }

    @Test
    fun testAddMultipleNotificationsRetained() = runTest {
        val notification1 = CapturedNotification(
            packageName = "com.example.test",
            title = "Message 1",
            text = "Hello",
            timestamp = 1000L
        )
        val notification2 = CapturedNotification(
            packageName = "com.example.test",
            title = "Message 1",
            text = "World",
            timestamp = 2000L
        )

        NotificationRepository.addNotification(notification1)
        NotificationRepository.addNotification(notification2)

        val notifications = NotificationRepository.notifications.first()
        assertEquals(2, notifications.size)
    }

    @Test
    fun testClearNotifications() = runTest {
        val notification = CapturedNotification(
            packageName = "com.example.test",
            title = "Test Title 2",
            text = "Test Text 2",
            timestamp = 123456789L
        )

        NotificationRepository.addNotification(notification)
        assertEquals(1, NotificationRepository.notifications.first().size)

        NotificationRepository.clearNotifications()
        assertEquals(0, NotificationRepository.notifications.first().size)
    }
}
