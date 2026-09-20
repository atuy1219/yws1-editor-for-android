package com.atuy.yws1editor.yw2

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class Yw2CryptoTest {
    @Test
    fun defaultKeyRoundTripIsStable() {
        val decoded = ByteArray(0x20 + 0x240)
        for (i in 0 until 12) decoded[i] = (i * 7 + 3).toByte()

        val bodyStart = 0x20
        val bodySize = decoded.size - bodyStart
        for (i in 0 until bodySize - 8) decoded[bodyStart + i] = (i * 13 + 11).toByte()
        // CRC field is rewritten by encrypt(); seed must remain stable.
        putU32(decoded, decoded.size - 4, 0x13572468)

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
