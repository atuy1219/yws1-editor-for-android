package com.atuy.yws1editor.yokai

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailySystemCodecTest {
    private val codec = DailySystemCodec()

    @Test
    fun decodeReadsConfirmedDailyFields() {
        val data = fixture(fill = 0)
        setBit(data, DailySystemCodec.GASHA_REWARD_BIT, true)
        setByte(data, DailySystemCodec.GASHA_USE_COUNT_BYTE, 3)
        setBit(data, DailySystemCodec.SASURAI_REWARD_BIT, true)
        setByte(data, DailySystemCodec.SASURAI_REWARD_COUNT_BYTE, 2)
        setBit(data, DailySimpleFlag.GoldEgg.bitIndex, true)
        setBit(data, 537, true)

        val state = codec.decode(data)

        assertTrue(state.gashaRewardClaimed)
        assertEquals(3, state.gashaUseCount)
        assertTrue(state.sasuraiRewardDrawn)
        assertEquals(2, state.sasuraiRewardCount)
        assertTrue(state.simpleFlags.single { it.definition == DailySimpleFlag.GoldEgg }.usedToday)
        assertTrue(state.battles.single { it.definition.flagIndex == 537 }.foughtToday)
    }

    @Test
    fun resetGashaDoesNotChangeLotteryStateOrUnrelatedBytes() {
        val original = fixture(fill = 0xA5)
        val decoded = codec.decode(original)

        val edited = codec.apply(original, codec.resetGasha(decoded))

        assertFalse(readBit(edited, DailySystemCodec.GASHA_REWARD_BIT))
        assertEquals(0, readByte(edited, DailySystemCodec.GASHA_USE_COUNT_BYTE))
        assertEquals(
            original[DailySystemCodec.GLOBAL_BYTE_DATA_OFFSET + 34],
            edited[DailySystemCodec.GLOBAL_BYTE_DATA_OFFSET + 34],
        )
        assertUnrelatedBitsPreserved(original, edited)
    }

    @Test
    fun resetSasuraiPreservesResidentRegionAndAppearanceFlags() {
        val original = fixture(fill = 0xFF)
        val state = codec.resetSasurai(codec.decode(original))

        val edited = codec.apply(original, state)

        assertFalse(readBit(edited, DailySystemCodec.SASURAI_REWARD_BIT))
        assertEquals(0, readByte(edited, DailySystemCodec.SASURAI_REWARD_COUNT_BYTE))
        (1098..1106).forEach { assertTrue(readBit(edited, it)) }
        assertUnrelatedBitsPreserved(original, edited)
    }

    @Test
    fun resetAllClearsOnlyDocumentedDailyFields() {
        val original = fixture(fill = 0xFF)
        val reset = codec.resetAll(codec.decode(original))
        val edited = codec.apply(original, reset)

        assertTrue(codec.decode(edited).battles.none { it.foughtToday })
        assertFalse(codec.decode(edited).gashaRewardClaimed)
        assertEquals(0, codec.decode(edited).gashaUseCount)
        assertFalse(codec.decode(edited).sasuraiRewardDrawn)
        assertEquals(0, codec.decode(edited).sasuraiRewardCount)
        assertTrue(codec.decode(edited).simpleFlags.none { it.usedToday })

        val reapplied = codec.apply(original, codec.decode(edited))
        assertArrayEquals(edited, reapplied)
        assertUnrelatedBitsPreserved(original, edited)
    }

    private fun fixture(fill: Int): ByteArray =
        ByteArray(DailySystemCodec.GLOBAL_FLAG_DATA_OFFSET + DailySystemCodec.GLOBAL_FLAG_DATA_SIZE) {
            fill.toByte()
        }

    private fun readBit(data: ByteArray, index: Int): Boolean {
        val offset = DailySystemCodec.GLOBAL_FLAG_DATA_OFFSET + index / 8
        return data[offset].toInt() and (1 shl (index and 7)) != 0
    }

    private fun setBit(data: ByteArray, index: Int, enabled: Boolean) {
        val offset = DailySystemCodec.GLOBAL_FLAG_DATA_OFFSET + index / 8
        val mask = 1 shl (index and 7)
        val old = data[offset].toInt() and 0xff
        data[offset] = if (enabled) (old or mask).toByte() else (old and mask.inv()).toByte()
    }

    private fun readByte(data: ByteArray, index: Int): Int =
        data[DailySystemCodec.GLOBAL_BYTE_DATA_OFFSET + index].toInt() and 0xff

    private fun setByte(data: ByteArray, index: Int, value: Int) {
        data[DailySystemCodec.GLOBAL_BYTE_DATA_OFFSET + index] = value.toByte()
    }

    private fun assertUnrelatedBitsPreserved(original: ByteArray, edited: ByteArray) {
        val editableBits = buildSet {
            addAll(DailyBattleCodec.DEFINITIONS.map { it.flagIndex })
            add(DailySystemCodec.GASHA_REWARD_BIT)
            add(DailySystemCodec.SASURAI_REWARD_BIT)
            addAll(DailySimpleFlag.entries.map { it.bitIndex })
        }
        original.indices.forEach { offset ->
            val byteIndex = offset - DailySystemCodec.GLOBAL_FLAG_DATA_OFFSET
            val editableMask = if (byteIndex in 0 until DailySystemCodec.GLOBAL_FLAG_DATA_SIZE) {
                editableBits
                    .filter { it / 8 == byteIndex }
                    .fold(0) { mask, bit -> mask or (1 shl (bit and 7)) }
            } else {
                0
            }
            val isEditableCounter = offset == DailySystemCodec.GLOBAL_BYTE_DATA_OFFSET +
                DailySystemCodec.GASHA_USE_COUNT_BYTE ||
                offset == DailySystemCodec.GLOBAL_BYTE_DATA_OFFSET +
                DailySystemCodec.SASURAI_REWARD_COUNT_BYTE
            if (!isEditableCounter) {
                val originalUnrelated = original[offset].toInt() and 0xff and editableMask.inv()
                val editedUnrelated = edited[offset].toInt() and 0xff and editableMask.inv()
                assertEquals("offset=0x${offset.toString(16)}", originalUnrelated, editedUnrelated)
            }
        }
    }
}
