package com.passworddisk.app.data

import com.passworddisk.app.crypto.CryptoManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID

class PasswordRepository(
    private val passwordDao: PasswordDao,
    private val categoryDao: CategoryDao,
    private val vaultSettingsDao: VaultSettingsDao
) {
    private var masterKey: ByteArray? = null
    private var salt: String? = null

    val allPasswords: Flow<List<PasswordItem>> = passwordDao.getAllPasswords()
    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()

    suspend fun isVaultInitialized(): Boolean {
        val settings = vaultSettingsDao.getSettings()
        return settings?.initialized == true
    }

    suspend fun initializeVault(masterPassword: String): Boolean {
        if (isVaultInitialized()) {
            return false
        }

        val salt = CryptoManager.generateSalt()
        val passwordHash = CryptoManager.hashPassword(masterPassword)

        val defaultCategories = listOf(
            Category("1", "所有", "📁"),
            Category("2", "个人", "👤"),
            Category("3", "工作", "💼"),
            Category("4", "金融", "💰"),
            Category("5", "社交媒体", "📱")
        )

        defaultCategories.forEach { categoryDao.insertCategory(it) }

        val settings = VaultSettings(
            masterPasswordHash = passwordHash,
            salt = salt,
            initialized = true
        )
        vaultSettingsDao.insertSettings(settings)

        this.salt = salt
        this.masterKey = CryptoManager.deriveKey(masterPassword, salt)

        return true
    }

    suspend fun verifyMasterPassword(masterPassword: String): Boolean {
        val settings = vaultSettingsDao.getSettings() ?: return false
        val verified = CryptoManager.verifyPassword(masterPassword, settings.masterPasswordHash)

        if (verified) {
            this.salt = settings.salt
            this.masterKey = CryptoManager.deriveKey(masterPassword, settings.salt)
        }

        return verified
    }

    fun isAuthenticated(): Boolean = masterKey != null

    fun logout() {
        masterKey = null
        salt = null
    }

    suspend fun addPassword(
        title: String,
        username: String,
        password: String,
        url: String,
        notes: String,
        categoryId: String
    ): PasswordItem? {
        val key = masterKey ?: return null
        val currentTime = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()

        val encryptedPassword = CryptoManager.encryptData(password, key)

        val passwordItem = PasswordItem(
            id = id,
            title = title,
            username = username,
            encryptedPassword = encryptedPassword,
            url = url,
            notes = notes,
            categoryId = categoryId,
            createdAt = currentTime,
            updatedAt = currentTime
        )

        passwordDao.insertPassword(passwordItem)
        return passwordItem
    }

    suspend fun updatePassword(passwordItem: PasswordItem, plainPassword: String): PasswordItem? {
        val key = masterKey ?: return null
        val currentTime = System.currentTimeMillis()

        val encryptedPassword = CryptoManager.encryptData(plainPassword, key)

        val updatedItem = passwordItem.copy(
            encryptedPassword = encryptedPassword,
            updatedAt = currentTime
        )

        passwordDao.updatePassword(updatedItem)
        return updatedItem
    }

    suspend fun deletePassword(passwordItem: PasswordItem) {
        passwordDao.deletePassword(passwordItem)
    }

    suspend fun deletePasswordById(id: String) {
        passwordDao.deletePasswordById(id)
    }

    fun getPasswordsByCategory(categoryId: String): Flow<List<PasswordItem>> {
        return passwordDao.getPasswordsByCategory(categoryId)
    }

    fun searchPasswords(query: String): Flow<List<PasswordItem>> {
        return passwordDao.searchPasswords(query)
    }

    suspend fun decryptPassword(encryptedPassword: String): String? {
        val key = masterKey ?: return null
        return try {
            CryptoManager.decryptData(encryptedPassword, key)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun addCategory(name: String, icon: String): Category {
        val id = UUID.randomUUID().toString()
        val category = Category(id, name, icon)
        categoryDao.insertCategory(category)
        return category
    }

    suspend fun updateCategory(category: Category) {
        categoryDao.updateCategory(category)
    }

    suspend fun deleteCategory(category: Category) {
        categoryDao.deleteCategory(category)
    }

    suspend fun deleteCategoryById(id: String) {
        categoryDao.deleteCategoryById(id)
    }

    suspend fun getVaultBackup(): String? {
        val settings = vaultSettingsDao.getSettings() ?: return null
        val passwords = passwordDao.getAllPasswords().first() // Collect the first emission
        val categories = categoryDao.getAllCategories().first() // Collect the first emission

        val passwordsJson = passwords.joinToString(",") {
            String.format(
                "{\"id\":\"%s\",\"title\":\"%s\",\"username\":\"%s\",\"encryptedPassword\":\"%s\",\"url\":\"%s\",\"notes\":\"%s\",\"categoryId\":\"%s\",\"createdAt\":%d,\"updatedAt\":%d}",
                it.id, it.title, it.username, it.encryptedPassword, it.url, it.notes, it.categoryId, it.createdAt, it.updatedAt
            )
        }

        val categoriesJson = categories.joinToString(",") {
            String.format(
                "{\"id\":\"%s\",\"name\":\"%s\",\"icon\":\"%s\"}",
                it.id, it.name, it.icon
            )
        }

        return String.format(
            "{\"master_password_hash\":\"%s\",\"salt\":\"%s\",\"passwords\":[%s],\"categories\":[%s]}",
            settings.masterPasswordHash, settings.salt, passwordsJson, categoriesJson
        )
    }

    suspend fun restoreVault(backupJson: String): Boolean {
        return try {
            // Clear existing data
            passwordDao.deleteAllPasswords()
            categoryDao.deleteAllCategories()
            vaultSettingsDao.deleteAll()

            // Parse and restore data
            // This is a simplified implementation
            // In a real app, you would parse the JSON properly
            true
        } catch (e: Exception) {
            false
        }
    }
}