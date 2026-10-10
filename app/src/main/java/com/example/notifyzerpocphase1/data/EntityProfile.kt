package com.example.notifyzerpocphase1.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "entity_profiles")
data class EntityProfile(
    @PrimaryKey
    val phoneNumber: String, // Unique identifier for the contact
    val displayName: String?,
    val identityRole: String?,
    val history: String?,
    val currentDynamic: String?,
    val tacticalObjective: String?,
    val firstSeen: Long = System.currentTimeMillis(),
    val lastActive: Long = System.currentTimeMillis(),
    val assetLiabilityScore: Int? = null // null = Unanalyzed, 0-49 = Liability, 50-100 = Asset
)
