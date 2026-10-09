package com.example.notifyzerpocphase1.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "entity_profiles")
data class EntityProfile(
    @PrimaryKey
    val phoneNumber: String, // Maps to NotificationEntity.title
    val identityRole: String,
    val history: String,
    val currentDynamic: String,
    val tacticalObjective: String
)
