package com.passworddisk.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.passworddisk.app.PasswordDiskApplication
import com.passworddisk.app.R
import com.passworddisk.app.cloud.CloudSyncError
import com.passworddisk.app.cloud.CloudSyncErrorException
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
            _errorMessage.value = getApplication<Application>().getString(R.string.passwords_do_not_match)
            return
        }
        if (masterPassword.length < 6) {
            _errorMessage.value = getApplication<Application>().getString(R.string.password_min_length)
            return
        }

        viewModelScope.launch {
            val success = repository.initializeVault(masterPassword)
            if (success) {
                _isVaultInitialized.value = true
                _isAuthenticated.value = true
                _successMessage.value = getApplication<Application>().getString(R.string.vault_initialized_successfully)
            } else {
                _errorMessage.value = getApplication<Application>().getString(R.string.vault_already_exists)
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
                _errorMessage.value = getApplication<Application>().getString(R.string.invalid_password)
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
                _successMessage.value = getApplication<Application>().getString(R.string.password_added_successfully)
            } else {
                _errorMessage.value = getApplication<Application>().getString(R.string.failed_to_add_password)
            }
        }
    }

    fun updatePassword(passwordItem: PasswordItem, plainPassword: String) {
        viewModelScope.launch {
            val result = repository.updatePassword(passwordItem, plainPassword)
            if (result != null) {
                _successMessage.value = getApplication<Application>().getString(R.string.password_updated_successfully)
            } else {
                _errorMessage.value = getApplication<Application>().getString(R.string.failed_to_update_password)
            }
        }
    }

    fun deletePassword(passwordItem: PasswordItem) {
        viewModelScope.launch {
            repository.deletePassword(passwordItem)
            _successMessage.value = getApplication<Application>().getString(R.string.password_deleted_successfully)
        }
    }

    fun deletePasswordById(id: String) {
        viewModelScope.launch {
            repository.deletePasswordById(id)
            _successMessage.value = getApplication<Application>().getString(R.string.password_deleted_successfully)
        }
    }

    suspend fun decryptPassword(encryptedPassword: String): String? {
        return repository.decryptPassword(encryptedPassword)
    }

    fun addCategory(name: String, icon: String) {
        viewModelScope.launch {
            repository.addCategory(name, icon)
            _successMessage.value = getApplication<Application>().getString(R.string.category_added_successfully)
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            repository.updateCategory(category)
            _successMessage.value = getApplication<Application>().getString(R.string.category_updated_successfully)
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            repository.deleteCategory(category)
            _successMessage.value = getApplication<Application>().getString(R.string.category_deleted_successfully)
        }
    }

    fun deleteCategoryById(id: String) {
        viewModelScope.launch {
            repository.deleteCategoryById(id)
            _successMessage.value = getApplication<Application>().getString(R.string.category_deleted_successfully)
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
                _errorMessage.value = getApplication<Application>().getString(R.string.failed_to_generate_totp_code)
            }
        }
    }

    fun registerCloud(username: String, password: String) {
        viewModelScope.launch {
            val result = CloudSyncManager.register(username, password)
            result.onSuccess {
                _successMessage.value = getApplication<Application>().getString(R.string.registered_successfully)
            }.onFailure {
                _errorMessage.value = getLocalizedErrorMessage(it)
            }
        }
    }

    fun loginCloud(username: String, password: String, backup: String? = null) {
        viewModelScope.launch {
            val result = CloudSyncManager.login(username, password, backup)
            result.onSuccess {
                _successMessage.value = getApplication<Application>().getString(R.string.logged_in_successfully)
            }.onFailure {
                _errorMessage.value = getLocalizedErrorMessage(it)
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
                        _successMessage.value = getApplication<Application>().getString(R.string.please_login_again)
                    } else {
                        _errorMessage.value = getApplication<Application>().getString(R.string.failed_to_restore_vault)
                    }
                } else {
                    _successMessage.value = getApplication<Application>().getString(R.string.synced_successfully)
                }
            }.onFailure {
                _errorMessage.value = getLocalizedErrorMessage(it)
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
                        _successMessage.value = getApplication<Application>().getString(R.string.backup_successful)
                    }.onFailure {
                        _errorMessage.value = getLocalizedErrorMessage(it)
                    }
                } else {
                    _errorMessage.value = getApplication<Application>().getString(R.string.failed_to_create_backup)
                }
            } catch (e: Exception) {
                _errorMessage.value = getApplication<Application>().getString(R.string.failed_to_backup_to_cloud)
            }
        }
    }

    private fun getLocalizedErrorMessage(throwable: Throwable): String {
        return if (throwable is CloudSyncErrorException) {
            when (throwable.error) {
                is CloudSyncError.RegistrationFailed -> getApplication<Application>().getString(R.string.registration_failed)
                is CloudSyncError.LoginFailed -> getApplication<Application>().getString(R.string.login_failed)
                is CloudSyncError.SyncFailed -> getApplication<Application>().getString(R.string.sync_failed)
                is CloudSyncError.GetPasswordsFailed -> getApplication<Application>().getString(R.string.failed_to_get_passwords)
                is CloudSyncError.NetworkError -> getApplication<Application>().getString(R.string.network_error, throwable.error.statusCode)
                is CloudSyncError.UnknownError -> getApplication<Application>().getString(R.string.unknown_error)
            }
        } else {
            throwable.message ?: getApplication<Application>().getString(R.string.unknown_error)
        }
    }

    fun backupVault() {
        viewModelScope.launch {
            try {
                val backup = repository.getVaultBackup()
                if (backup != null) {
                    // In a real app, you would save this backup to a file or share it
                    _successMessage.value = getApplication<Application>().getString(R.string.backup_created_successfully)
                } else {
                    _errorMessage.value = getApplication<Application>().getString(R.string.failed_to_create_backup)
                }
            } catch (e: Exception) {
                _errorMessage.value = getApplication<Application>().getString(R.string.failed_to_create_backup)
            }
        }
    }

    fun restoreVault(backup: String) {
        viewModelScope.launch {
            try {
                val success = repository.restoreVault(backup)
                if (success) {
                    _successMessage.value = getApplication<Application>().getString(R.string.vault_restored_successfully)
                } else {
                    _errorMessage.value = getApplication<Application>().getString(R.string.failed_to_restore_vault)
                }
            } catch (e: Exception) {
                _errorMessage.value = getApplication<Application>().getString(R.string.failed_to_restore_vault)
            }
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }
}