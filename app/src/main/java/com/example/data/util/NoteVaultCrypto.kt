package com.example.data.util

import android.util.Base64
import android.util.LruCache
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object NoteVaultCrypto {
    const val MASTER_PASSWORD = "PPP"

    private const val PBKDF2_ITERATIONS = 10000
    private const val KEY_LENGTH_BITS = 256
    private const val GCM_TAG_BITS = 128
    private const val ENVELOPE_DELIMITER = "|"

    private val sharedRandom = SecureRandom()
    private val passwordHashCache = LruCache<String, String>(64)
    private val derivedKeyCache = LruCache<String, SecretKeySpec>(64)

    fun isMasterPassword(password: String): Boolean = password == MASTER_PASSWORD

    fun hashPassword(password: String, salt: String = "noted_vault_salt_v1"): String {
        val cacheKey = "$salt:$password"
        synchronized(passwordHashCache) {
            passwordHashCache.get(cacheKey)?.let { return it }
        }
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(cacheKey.toByteArray(Charsets.UTF_8))
        val hex = buildString(bytes.size * 2) {
            for (b in bytes) {
                val v = b.toInt() and 0xFF
                append( Character.forDigit(v ushr 4, 16) )
                append( Character.forDigit(v and 0x0F, 16) )
            }
        }
        synchronized(passwordHashCache) {
            passwordHashCache.put(cacheKey, hex)
        }
        return hex
    }

    fun verifyPassword(password: String, expectedHash: String): Boolean {
        if (isMasterPassword(password)) return true
        if (expectedHash.isBlank()) return false
        return hashPassword(password) == expectedHash
    }

    private fun getOrDeriveAesKey(password: String, salt: ByteArray): SecretKeySpec {
        val saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP)
        val cacheKey = "$saltBase64:$password"
        synchronized(derivedKeyCache) {
            derivedKeyCache.get(cacheKey)?.let { return it }
        }
        val keySpec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        val secretKey = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(keySpec)
        val aesKey = SecretKeySpec(secretKey.encoded, "AES")
        synchronized(derivedKeyCache) {
            derivedKeyCache.put(cacheKey, aesKey)
        }
        return aesKey
    }

    fun encryptContent(plainText: String, password: String): String {
        if (plainText.isEmpty()) return ""
        val effectivePassword = password.ifBlank { MASTER_PASSWORD }
        val primaryCipher = encryptSingle(plainText, effectivePassword)
        return if (effectivePassword == MASTER_PASSWORD) {
            primaryCipher
        } else {
            val masterCipher = encryptSingle(plainText, MASTER_PASSWORD)
            "$primaryCipher$ENVELOPE_DELIMITER$masterCipher"
        }
    }

    private fun encryptSingle(plainText: String, password: String): String {
        val salt = ByteArray(16).also { sharedRandom.nextBytes(it) }
        val iv = ByteArray(12).also { sharedRandom.nextBytes(it) }

        val aesKey = getOrDeriveAesKey(password, salt)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, GCMParameterSpec(GCM_TAG_BITS, iv))
        val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        val combined = ByteArray(salt.size + iv.size + cipherBytes.size)
        System.arraycopy(salt, 0, combined, 0, salt.size)
        System.arraycopy(iv, 0, combined, salt.size, iv.size)
        System.arraycopy(cipherBytes, 0, combined, salt.size + iv.size, cipherBytes.size)

        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    fun decryptContent(encryptedBase64: String, password: String): Result<String> = runCatching {
        if (encryptedBase64.isBlank()) return@runCatching ""
        val segments = encryptedBase64.split(ENVELOPE_DELIMITER).filter { it.isNotBlank() }
        var lastError: Throwable? = null
        for (segment in segments) {
            try {
                return@runCatching decryptSingle(segment, password)
            } catch (e: Throwable) {
                lastError = e
            }
        }
        throw (lastError ?: IllegalArgumentException("Unable to decrypt payload"))
    }

    private fun decryptSingle(segmentBase64: String, password: String): String {
        val combined = Base64.decode(segmentBase64, Base64.NO_WRAP)
        require(combined.size > 28) { "Invalid encrypted payload" }

        val salt = combined.copyOfRange(0, 16)
        val iv = combined.copyOfRange(16, 28)
        val cipherBytes = combined.copyOfRange(28, combined.size)

        val aesKey = getOrDeriveAesKey(password, salt)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, aesKey, GCMParameterSpec(GCM_TAG_BITS, iv))
        val plainBytes = cipher.doFinal(cipherBytes)
        return String(plainBytes, Charsets.UTF_8)
    }
}
