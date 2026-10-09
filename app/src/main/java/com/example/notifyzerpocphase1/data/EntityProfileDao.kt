package com.example.notifyzerpocphase1.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface EntityProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: EntityProfile)

    @Query("SELECT * FROM entity_profiles WHERE phoneNumber = :phoneNumber")
    suspend fun getProfile(phoneNumber: String): EntityProfile?

    @Transaction
    @Query("SELECT * FROM entity_profiles WHERE phoneNumber = :phoneNumber")
    fun getDossierData(phoneNumber: String): Flow<DossierData?>
    
    // For Verification/Debugging
    @Transaction
    @Query("SELECT * FROM entity_profiles")
    suspend fun getAllDossierData(): List<DossierData>
}
