package com.atuy.yws1editor.yokai

import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SasuraiCodecTest {
    private val codec = SasuraiCodec()

    @Test
    fun decodesFirstAndNinthResidentBoundaryAndSummaries() {
        val data = fixture()
        val first = SasuraiCodec.OFFSET
        putU16(data, first, 0x1234)
        putU16(data, first + 2, 0x0081)
        putU32(data, first + 4, 0x89ABCDEF)
        "resident".toByteArray().copyInto(data, first + 8)
        putU32(data, first + 0x4c + 0x40, 0x10203040)
        putU32(data, first + 0x4c + 0x50, 0xFEDCBA98)

        val ninth = SasuraiCodec.OFFSET + (SasuraiCodec.ENTRY_COUNT - 1) * SasuraiCodec.STRIDE
        putU16(data, ninth, 9)
        putU32(data, ninth + 4, 0x55667788)
        data[ninth + 0x198] = 1

        val decoded = codec.decode(data)

        assertEquals(SasuraiCodec.ENTRY_COUNT, decoded.size)
        assertTrue(decoded.first().isUsed)
        assertEquals(0x1234, decoded.first().sequence)
        assertEquals(0x81, decoded.first().state)
        assertEquals(0x89ABCDEFL, decoded.first().encounterId)
        assertEquals("resident", decoded.first().displayName)
        assertEquals(3, decoded.first().yokai.size)
        assertEquals(0x10203040L, decoded.first().yokai.first().equipmentConfigId)
        assertEquals(0xFEDCBA98L, decoded.first().yokai.first().yokaiId)
        assertTrue(decoded.last().isUsed)
        assertEquals(1, decoded.last().statusByte)
    }

    @Test
    fun allZeroResidentIsEmpty() {
        val decoded = codec.decode(fixture())
        assertEquals(SasuraiCodec.ENTRY_COUNT, decoded.size)
        assertFalse(decoded.first().isUsed)
        assertFalse(decoded.last().isUsed)
    }

    @Test
    fun rejectsDataTruncatedBeforeNinthResidentEnd() {
        assertThrows(IOException::class.java) {
            codec.decode(ByteArray(SasuraiCodec.OFFSET + SasuraiCodec.REGION_SIZE - 1))
        }
    }

    private fun fixture() = ByteArray(SasuraiCodec.OFFSET + SasuraiCodec.REGION_SIZE)

    private fun putU16(data: ByteArray, offset: Int, value: Int) {
        data[offset] = value.toByte()
        data[offset + 1] = (value ushr 8).toByte()
    }

    private fun putU32(data: ByteArray, offset: Int, value: Long) {
        repeat(4) { index -> data[offset + index] = (value ushr (index * 8)).toByte() }
    }
}
