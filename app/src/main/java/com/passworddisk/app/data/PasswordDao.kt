package com.passworddisk.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PasswordDao {
    @Query("SELECT * FROM password_items ORDER BY updatedAt DESC")
    fun getAllPasswords(): Flow<List<PasswordItem>>

    @Query("SELECT * FROM password_items WHERE categoryId = :categoryId ORDER BY updatedAt DESC")
    fun getPasswordsByCategory(categoryId: String): Flow<List<PasswordItem>>

    @Query("SELECT * FROM password_items WHERE id = :id")
    suspend fun getPasswordById(id: String): PasswordItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPassword(password: PasswordItem)

    @Update
    suspend fun updatePassword(password: PasswordItem)

    @Delete
    suspend fun deletePassword(password: PasswordItem)

    @Query("DELETE FROM password_items WHERE id = :id")
    suspend fun deletePasswordById(id: String)

    @Query("SELECT * FROM password_items WHERE title LIKE '%' || :query || '%' OR username LIKE '%' || :query || '%' OR url LIKE '%' || :query || '%'")
    fun searchPasswords(query: String): Flow<List<PasswordItem>>

    @Query("DELETE FROM password_items")
    suspend fun deleteAllPasswords()

    @Query("DELETE FROM password_items WHERE categoryId = :categoryId")
    suspend fun deletePasswordsByCategory(categoryId: String)
}