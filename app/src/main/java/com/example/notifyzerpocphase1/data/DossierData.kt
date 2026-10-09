package com.example.notifyzerpocphase1.data

import androidx.room.Embedded
import androidx.room.Relation

data class DossierData(
    @Embedded val profile: EntityProfile,
    @Relation(
        parentColumn = "phoneNumber",
        entityColumn = "title" // Maps to the sender/target number stored in NotificationEntity
    )
    val messages: List<NotificationEntity>
)
