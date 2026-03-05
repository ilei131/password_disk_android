package com.passworddisk.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "password_items")
data class PasswordItem(
    @PrimaryKey
    val id: String,
    val title: String,
    val username: String,
    val password: String,
    val url: String,
    val notes: String,
    val categoryId: String,
    val createdAt: Long,
    val updatedAt: Long
)