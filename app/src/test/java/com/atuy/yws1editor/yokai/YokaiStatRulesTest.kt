package com.atuy.yws1editor.yokai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YokaiStatRulesTest {
    @Test
    fun ivaCellMaximumMatchesOneAndTwoStatClasses() {
        assertEquals(0, YokaiStatRules.ivaCellMax(0))
        assertEquals(8, YokaiStatRules.ivaCellMax(1))
        assertEquals(8, YokaiStatRules.ivaCellMax(4))
        assertEquals(4, YokaiStatRules.ivaCellMax(5))
        assertEquals(4, YokaiStatRules.ivaCellMax(6))
        assertEquals(4, YokaiStatRules.ivaCellMax(7))
        assertEquals(8, YokaiStatRules.ivaCellMax(8))
    }

    @Test
    fun normalizeIvaMasksNonClassStatsAndCapsTwoStatClassesAtFour() {
        val normalized = YokaiStatRules.normalizeIva(
            Stat5(hp = 9, power = 9, spirit = 9, defense = 9, speed = 9),
            yokaiClass = 5,
        )

        assertEquals(Stat5(0, 0, 4, 4, 0), normalized)
        assertEquals(8, normalized.values().sum())
    }

    @Test
    fun normalizeIvb1AlwaysProducesNaturalRangeAndExactTotal() {
        val normalized = YokaiStatRules.normalizeIvb1(Stat5(15, -3, 1, 1, 1))

        assertEquals(10, normalized.values().sum())
        assertTrue(normalized.values().all { it in 0..5 })
    }

    @Test
    fun updateIvb1KeepsRequestedCellAndRebalancesOtherCells() {
        val updated = YokaiStatRules.updateIvb1(Stat5(2, 2, 2, 2, 2), index = 0, requested = 5)

        assertEquals(5, updated.hp)
        assertEquals(10, updated.values().sum())
        assertTrue(updated.values().all { it in 0..5 })
    }

    @Test
    fun updateIvb1CanLowerRequestedCellWithoutBreakingTotal() {
        val updated = YokaiStatRules.updateIvb1(Stat5(5, 5, 0, 0, 0), index = 0, requested = 0)

        assertEquals(0, updated.hp)
        assertEquals(10, updated.values().sum())
        assertTrue(updated.values().all { it in 0..5 })
    }
}
