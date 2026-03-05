package com.passworddisk.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.passworddisk.app.PasswordDiskApplication
import com.passworddisk.app.cloud.CloudSyncManager
import com.passworddisk.app.crypto.CryptoManager
import com.passworddisk.app.crypto.TotpGenerator
import com.passworddisk.app.data.Category
import com.passworddisk.app.data.PasswordItem
import com.passworddisk.app.data.PasswordRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class PasswordViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: PasswordRepository

    val allPasswords: Flow<List<PasswordItem>>
    val allCategories: Flow<List<Category>>

    private val _isAuthenticated = MutableLiveData<Boolean>()
    val isAuthenticated: LiveData<Boolean> = _isAuthenticated

    private val _isVaultInitialized = MutableLiveData<Boolean>()
    val isVaultInitialized: LiveData<Boolean> = _isVaultInitialized

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _successMessage = MutableLiveData<String?>()
    val successMessage: LiveData<String?> = _successMessage

    private val _generatedPassword = MutableLiveData<String?>()
    val generatedPassword: LiveData<String?> = _generatedPassword

    private val _totpCode = MutableLiveData<String?>()
    val totpCode: LiveData<String?> = _totpCode

    init {
        val database = (application as PasswordDiskApplication).database
        repository = PasswordRepository(
            database.passwordDao(),
            database.categoryDao(),
            database.vaultSettingsDao()
        )
        allPasswords = repository.allPasswords
        allCategories = repository.allCategories
        checkVaultInitialization()
    }

    private fun checkVaultInitialization() {
        viewModelScope.launch {
            _isVaultInitialized.value = repository.isVaultInitialized()
        }
    }

    fun initializeVault(masterPassword: String, confirmPassword: String) {
        if (masterPassword != confirmPassword) {
            _errorMessage.value = "Passwords do not match"
            return
        }
        if (masterPassword.length < 6) {
            _errorMessage.value = "Password must be at least 6 characters"
            return
        }

        viewModelScope.launch {
            val success = repository.initializeVault(masterPassword)
            if (success) {
                _isVaultInitialized.value = true
                _isAuthenticated.value = true
                _successMessage.value = "Vault initialized successfully"
            } else {
                _errorMessage.value = "Vault already exists"
            }
        }
    }

    fun login(masterPassword: String) {
        viewModelScope.launch {
            val success = repository.verifyMasterPassword(masterPassword)
            if (success) {
                _isAuthenticated.value = true
                _errorMessage.value = null
            } else {
                _isAuthenticated.value = false
                _errorMessage.value = "Invalid password"
            }
        }
    }

    fun logout() {
        repository.logout()
        _isAuthenticated.value = false
    }

    fun addPassword(
        title: String,
        username: String,
        password: String,
        url: String,
        notes: String,
        categoryId: String
    ) {
        viewModelScope.launch {
            val result = repository.addPassword(title, username, password, url, notes, categoryId)
            if (result != null) {
                _successMessage.value = "Password added successfully"
            } else {
                _errorMessage.value = "Failed to add password"
            }
        }
    }

    fun updatePassword(passwordItem: PasswordItem, plainPassword: String) {
        viewModelScope.launch {
            val result = repository.updatePassword(passwordItem, plainPassword)
            if (result != null) {
                _successMessage.value = "Password updated successfully"
            } else {
                _errorMessage.value = "Failed to update password"
            }
        }
    }

    fun deletePassword(passwordItem: PasswordItem) {
        viewModelScope.launch {
            repository.deletePassword(passwordItem)
            _successMessage.value = "Password deleted successfully"
        }
    }

    fun deletePasswordById(id: String) {
        viewModelScope.launch {
            repository.deletePasswordById(id)
            _successMessage.value = "Password deleted successfully"
        }
    }

    suspend fun decryptPassword(encryptedPassword: String): String? {
        return repository.decryptPassword(encryptedPassword)
    }

    fun addCategory(name: String, icon: String) {
        viewModelScope.launch {
            repository.addCategory(name, icon)
            _successMessage.value = "Category added successfully"
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            repository.updateCategory(category)
            _successMessage.value = "Category updated successfully"
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            repository.deleteCategory(category)
            _successMessage.value = "Category deleted successfully"
        }
    }

    fun deleteCategoryById(id: String) {
        viewModelScope.launch {
            repository.deleteCategoryById(id)
            _successMessage.value = "Category deleted successfully"
        }
    }

    fun generatePassword(
        length: Int,
        includeUppercase: Boolean,
        includeLowercase: Boolean,
        includeNumbers: Boolean,
        includeSymbols: Boolean
    ) {
        viewModelScope.launch {
            try {
                val password = CryptoManager.generatePassword(
                    length,
                    includeUppercase,
                    includeLowercase,
                    includeNumbers,
                    includeSymbols
                )
                _generatedPassword.value = password
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun generateTotpCode(secret: String) {
        viewModelScope.launch {
            try {
                val code = TotpGenerator.generateCode(secret)
                _totpCode.value = code
            } catch (e: Exception) {
                _errorMessage.value = "Failed to generate TOTP code"
            }
        }
    }

    fun registerCloud(username: String, password: String) {
        viewModelScope.launch {
            val result = CloudSyncManager.register(username, password)
            result.onSuccess {
                _successMessage.value = "Registered successfully"
            }.onFailure {
                _errorMessage.value = it.message
            }
        }
    }

    fun loginCloud(username: String, password: String, backup: String? = null) {
        viewModelScope.launch {
            val result = CloudSyncManager.login(username, password, backup)
            result.onSuccess {
                _successMessage.value = "Logged in successfully"
            }.onFailure {
                _errorMessage.value = it.message
            }
        }
    }

    fun syncCloud(username: String, password: String) {
        viewModelScope.launch {
            val result = CloudSyncManager.sync(username, password)
            result.onSuccess { response -> 
                // 处理返回的密码数据
                if (response.backup != null) {
                    // 使用返回的备份数据恢复密码库
                    val restoreSuccess = repository.restoreVault(response.backup)
                    if (restoreSuccess) {
                        // 同步成功后，强制用户重新登录以获取新的 masterKey
                        _isAuthenticated.value = false
                        _successMessage.value = "Synced successfully. Please login again to access your passwords."
                    } else {
                        _errorMessage.value = "Failed to restore vault from sync"
                    }
                } else {
                    _successMessage.value = "Synced successfully"
                }
            }.onFailure { 
                _errorMessage.value = it.message
            }
        }
    }

    fun backupToCloud(username: String, password: String) {
        viewModelScope.launch {
            try {
                // 获取本地备份数据
                val backup = repository.getVaultBackup()
                if (backup != null) {
                    // 使用 login 接口上传备份数据
                    val result = CloudSyncManager.login(username, password, backup)
                    result.onSuccess { 
                        _successMessage.value = "Backup to cloud successful"
                    }.onFailure { 
                        _errorMessage.value = it.message
                    }
                } else {
                    _errorMessage.value = "Failed to create backup"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to backup to cloud"
            }
        }
    }

    fun backupVault() {
        viewModelScope.launch {
            try {
                val backup = repository.getVaultBackup()
                if (backup != null) {
                    // In a real app, you would save this backup to a file or share it
                    _successMessage.value = "Backup created successfully"
                } else {
                    _errorMessage.value = "Failed to create backup"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to create backup"
            }
        }
    }

    fun restoreVault(backup: String) {
        viewModelScope.launch {
            try {
                val success = repository.restoreVault(backup)
                if (success) {
                    _successMessage.value = "Vault restored successfully"
                } else {
                    _errorMessage.value = "Failed to restore vault"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to restore vault"
            }
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }
}