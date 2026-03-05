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
        digest.update(salt.fromHex())
        return digest.digest()
    }

    fun encryptData(data: String, key: ByteArray): String {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val iv = ByteArray(16)
        SecureRandom().nextBytes(iv)
        val ivSpec = IvParameterSpec(iv)
        val secretKey = SecretKeySpec(key, "AES")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)
        val encrypted = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
        return (iv + encrypted).toHex()
    }

    fun decryptData(encryptedData: String, key: ByteArray): String {
        val encryptedBytes = encryptedData.fromHex()
        if (encryptedBytes.size < 16) {
            throw IllegalArgumentException("Encrypted data too short")
        }
        val iv = encryptedBytes.copyOfRange(0, 16)
        val ciphertext = encryptedBytes.copyOfRange(16, encryptedBytes.size)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val ivSpec = IvParameterSpec(iv)
        val secretKey = SecretKeySpec(key, "AES")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
        val decrypted = cipher.doFinal(ciphertext)
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