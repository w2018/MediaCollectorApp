package com.mediacollector.app.ui.chat

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-256-CBC 加密/解密工具
 *
 * 硬编码密钥（32字节 = 256位）
 * 每条消息随机 IV，附在密文前一起传输
 */
object ChatCrypto {

    // 32 字节密钥（256 位）— 硬编码
    private const val SECRET_KEY = "M3d14C0ll3ct0rCh4tK3y!@#2025"

    private const val ALGORITHM = "AES/CBC/PKCS5Padding"
    private const val IV_SIZE = 16 // AES 块大小

    /** 加密文本 */
    fun encrypt(plainText: String): String {
        return try {
            val cipher = Cipher.getInstance(ALGORITHM)
            val keySpec = SecretKeySpec(SECRET_KEY.toByteArray(Charsets.UTF_8), "AES")

            // 生成随机 IV
            cipher.init(Cipher.ENCRYPT_MODE, keySpec)
            val iv = cipher.iv // 自动生成

            val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

            // 格式: Base64(IV + 密文)
            val combined = iv + encrypted
            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            // 加密失败返回原文（降级）
            plainText
        }
    }

    /** 解密文本 */
    fun decrypt(encryptedText: String): String {
        return try {
            val combined = Base64.decode(encryptedText, Base64.NO_WRAP)

            // 提取 IV（前 16 字节）
            val iv = combined.copyOfRange(0, IV_SIZE)
            val cipherText = combined.copyOfRange(IV_SIZE, combined.size)

            val cipher = Cipher.getInstance(ALGORITHM)
            val keySpec = SecretKeySpec(SECRET_KEY.toByteArray(Charsets.UTF_8), "AES")
            val ivSpec = IvParameterSpec(iv)

            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
            val decrypted = cipher.doFinal(cipherText)

            String(decrypted, Charsets.UTF_8)
        } catch (e: Exception) {
            // 解密失败返回原文（兼容未加密消息）
            encryptedText
        }
    }

    /**
     * 检查文本是否已加密（Base64 + 长度 >= 24）
     */
    fun isEncrypted(text: String): Boolean {
        return try {
            Base64.decode(text, Base64.NO_WRAP).size >= IV_SIZE + 1
        } catch (_: Exception) {
            false
        }
    }
}
