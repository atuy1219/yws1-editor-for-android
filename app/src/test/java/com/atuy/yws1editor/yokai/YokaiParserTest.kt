package com.atuy.yws1editor.yokai

import org.junit.Assert.assertEquals
import org.junit.Test

class YokaiParserTest {
    private val parser = YokaiParser()
    private val yokaiStart = 0x1D40
    private val recordSize = 0x7C

    @Test
    fun parseTreatsIvaAndCbAsSignedBytesAndIvbAsUnsignedNibbles() {
        val data = newRecord()
        writeBytes(data, yokaiStart + 0x60, listOf(-128, -1, 0, 1, 127))
        writeBytes(data, yokaiStart + 0x65, listOf(0xF0, 0xE1, 0xD2, 0xC3, 0xB4))
        writeBytes(data, yokaiStart + 0x6A, listOf(127, 1, 0, -1, -128))

        val entry = parser.parse(data).single()

        assertEquals(Stat5(-128, -1, 0, 1, 127), entry.iva)
        assertEquals(Stat5(0, 1, 2, 3, 4), entry.ivb1)
        assertEquals(Stat5(15, 14, 13, 12, 11), entry.ivb2)
        assertEquals(Stat5(127, 1, 0, -1, -128), entry.cb)
    }

    @Test
    fun applyEntriesClampsSignedBytesAndPacksUnsignedNibbles() {
        val data = newRecord()
        val original = parser.parse(data).single()
        val edited = original.copy(
            iva = Stat5(-129, -128, -1, 127, 128),
            ivb1 = Stat5(-1, 0, 1, 15, 16),
            ivb2 = Stat5(16, 15, 14, 0, -1),
            cb = Stat5(128, 127, 1, -128, -129),
        )

        val reparsed = parser.parse(parser.applyEntries(data, listOf(edited))).single()

        assertEquals(Stat5(-128, -128, -1, 127, 127), reparsed.iva)
        assertEquals(Stat5(0, 0, 1, 15, 15), reparsed.ivb1)
        assertEquals(Stat5(15, 15, 14, 0, 0), reparsed.ivb2)
        assertEquals(Stat5(127, 127, 1, -128, -128), reparsed.cb)
    }

    private fun newRecord(): ByteArray {
        val data = ByteArray(yokaiStart + recordSize)
        writeIntLe(data, yokaiStart, 1)
        writeIntLe(data, yokaiStart + 0x04, 1)
        return data
    }

    private fun writeBytes(data: ByteArray, offset: Int, values: List<Int>) {
        values.forEachIndexed { index, value -> data[offset + index] = value.toByte() }
    }

    private fun writeIntLe(data: ByteArray, offset: Int, value: Int) {
        data[offset] = value.toByte()
        data[offset + 1] = (value ushr 8).toByte()
        data[offset + 2] = (value ushr 16).toByte()
        data[offset + 3] = (value ushr 24).toByte()
    }
}
