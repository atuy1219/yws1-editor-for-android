package com.atuy.yws1editor.yw2

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class Yw2CryptoTest {
    @Test
    fun defaultKeyRoundTripIsStable() {
        // Build a minimal but structurally valid YW2 section tree:
        // root(F3) -> section 01 + section 07, followed by CRC + YWCipher seed.
        val payloadSize = 44
        val decoded = ByteArray(0x20 + payloadSize + 8)
        for (i in 0 until 12) decoded[i] = (i * 7 + 3).toByte()

        val bodyStart = 0x20
        putU32(decoded, bodyStart, 0x0000FFFE)
        putU32(decoded, bodyStart + 4, (32 shl 8) or 0xF3)

        val s01 = bodyStart + 8
        putU32(decoded, s01, 0x0000FFFE)
        putU32(decoded, s01 + 4, (4 shl 8) or 0x01)
        putU32(decoded, s01 + 8, 0x11223344)
        putU32(decoded, s01 + 12, 0x0000FEFF)

        val s07 = s01 + 16
        putU32(decoded, s07, 0x0000FFFE)
        putU32(decoded, s07 + 4, (4 shl 8) or 0x07)
        putU32(decoded, s07 + 8, 0x55667788)
        putU32(decoded, s07 + 12, 0x0000FEFF)

        putU32(decoded, bodyStart + 40, 0x0000FEFF)
        // CRC field is rewritten by encrypt(); seed must remain stable.
        putU32(decoded, decoded.size - 4, 0x13572468)
        val bodySize = decoded.size - bodyStart

        val encrypted1 = Yw2Crypto.encrypt(decoded, Yw2Crypto.KeyMode.DEFAULT)
        val decoded1 = Yw2Crypto.decrypt(encrypted1)
        assertEquals(Yw2Crypto.KeyMode.DEFAULT, decoded1.keyMode)

        for (i in 0 until bodySize - 8) {
            assertEquals(decoded[bodyStart + i], decoded1.data[bodyStart + i])
        }
        assertArrayEquals(
            decoded.copyOfRange(decoded.size - 4, decoded.size),
            decoded1.data.copyOfRange(decoded1.data.size - 4, decoded1.data.size),
        )

        val encrypted2 = Yw2Crypto.encrypt(decoded1.data, decoded1.keyMode)
        assertArrayEquals(encrypted1, encrypted2)
    }

    private fun putU32(data: ByteArray, o: Int, value: Int) {
        data[o] = value.toByte()
        data[o + 1] = (value ushr 8).toByte()
        data[o + 2] = (value ushr 16).toByte()
        data[o + 3] = (value ushr 24).toByte()
    }
}
