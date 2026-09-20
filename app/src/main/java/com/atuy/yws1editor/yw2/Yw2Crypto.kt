package com.atuy.yws1editor.yw2

import java.io.IOException
import java.security.MessageDigest
import java.util.zip.CRC32
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * Yo-kai Watch 2 save encryption/decryption.
 *
 * Port of the MIT-licensed yw_save.py logic used by ykw-editors.
 * The returned decrypted image keeps the 0x20-byte CYBER-compatible header:
 * nonce[12] + 4 zero bytes + encrypted CCM tag[16] + decrypted save body.
 */
object Yw2Crypto {
    private val DEFAULT_KEY = "5+NI8WVq09V7LI5w".toByteArray(Charsets.US_ASCII)
    private const val HEADER_SIZE = 0x20
    private const val NONCE_SIZE = 12
    private const val TAG_SIZE = 16
    private const val MASK32 = 0xFFFF_FFFFL

    enum class KeyMode(val label: String) {
        DEFAULT("真打 / 元祖・本家 1.x"),
        HEAD_DERIVED("元祖・本家 2.x (head.yw)"),
    }

    data class Decoded(
        val data: ByteArray,
        val keyMode: KeyMode,
    )

    fun decrypt(encrypted: ByteArray, headYw: ByteArray? = null): Decoded {
        requireEncryptedShape(encrypted)

        runCatching { decryptWithKey(encrypted, DEFAULT_KEY) }
            .getOrNull()
            ?.let { return Decoded(it, KeyMode.DEFAULT) }

        if (headYw != null) {
            val derived = deriveYw2xKey(headYw)
            runCatching { decryptWithKey(encrypted, derived) }
                .getOrNull()
                ?.let { return Decoded(it, KeyMode.HEAD_DERIVED) }
        }

        throw IOException(
            if (headYw == null) {
                "YW2セーブを復号できませんでした。元祖・本家2.xの場合は先に head.yw を選択してください。"
            } else {
                "YW2セーブを復号できませんでした。game*.yw / head.yw の組み合わせを確認してください。"
            }
        )
    }

    fun encrypt(decoded: ByteArray, keyMode: KeyMode, headYw: ByteArray? = null): ByteArray {
        if (decoded.size <= HEADER_SIZE + 8) throw IOException("復号済みセーブが短すぎます")
        val nonce = decoded.copyOfRange(0, NONCE_SIZE)
        val key = when (keyMode) {
            KeyMode.DEFAULT -> DEFAULT_KEY
            KeyMode.HEAD_DERIVED -> {
                if (headYw == null) throw IOException("元祖・本家2.xの保存には head.yw が必要です")
                deriveYw2xKey(headYw)
            }
        }

        val body = decoded.copyOfRange(HEADER_SIZE, decoded.size)
        val ywEncrypted = ywTransform(body, encrypt = true)
        return ccmEncrypt(ywEncrypted, nonce, key)
    }

    internal fun deriveYw2xKey(headYw: ByteArray): ByteArray {
        val head = ywTransform(headYw, encrypt = false)
        if (head.size < 0x10) throw IOException("head.yw が短すぎます")
        val seed = readU32Le(head, 0x0C)
        // Match yw_save.py exactly: a fresh Xorshift is constructed for each byte.
        val value = XorShift(seed).next(0x100).toInt()
        return ByteArray(16) { value.toByte() }
    }

    private fun decryptWithKey(encrypted: ByteArray, key: ByteArray): ByteArray {
        val nonce = encrypted.copyOfRange(0, NONCE_SIZE)
        val tagEncrypted = encrypted.copyOfRange(0x10, 0x20)
        val ciphertext = encrypted.copyOfRange(0x20, encrypted.size)

        val plainYw = ccmDecrypt(ciphertext, tagEncrypted, nonce, key)
        val body = ywTransform(plainYw, encrypt = false)

        return ByteArray(HEADER_SIZE + body.size).also { out ->
            nonce.copyInto(out, 0)
            tagEncrypted.copyInto(out, 0x10)
            body.copyInto(out, HEADER_SIZE)
        }
    }

    private fun requireEncryptedShape(data: ByteArray) {
        if (data.size <= HEADER_SIZE + 8) throw IOException("game*.yw が短すぎます")
        if (data.copyOfRange(12, 16).any { it.toInt() != 0 }) {
            throw IOException("game*.yw のnonceヘッダーが不正です")
        }
    }

    private fun ywTransform(data: ByteArray, encrypt: Boolean): ByteArray {
        if (data.size < 8) throw IOException("YW暗号データが短すぎます")
        val payloadSize = data.size - 8
        val storedCrc = readU32Le(data, payloadSize)
        val seed = readU32Le(data, payloadSize + 4)

        if (!encrypt) {
            val actual = crc32(data, 0, payloadSize)
            if (actual != storedCrc) {
                throw IOException("YWセーブのCRC32が一致しません")
            }
        }

        val transformed = YwCipher(seed, 0x1000).apply(data.copyOfRange(0, payloadSize))
        val out = ByteArray(data.size)
        transformed.copyInto(out, 0)
        data.copyOfRange(payloadSize, data.size).copyInto(out, payloadSize)

        if (encrypt) {
            writeU32Le(out, payloadSize, crc32(out, 0, payloadSize))
        }
        return out
    }

    private fun ccmEncrypt(plain: ByteArray, nonce: ByteArray, key: ByteArray): ByteArray {
        if (nonce.size != NONCE_SIZE) throw IOException("nonce長が不正です")
        if (key.size != 16) throw IOException("AES key長が不正です")

        val mac = calculateMac(plain, nonce, key)
        val tagEncrypted = xor(mac, counterBlock(nonce, 0, key))
        val ciphertext = ctrCrypt(plain, nonce, key, startCounter = 1)

        return ByteArray(0x20 + ciphertext.size).also { out ->
            nonce.copyInto(out, 0)
            tagEncrypted.copyInto(out, 0x10)
            ciphertext.copyInto(out, 0x20)
        }
    }

    private fun ccmDecrypt(
        ciphertext: ByteArray,
        encryptedTag: ByteArray,
        nonce: ByteArray,
        key: ByteArray,
    ): ByteArray {
        if (encryptedTag.size != TAG_SIZE) throw IOException("CCM tag長が不正です")
        val mac = xor(encryptedTag, counterBlock(nonce, 0, key))
        val plain = ctrCrypt(ciphertext, nonce, key, startCounter = 1)
        val expected = calculateMac(plain, nonce, key)
        if (!MessageDigest.isEqual(mac, expected)) throw IOException("AES-CCM認証に失敗しました")
        return plain
    }

    private fun calculateMac(message: ByteArray, nonce: ByteArray, aes: AesEcb): ByteArray {
        if (message.size > 0xFF_FFFF) throw IOException("CCMメッセージが長すぎます")
        val b0 = ByteArray(16)
        // M=16 => M'=7, L=3 => L'=2, no associated data.
        b0[0] = 0x3A
        nonce.copyInto(b0, 1)
        b0[13] = ((message.size ushr 16) and 0xFF).toByte()
        b0[14] = ((message.size ushr 8) and 0xFF).toByte()
        b0[15] = (message.size and 0xFF).toByte()

        var x = aes.encrypt(b0)
        var pos = 0
        while (pos < message.size) {
            val block = ByteArray(16)
            val n = minOf(16, message.size - pos)
            message.copyInto(block, 0, pos, pos + n)
            for (i in 0 until 16) block[i] = (block[i].toInt() xor x[i].toInt()).toByte()
            x = aes.encrypt(block)
            pos += n
        }
        return x
    }

    private fun ctrCrypt(data: ByteArray, nonce: ByteArray, aes: AesEcb, startCounter: Int): ByteArray {
        val out = ByteArray(data.size)
        var pos = 0
        var counter = startCounter
        while (pos < data.size) {
            val stream = counterBlock(nonce, counter, aes)
            val n = minOf(16, data.size - pos)
            for (i in 0 until n) out[pos + i] = (data[pos + i].toInt() xor stream[i].toInt()).toByte()
            pos += n
            counter++
        }
        return out
    }

    private fun counterBlock(nonce: ByteArray, counter: Int, aes: AesEcb): ByteArray {
        if (counter !in 0..0xFF_FFFF) throw IOException("CCM counter範囲外です")
        val a = ByteArray(16)
        a[0] = 2 // L' for a 12-byte nonce
        nonce.copyInto(a, 1)
        a[13] = ((counter ushr 16) and 0xFF).toByte()
        a[14] = ((counter ushr 8) and 0xFF).toByte()
        a[15] = (counter and 0xFF).toByte()
        return aes.encrypt(a)
    }

    private class AesEcb(key: ByteArray) {
        private val cipher = Cipher.getInstance("AES/ECB/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"))
        }

        fun encrypt(block: ByteArray): ByteArray = cipher.doFinal(block)
    }

    private fun xor(a: ByteArray, b: ByteArray): ByteArray =
        ByteArray(a.size) { i -> (a[i].toInt() xor b[i].toInt()).toByte() }

    private fun crc32(data: ByteArray, offset: Int, length: Int): Long =
        CRC32().apply { update(data, offset, length) }.value and MASK32

    private fun readU32Le(data: ByteArray, offset: Int): Long {
        if (offset < 0 || offset + 4 > data.size) throw IOException("u32 read範囲外です")
        return (data[offset].toLong() and 0xFF) or
            ((data[offset + 1].toLong() and 0xFF) shl 8) or
            ((data[offset + 2].toLong() and 0xFF) shl 16) or
            ((data[offset + 3].toLong() and 0xFF) shl 24)
    }

    private fun writeU32Le(data: ByteArray, offset: Int, value: Long) {
        data[offset] = (value and 0xFF).toByte()
        data[offset + 1] = ((value ushr 8) and 0xFF).toByte()
        data[offset + 2] = ((value ushr 16) and 0xFF).toByte()
        data[offset + 3] = ((value ushr 24) and 0xFF).toByte()
    }

    private class XorShift(seed: Long) {
        private val states = longArrayOf(0x6C078966L, 0xDD5254A5L, 0xB9523B81L, 0x03DF95B3L)

        init {
            if ((seed and MASK32) != 0L) {
                var s = seed and MASK32
                repeat(3) { i ->
                    s = s xor (s ushr 30)
                    s = (s * (0x6C078966L - 1L)) and MASK32
                    s = (s + i + 1L) and MASK32
                    states[i] = s
                }
            }
        }

        fun next(bound: Int): Long {
            var x = states[0]
            var y = states[3]
            states[0] = states[1]
            states[1] = states[2]
            states[2] = states[3]
            x = x xor ((x shl 11) and MASK32)
            x = x xor (x ushr 8)
            y = y xor (y ushr 19)
            states[3] = (x xor y) and MASK32
            return if (bound == 0) states[3] else states[3] % bound.toLong()
        }
    }

    private class YwCipher(seed: Long, count: Int) {
        private val table = IntArray(256) { it }
        private val rng = XorShift(seed)
        private val oddPrimes = firstOddPrimes(256)

        init {
            repeat(count) {
                val r = rng.next(0x10000).toInt()
                var r1 = r and 0xFF
                var r2 = (r ushr 8) and 0xFF
                if (r1 != r2) {
                    r1 = table[r1]
                    r2 = table[r2]
                    val tmp = table[r1]
                    table[r1] = table[r2]
                    table[r2] = tmp
                }
            }
        }

        fun apply(input: ByteArray): ByteArray {
            val out = ByteArray(input.size)
            var ka = 0
            for (i in input.indices) {
                if (i % 0x100 == 0) {
                    val tableIndex = (i and 0xFF00) ushr 8
                    if (tableIndex >= table.size) throw IOException("YW暗号の入力が大きすぎます")
                    ka = oddPrimes[table[tableIndex]]
                }
                val kb = table[(ka * (i + 1)) and 0xFF]
                out[i] = (input[i].toInt() xor kb).toByte()
            }
            return out
        }
    }

    private fun firstOddPrimes(count: Int): IntArray {
        val out = IntArray(count)
        var found = 0
        var n = 3
        while (found < count) {
            var prime = true
            var d = 3
            while (d * d <= n) {
                if (n % d == 0) {
                    prime = false
                    break
                }
                d += 2
            }
            if (prime) out[found++] = n
            n += 2
        }
        return out
    }
}
