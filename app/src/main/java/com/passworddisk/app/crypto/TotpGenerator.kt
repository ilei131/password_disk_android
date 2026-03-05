package com.passworddisk.app.crypto

import org.apache.commons.codec.binary.Base32
import java.nio.ByteBuffer
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.experimental.and

object TotpGenerator {
    private const val TIME_STEP = 30L
    private const val CODE_DIGITS = 6

    fun generateCode(secret: String): String {
        val normalizedSecret = secret.trim().uppercase().replace(" ", "")
        val base32 = Base32()
        val key = base32.decode(normalizedSecret)
        val truncatedKey = if (key.size > 20) key.copyOf(20) else key

        val timeIndex = System.currentTimeMillis() / 1000 / TIME_STEP
        val buffer = ByteBuffer.allocate(8)
        buffer.putLong(timeIndex)

        val signData = buffer.array()
        val mac = Mac.getInstance("HmacSHA1")
        val signKey = SecretKeySpec(truncatedKey, "HmacSHA1")
        mac.init(signKey)
        val hash = mac.doFinal(signData)

        val offset = hash[hash.size - 1].and(0x0F).toInt()
        val binary = ((hash[offset].toInt() and 0x7F) shl 24 or
                (hash[offset + 1].toInt() and 0xFF) shl 16 or
                (hash[offset + 2].toInt() and 0xFF) shl 8 or
                (hash[offset + 3].toInt() and 0xFF))

        val otp = binary % Math.pow(10.0, CODE_DIGITS.toDouble()).toInt()
        return otp.toString().padStart(CODE_DIGITS, '0')
    }
}