package com.passworddisk.app.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import at.favre.lib.crypto.bcrypt.BCrypt
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoManager {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "PasswordDiskKey"
    private const val AES_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128
    private const val IV_SIZE = 12

    fun generateSalt(): String {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        return salt.toHex()
    }

    fun hashPassword(password: String): String {
        return BCrypt.withDefaults().hashToString(12, password.toCharArray())
    }

    fun verifyPassword(password: String, hash: String): Boolean {
        return BCrypt.verifyer().verify(password.toCharArray(), hash).verified
    }

    fun deriveKey(masterPassword: String, salt: String): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(masterPassword.toByteArray(Charsets.UTF_8))
        try {
            // 尝试将 salt 作为十六进制字符串解码，与 Rust 代码保持一致
            val saltBytes = salt.fromHex()
            println("Salt decoded successfully: ${saltBytes.size} bytes, salt: $salt")
            digest.update(saltBytes)
        } catch (e: Exception) {
            // 如果解码失败，打印错误信息
            println("Salt decoding failed: ${e.message}, salt: $salt")
            throw e
        }
        val key = digest.digest()
        println("Generated key: ${key.toHex().take(10)}...")
        return key
    }

    fun encryptData(data: String, key: ByteArray): String {
        // 使用 ECB 模式，与 Rust 代码保持一致
        val cipher = Cipher.getInstance("AES/ECB/NoPadding")
        val secretKey = SecretKeySpec(key, "AES")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        
        // 手动添加填充，与 Rust 代码保持一致
        val dataBytes = data.toByteArray(Charsets.UTF_8)
        val paddingSize = 16 - (dataBytes.size % 16)
        val paddedData = ByteArray(dataBytes.size + paddingSize)
        System.arraycopy(dataBytes, 0, paddedData, 0, dataBytes.size)
        for (i in dataBytes.size until paddedData.size) {
            paddedData[i] = paddingSize.toByte()
        }
        
        val encrypted = cipher.doFinal(paddedData)
        
        // 生成 16 字节的随机 IV 并添加到加密数据的前面，与 Rust 代码保持一致
        val iv = ByteArray(16)
        SecureRandom().nextBytes(iv)
        val result = ByteArray(iv.size + encrypted.size)
        System.arraycopy(iv, 0, result, 0, iv.size)
        System.arraycopy(encrypted, 0, result, iv.size, encrypted.size)
        
        return result.toHex()
    }

    fun decryptData(encryptedData: String, key: ByteArray): String {
        // 使用 ECB 模式，与 Rust 代码保持一致
        val encryptedBytes = encryptedData.fromHex()
        
        // 跳过前 16 个字节的 IV，与 Rust 代码保持一致
        val ciphertext = if (encryptedBytes.size >= 16) {
            encryptedBytes.copyOfRange(16, encryptedBytes.size)
        } else {
            encryptedBytes
        }
        
        val cipher = Cipher.getInstance("AES/ECB/NoPadding")
        val secretKey = SecretKeySpec(key, "AES")
        cipher.init(Cipher.DECRYPT_MODE, secretKey)
        val decrypted = cipher.doFinal(ciphertext)
        
        // 手动处理填充，与 Rust 代码保持一致
        if (decrypted.isNotEmpty()) {
            val paddingSize = decrypted.last().toInt()
            if (paddingSize > 0 && paddingSize <= 16) {
                return String(decrypted.copyOfRange(0, decrypted.size - paddingSize), Charsets.UTF_8)
            }
        }
        
        return String(decrypted, Charsets.UTF_8)
    }

    fun generatePassword(
        length: Int,
        includeUppercase: Boolean,
        includeLowercase: Boolean,
        includeNumbers: Boolean,
        includeSymbols: Boolean
    ): String {
        val chars = mutableListOf<Char>()
        if (includeUppercase) chars.addAll('A'..'Z')
        if (includeLowercase) chars.addAll('a'..'z')
        if (includeNumbers) chars.addAll('0'..'9')
        if (includeSymbols) chars.addAll("!@#$%^&*()_+-=[]{}|;:,.<>?".toList())

        if (chars.isEmpty()) {
            throw IllegalArgumentException("At least one character type must be selected")
        }

        val random = SecureRandom()
        return (1..length)
            .map { chars[random.nextInt(chars.size)] }
            .joinToString("")
    }

    private fun ByteArray.toHex(): String {
        return joinToString("") { "%02x".format(it) }
    }

    private fun String.fromHex(): ByteArray {
        return chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
    }
}