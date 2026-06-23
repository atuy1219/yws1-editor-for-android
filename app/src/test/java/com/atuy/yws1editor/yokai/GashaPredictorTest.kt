package com.atuy.yws1editor.yokai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GashaPredictorTest {
    private val prizeTable = listOf(
        prize(rangeStart = 0, rangeEndExclusive = 20, configKey = 0x00000001, name = "dummy-low"),
        prize(rangeStart = 20, rangeEndExclusive = 25, configKey = 0x5CA1E4A8, name = "コマじろう"),
        prize(rangeStart = 25, rangeEndExclusive = 82, configKey = 0x00000002, name = "dummy-mid"),
        prize(rangeStart = 82, rangeEndExclusive = 86, configKey = 0x778021C8, name = "まじめに生きる"),
        prize(rangeStart = 86, rangeEndExclusive = 100, configKey = 0x00000003, name = "dummy-high"),
    )
    private val predictor = GashaPredictor(prizeTable)

    @Test
    fun predictsKnownMomoCoinSampleDraws() {
        val entry = entry(
            0x7519C013,
            0xF194DE8E,
            0x0E19BF02,
            0xE20CFE0D,
        )

        val predictions = predictor.predict(entry, 2)

        assertEquals("まじめに生きる", predictions[0].prize.name)
        assertEquals(85, predictions[0].oldWModulo)
        assertEquals(listOf(0xF194DE8EL, 0x0E19BF02L, 0xE20CFE0DL, 0x59AEA307L), predictions[0].afterWords)
        assertEquals("コマじろう", predictions[1].prize.name)
        assertEquals(23, predictions[1].oldWModulo)
        assertEquals(listOf(0x0E19BF02L, 0xE20CFE0DL, 0x59AEA307L, 0x0E996612L), predictions[1].afterWords)
    }

    @Test
    fun advancesStateToTargetPrize() {
        val entry = entry(
            0x7519C013,
            0xF194DE8E,
            0x0E19BF02,
            0xE20CFE0D,
        )

        val advanceCount = predictor.advanceCountUntilPrize(entry, 0x5CA1E4A8, maxSteps = 10)
        val advanced = predictor.advance(entry, advanceCount!!)

        assertEquals(1, advanceCount)
        assertEquals("コマじろう", predictor.predict(advanced, 1).single().prize.name)
    }

    @Test
    fun returnsNullWhenTargetPrizeIsNotFound() {
        val entry = entry(1, 2, 3, 4)

        assertNull(predictor.advanceCountUntilPrize(entry, 0xFFFF0000, maxSteps = 5))
    }

    private fun entry(x: Long, y: Long, z: Long, w: Long) = GashaStateEntry(
        index = 3,
        words = listOf(x, y, z, w),
        rawEntry = GashaPredictor.encodeWords(listOf(x, y, z, w)),
    )

    private fun prize(
        rangeStart: Int,
        rangeEndExclusive: Int,
        configKey: Long,
        name: String,
    ) = GashaPrizeEntry(
        slot = 3,
        rateListKey = 0xEAE34BF3,
        rangeStart = rangeStart,
        rangeEndExclusive = rangeEndExclusive,
        configKey = configKey,
        prizeKind = "item",
        prizeId = configKey,
        identifier = name,
        name = name,
        classWeights = listOf(0, 100, 0, 0, 0),
    )
}
