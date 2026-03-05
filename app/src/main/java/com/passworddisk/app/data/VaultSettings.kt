package com.passworddisk.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vault_settings")
data class VaultSettings(
    @PrimaryKey
    val id: Int = 1,
    val masterPasswordHash: String,
    val salt: String,
    val initialized: Boolean = false
)