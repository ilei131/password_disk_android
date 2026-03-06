package com.passworddisk.app.data

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.passworddisk.app.crypto.CryptoManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID

// 辅助数据类，用于解析 JSON 数据
private data class CloudBackup(
    val master_password_hash: String,
    val salt: String,
    val passwords: List<CloudPassword>,
    val categories: List<CloudCategory>
)

private data class CloudPassword(
    val id: String,
    val title: String,
    val username: String,
    val password: String,
    val url: String,
    val notes: String,
    val category: String,
    @SerializedName("created_at")
    val createdAt: Long,
    @SerializedName("updated_at")
    val updatedAt: Long
)

private data class CloudCategory(
    val id: String,
    val name: String,
    val icon: String
)

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
            password = encryptedPassword,
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
            password = encryptedPassword,
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
        // 先删除分类下的所有密码
        passwordDao.deletePasswordsByCategory(category.id)
        // 再删除分类
        categoryDao.deleteCategory(category)
    }

    suspend fun deleteCategoryById(id: String) {
        // 先删除分类下的所有密码
        passwordDao.deletePasswordsByCategory(id)
        // 再删除分类
        categoryDao.deleteCategoryById(id)
    }

    suspend fun getVaultBackup(): String? {
        val settings = vaultSettingsDao.getSettings() ?: return null
        val passwords = passwordDao.getAllPasswords().first() // Collect the first emission
        val categories = categoryDao.getAllCategories().first() // Collect the first emission

        // Create category ID to name mapping
        val categoryMap = categories.associateBy { it.id }

        // Sort passwords by created_at (ascending) to match Rust behavior
        val sortedPasswords = passwords.sortedBy { it.createdAt }

        // Sort categories by id (ascending) to match Rust behavior
        val sortedCategories = categories.sortedBy { it.id }

        val passwordsJson = sortedPasswords.joinToString(",") {
            val categoryName = categoryMap[it.categoryId]?.name ?: "所有"
            String.format(
                "{\"id\":\"%s\",\"title\":\"%s\",\"username\":\"%s\",\"password\":\"%s\",\"url\":\"%s\",\"notes\":\"%s\",\"category\":\"%s\",\"created_at\":%d,\"updated_at\":%d}",
                it.id, it.title, it.username, it.password, it.url, it.notes, categoryName, it.createdAt, it.updatedAt
            )
        }

        val categoriesJson = sortedCategories.joinToString(",") {
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

            // Parse JSON data
            val gson = Gson()
            val backup = gson.fromJson(backupJson, CloudBackup::class.java)

            // Log backup data
            println("Backup data received: passwords count = ${backup.passwords.size}, categories count = ${backup.categories.size}")
//            backup.passwords.forEachIndexed { index, password ->
//                println("Password $index: id=${password.id}, title=${password.title}, username=${password.username}, password=${password.password?.take(10)}..., category=${password.category}")
//            }

            // Restore categories
            val categoryMap = mutableMapOf<String, String>() // category name to id mapping
            backup.categories.forEach { cloudCategory ->
                val category = Category(
                    id = cloudCategory.id,
                    name = cloudCategory.name,
                    icon = cloudCategory.icon
                )
                categoryDao.insertCategory(category)
                categoryMap[cloudCategory.name] = cloudCategory.id
                println("Added category: ${cloudCategory.name} (${cloudCategory.id})")
            }

            // Restore passwords
            backup.passwords.forEach { cloudPassword ->
                // Get category ID from category name
                val categoryId = categoryMap[cloudPassword.category] ?: "1" // Default to "所有"
                println("Processing password: ${cloudPassword.title}, category=${cloudPassword.category}, categoryId=$categoryId")
                
                val passwordItem = PasswordItem(
                    id = cloudPassword.id,
                    title = cloudPassword.title,
                    username = cloudPassword.username,
                    password = cloudPassword.password, // 直接使用返回的加密密码
                    url = cloudPassword.url,
                    notes = cloudPassword.notes,
                    categoryId = categoryId,
                    createdAt = cloudPassword.createdAt,
                    updatedAt = cloudPassword.updatedAt
                )
                passwordDao.insertPassword(passwordItem)
                println("Added password: ${passwordItem.title}, encryptedPassword=${passwordItem.password?.take(10)}...")
            }

            // Restore settings
            val settings = VaultSettings(
                masterPasswordHash = backup.master_password_hash,
                salt = backup.salt,
                initialized = true
            )
            vaultSettingsDao.insertSettings(settings)

            // Update master key and salt
            this.salt = backup.salt
            // Note: We don't have the master password here, so masterKey remains null
            // The user will need to login again to get the master key

            println("Restore completed successfully")
            true
        } catch (e: Exception) {
            e.printStackTrace()
            println("Restore failed: ${e.message}")
            false
        }
    }
}