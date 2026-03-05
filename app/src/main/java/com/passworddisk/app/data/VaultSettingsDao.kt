package com.passworddisk.app.data

import androidx.room.*

@Dao
interface VaultSettingsDao {
    @Query("SELECT * FROM vault_settings WHERE id = 1")
    suspend fun getSettings(): VaultSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: VaultSettings)

    @Update
    suspend fun updateSettings(settings: VaultSettings)

    @Query("DELETE FROM vault_settings")
    suspend fun deleteAll()
}