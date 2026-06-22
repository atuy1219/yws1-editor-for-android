package com.atuy.yws1editor.yokai

import java.io.IOException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class GashaStateCodecTest {
    private val codec = GashaStateCodec()

    @Test
    fun decodesFirstAndLastOf32EntriesAsLittleEndianU32() {
        val data = fixture()
        putU32(data, GashaStateCodec.OFFSET, 0x89ABCDEF)
        val last = GashaStateCodec.OFFSET + (GashaStateCodec.ENTRY_COUNT - 1) * GashaStateCodec.STRIDE
        putU32(data, last + 12, 0xFEDCBA98)

        val decoded = codec.decode(data)

        assertEquals(GashaStateCodec.ENTRY_COUNT, decoded.size)
        assertEquals(0x89ABCDEFL, decoded.first().words.first())
        assertEquals(0xFEDCBA98L, decoded.last().words.last())
    }

    @Test
    fun backupAndRestoreRoundTripPreservesOriginalInput() {
        val original = fixture().also { data ->
            repeat(GashaStateCodec.REGION_SIZE) { data[GashaStateCodec.OFFSET + it] = (it * 31).toByte() }
        }
        val backup = codec.backup(original)
        val changed = original.copyOf().also { it[GashaStateCodec.OFFSET + 7] = 0 }

        val restored = codec.restore(changed, backup)

        assertArrayEquals(original, restored)
        assertEquals((7 * 31).toByte(), original[GashaStateCodec.OFFSET + 7])
    }

    @Test
    fun rejectsShortDataAndWrongBackupSize() {
        assertThrows(IOException::class.java) { codec.decode(ByteArray(GashaStateCodec.OFFSET)) }
        assertThrows(IOException::class.java) { codec.restore(fixture(), ByteArray(511)) }
    }

    private fun fixture() = ByteArray(GashaStateCodec.OFFSET + GashaStateCodec.REGION_SIZE)

    private fun putU32(data: ByteArray, offset: Int, value: Long) {
        repeat(4) { index -> data[offset + index] = (value ushr (index * 8)).toByte() }
    }
}
