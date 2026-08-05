from pathlib import Path


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected exactly one match, found {count}")
    return text.replace(old, new, 1)


root = Path(__file__).resolve().parents[2]

rules_path = root / "app/src/main/java/com/atuy/yws1editor/yokai/YokaiStatRules.kt"
rules_path.write_text(
    '''package com.atuy.yws1editor.yokai

object YokaiStatRules {
    const val IVA_TOTAL_MAX = 8
    const val IVB1_CELL_MAX = 5
    const val IVB1_TOTAL = 10
    const val PACKED_IVB_CELL_MAX = 15

    // H, A, M, D, S
    private val ivaEditableByClass: Map<Int, List<Boolean>> = mapOf(
        0 to listOf(false, false, false, false, false),
        1 to listOf(false, true, false, false, false),
        2 to listOf(false, false, true, false, false),
        3 to listOf(false, false, false, true, false),
        4 to listOf(false, false, false, false, true),
        5 to listOf(false, false, true, true, false),
        6 to listOf(true, false, false, false, true),
        7 to listOf(false, true, true, false, false),
        8 to listOf(true, false, false, false, false),
    )

    fun ivaEditableMask(yokaiClass: Int?): List<Boolean> {
        return ivaEditableByClass[yokaiClass] ?: listOf(true, true, true, true, true)
    }

    fun ivaCellMax(yokaiClass: Int?): Int {
        return when (yokaiClass) {
            0 -> 0
            5, 6, 7 -> 4
            else -> IVA_TOTAL_MAX
        }
    }

    fun normalizeIva(stat: Stat5, yokaiClass: Int?): Stat5 {
        val mask = ivaEditableMask(yokaiClass)
        val cellMax = ivaCellMax(yokaiClass)
        val values = stat.values().mapIndexed { index, value ->
            if (mask.getOrElse(index) { false }) value.coerceIn(0, cellMax) else 0
        }.toMutableList()

        var remaining = IVA_TOTAL_MAX
        for (index in values.indices) {
            val allowed = values[index].coerceAtMost(remaining)
            values[index] = allowed
            remaining -= allowed
        }
        return statFromValues(values)
    }

    fun normalizeIvb1(stat: Stat5): Stat5 {
        val values = stat.values()
            .map { it.coerceIn(0, IVB1_CELL_MAX) }
            .toMutableList()
        rebalanceToTotal(values, protectedIndex = null)
        return statFromValues(values)
    }

    fun updateIvb1(stat: Stat5, index: Int, requested: Int): Stat5 {
        if (index !in 0..4) return stat
        val values = normalizeIvb1(stat).values().toMutableList()
        values[index] = requested.coerceIn(0, IVB1_CELL_MAX)
        rebalanceToTotal(values, protectedIndex = index)
        return statFromValues(values)
    }

    private fun rebalanceToTotal(values: MutableList<Int>, protectedIndex: Int?) {
        var total = values.sum()
        if (total > IVB1_TOTAL) {
            var excess = total - IVB1_TOTAL
            candidateIndices(values, protectedIndex, adding = false).forEach { index ->
                if (excess == 0) return@forEach
                val removed = values[index].coerceAtMost(excess)
                values[index] -= removed
                excess -= removed
            }
        } else if (total < IVB1_TOTAL) {
            var deficit = IVB1_TOTAL - total
            candidateIndices(values, protectedIndex, adding = true).forEach { index ->
                if (deficit == 0) return@forEach
                val capacity = IVB1_CELL_MAX - values[index]
                val added = capacity.coerceAtMost(deficit)
                values[index] += added
                deficit -= added
            }
        }
    }

    private fun candidateIndices(
        values: List<Int>,
        protectedIndex: Int?,
        adding: Boolean,
    ): List<Int> {
        return values.indices
            .filter { it != protectedIndex }
            .sortedWith(
                compareByDescending<Int> { index ->
                    if (adding) IVB1_CELL_MAX - values[index] else values[index]
                }.thenBy { it },
            )
    }

    private fun statFromValues(values: List<Int>): Stat5 {
        return Stat5(
            hp = values.getOrElse(0) { 0 },
            power = values.getOrElse(1) { 0 },
            spirit = values.getOrElse(2) { 0 },
            defense = values.getOrElse(3) { 0 },
            speed = values.getOrElse(4) { 0 },
        )
    }
}
''',
    encoding="utf-8",
)

view_model_path = root / "app/src/main/java/com/atuy/yws1editor/MainViewModel.kt"
view_model = view_model_path.read_text(encoding="utf-8")
view_model = replace_once(
    view_model,
    "import com.atuy.yws1editor.yokai.YokaiParser\n",
    "import com.atuy.yws1editor.yokai.YokaiParser\nimport com.atuy.yws1editor.yokai.YokaiStatRules\n",
    "MainViewModel import",
)
view_model = replace_once(
    view_model,
    '''        private const val NORMAL_LEVEL_MAX = 99
        private const val NORMAL_IVA_TOTAL_MAX = 8
        private const val NORMAL_IVB1_TOTAL_MAX = 10
        private const val NORMAL_CB_TOTAL_MAX = 20
        private const val NORMAL_IVB_MAX = 15

        // H, A, M, D, S
        private val IVA_EDITABLE_BY_CLASS: Map<Int, List<Boolean>> = mapOf(
            0 to listOf(false, false, false, false, false),
            1 to listOf(false, true, false, false, false),
            2 to listOf(false, false, true, false, false),
            3 to listOf(false, false, false, true, false),
            4 to listOf(false, false, false, false, true),
            5 to listOf(false, false, true, true, false),
            6 to listOf(true, false, false, false, true),
            7 to listOf(false, true, true, false, false),
            8 to listOf(true, false, false, false, false),
        )
''',
    '''        private const val NORMAL_LEVEL_MAX = 99
        private const val NORMAL_CB_TOTAL_MAX = 20
''',
    "MainViewModel constants",
)
view_model = replace_once(
    view_model,
    '''                    val updated = applyStatUpdate(
                        stat = masked,
                        index = index,
                        requested = value,
                        cellMax = SIGNED_STAT_MAX,
                        totalMax = NORMAL_IVA_TOTAL_MAX,
                    )
''',
    '''                    val updated = applyStatUpdate(
                        stat = masked,
                        index = index,
                        requested = value,
                        cellMax = YokaiStatRules.ivaCellMax(entry.yokaiClass),
                        totalMax = YokaiStatRules.IVA_TOTAL_MAX,
                    )
''',
    "IVA update limits",
)
view_model = replace_once(
    view_model,
    '''                StatGroup.IVB1 -> {
                    val updated = applyStatUpdate(
                        stat = entry.ivb1,
                        index = index,
                        requested = value,
                        cellMax = NORMAL_IVB_MAX,
                        totalMax = if (isCheatMode) null else NORMAL_IVB1_TOTAL_MAX,
                    )
                    entry.copy(ivb1 = updated)
                }
''',
    '''                StatGroup.IVB1 -> {
                    val updated = if (isCheatMode) {
                        applyStatUpdate(
                            stat = entry.ivb1,
                            index = index,
                            requested = value,
                            cellMax = YokaiStatRules.PACKED_IVB_CELL_MAX,
                            totalMax = null,
                        )
                    } else {
                        YokaiStatRules.updateIvb1(entry.ivb1, index, value)
                    }
                    entry.copy(ivb1 = updated)
                }
''',
    "IVB1 update rules",
)
view_model = view_model.replace(
    "cellMax = NORMAL_IVB_MAX,",
    "cellMax = YokaiStatRules.PACKED_IVB_CELL_MAX,",
)
view_model = replace_once(
    view_model,
    '''        val ivaMask = ivaEditableMask(entry.yokaiClass)
        val normalizedIva = normalizeStatForNormal(
            applyIvaMask(entry.iva, ivaMask),
            SIGNED_STAT_MAX,
            NORMAL_IVA_TOTAL_MAX,
        )
        val normalizedIvb1 = normalizeStatForNormal(entry.ivb1, NORMAL_IVB_MAX, NORMAL_IVB1_TOTAL_MAX)
        val normalizedIvb2 = normalizeStatForNormal(entry.ivb2, NORMAL_IVB_MAX, totalMax = null)
''',
    '''        val normalizedIva = YokaiStatRules.normalizeIva(entry.iva, entry.yokaiClass)
        val normalizedIvb1 = YokaiStatRules.normalizeIvb1(entry.ivb1)
        val normalizedIvb2 = normalizeStatForNormal(
            entry.ivb2,
            YokaiStatRules.PACKED_IVB_CELL_MAX,
            totalMax = null,
        )
''',
    "normal-mode normalization",
)
view_model = replace_once(
    view_model,
    '''    private fun ivaEditableMask(yokaiClass: Int?): List<Boolean> {
        return IVA_EDITABLE_BY_CLASS[yokaiClass] ?: listOf(true, true, true, true, true)
    }
''',
    '''    private fun ivaEditableMask(yokaiClass: Int?): List<Boolean> {
        return YokaiStatRules.ivaEditableMask(yokaiClass)
    }
''',
    "IVA mask delegation",
)
view_model_path.write_text(view_model, encoding="utf-8")

activity_path = root / "app/src/main/java/com/atuy/yws1editor/MainActivity.kt"
activity = activity_path.read_text(encoding="utf-8")
activity = replace_once(
    activity,
    "import com.atuy.yws1editor.yokai.YokaiStatusCalculator\n",
    "import com.atuy.yws1editor.yokai.YokaiStatusCalculator\nimport com.atuy.yws1editor.yokai.YokaiStatRules\n",
    "MainActivity import",
)
activity = replace_once(
    activity,
    '''private val IVA_EDITABLE_BY_CLASS: Map<Int, List<Boolean>> = mapOf(
    0 to listOf(false, false, false, false, false),
    1 to listOf(false, true, false, false, false),
    2 to listOf(false, false, true, false, false),
    3 to listOf(false, false, false, true, false),
    4 to listOf(false, false, false, false, true),
    5 to listOf(false, false, true, true, false),
    6 to listOf(true, false, false, false, true),
    7 to listOf(false, true, true, false, false),
    8 to listOf(true, false, false, false, false),
)

''',
    "",
    "MainActivity duplicate IVA map",
)
activity = replace_once(
    activity,
    "        IVA_EDITABLE_BY_CLASS[entry.yokaiClass] ?: listOf(true, true, true, true, true)\n",
    "        YokaiStatRules.ivaEditableMask(entry.yokaiClass)\n",
    "MainActivity IVA mask",
)
activity = replace_once(
    activity,
    '''    val ivaEditableMask = if (isCheatMode) {
        null
    } else {
        YokaiStatRules.ivaEditableMask(entry.yokaiClass)
    }

    Column(
''',
    '''    val ivaEditableMask = if (isCheatMode) {
        null
    } else {
        YokaiStatRules.ivaEditableMask(entry.yokaiClass)
    }
    val ivaCellMax = if (isCheatMode) {
        ivaInputMax
    } else {
        YokaiStatRules.ivaCellMax(entry.yokaiClass)
    }

    Column(
''',
    "MainActivity IVA cell max",
)
activity = replace_once(
    activity,
    "            max = ivaInputMax,\n",
    "            max = ivaCellMax,\n",
    "MainActivity IVA input max",
)
activity = replace_once(
    activity,
    '''        StatusEditableRow(label = "IVB1", stat = entry.ivb1, max = 15, onValueChange = { i, v -> onStatChange(StatGroup.IVB1, i, v) })
        StatusEditableRow(label = "IVB2", stat = entry.ivb2, max = 15, onValueChange = { i, v -> onStatChange(StatGroup.IVB2, i, v) })
''',
    '''        StatusEditableRow(
            label = "IVB1",
            stat = entry.ivb1,
            max = if (isCheatMode) YokaiStatRules.PACKED_IVB_CELL_MAX else YokaiStatRules.IVB1_CELL_MAX,
            onValueChange = { i, v -> onStatChange(StatGroup.IVB1, i, v) },
        )
        StatusEditableRow(
            label = "IVB2",
            stat = entry.ivb2,
            max = YokaiStatRules.PACKED_IVB_CELL_MAX,
            onValueChange = { i, v -> onStatChange(StatGroup.IVB2, i, v) },
        )
''',
    "MainActivity IVB limits",
)
activity_path.write_text(activity, encoding="utf-8")

test_path = root / "app/src/test/java/com/atuy/yws1editor/yokai/YokaiStatRulesTest.kt"
test_path.write_text(
    '''package com.atuy.yws1editor.yokai

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
''',
    encoding="utf-8",
)
