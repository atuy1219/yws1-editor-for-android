package com.atuy.yws1editor.yokai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyBattleCodecTest {
    private val codec = DailyBattleCodec()

    @Test
    fun definitionsContainAllThirteenUniqueDailyFlags() {
        assertEquals(13, DailyBattleCodec.DEFINITIONS.size)
        assertEquals(
            DailyBattleCodec.DEFINITIONS.size,
            DailyBattleCodec.DEFINITIONS.map { it.flagIndex }.distinct().size,
        )
        assertEquals(537, DailyBattleCodec.DEFINITIONS.single { it.name == "サファイニャン" }.flagIndex)
    }

    @Test
    fun decodeReadsEachConfiguredBit() {
        val data = fixture()
        val sapphire = DailyBattleCodec.DEFINITIONS.single { it.name == "サファイニャン" }
        data[sapphire.byteOffset] = sapphire.bitMask.toByte()

        val decoded = codec.decode(data)

        assertTrue(decoded.single { it.definition == sapphire }.foughtToday)
        assertEquals(1, decoded.count { it.foughtToday })
    }

    @Test
    fun applyChangesOnlyConfiguredBitsAndPreservesNearbyFlags() {
        val original = fixture(fill = 0xA5)
        val entries = codec.decode(original).mapIndexed { index, entry ->
            entry.copy(foughtToday = index % 2 == 0)
        }

        val edited = codec.apply(original, entries)
        val configuredMasksByOffset = DailyBattleCodec.DEFINITIONS
            .groupBy { it.byteOffset }
            .mapValues { (_, definitions) -> definitions.fold(0) { mask, definition -> mask or definition.bitMask } }

        original.indices.forEach { offset ->
            val configuredMask = configuredMasksByOffset[offset] ?: 0
            if (configuredMask == 0) {
                assertEquals(original[offset], edited[offset])
            } else {
                val originalUnrelated = original[offset].toInt() and 0xff and configuredMask.inv()
                val editedUnrelated = edited[offset].toInt() and 0xff and configuredMask.inv()
                assertEquals(originalUnrelated, editedUnrelated)
            }
        }
        assertEquals(entries, codec.decode(edited))
    }

    @Test
    fun setAllCanResetEveryDailyBattleWithoutChangingDefinitions() {
        val original = codec.decode(fixture(fill = 0xFF))

        val reset = codec.setAll(original, foughtToday = false)

        assertTrue(reset.none { it.foughtToday })
        assertEquals(original.map { it.definition }, reset.map { it.definition })
    }

    private fun fixture(fill: Int = 0): ByteArray =
        ByteArray(DailyBattleCodec.GLOBAL_FLAG_DATA_OFFSET + DailyBattleCodec.GLOBAL_FLAG_DATA_SIZE) {
            fill.toByte()
        }
}
