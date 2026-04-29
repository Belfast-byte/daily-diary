package com.example.dailydiary.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

class KeyAccessException(message: String, cause: Throwable? = null) :
    Exception(message, cause)

@Singleton
class KeyManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val AES_KEY_ALIAS = "daily_diary_db_aes"
        private const val PASS_FILE = "db_passphrase_v1.enc"
        private const val AES_KEY_SIZE = 256
        private const val GCM_TAG_LENGTH = 128
        private const val GCM_IV_LENGTH = 12
    }

    private val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    private val passFile = File(context.filesDir, PASS_FILE)

    fun getOrCreatePassphrase(): ByteArray {
        return if (passFile.exists()) {
            decryptPassphrase(passFile.readBytes())
        } else {
            val passphrase = ByteArray(32)
            kotlin.random.Random.nextBytes(passphrase)
            passFile.writeBytes(encryptPassphrase(passphrase))
            passphrase
        }
    }

    private fun encryptPassphrase(passphrase: ByteArray): ByteArray {
        val key = getOrCreateAesKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(passphrase)
        return iv + encrypted
    }

    private fun decryptPassphrase(data: ByteArray): ByteArray {
        val key = getOrCreateAesKey()
        val iv = data.copyOfRange(0, GCM_IV_LENGTH)
        val encrypted = data.copyOfRange(GCM_IV_LENGTH, data.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        return cipher.doFinal(encrypted)
    }

    private fun getOrCreateAesKey(): SecretKey {
        return if (keyStore.containsAlias(AES_KEY_ALIAS)) {
            val entry = keyStore.getEntry(AES_KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
                ?: throw KeyAccessException("Failed to load AES key from Keystore")
            entry.secretKey
        } else {
            val generator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )
            val spec = KeyGenParameterSpec.Builder(
                AES_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setKeySize(AES_KEY_SIZE)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
            generator.init(spec)
            generator.generateKey()
        }
    }
}
