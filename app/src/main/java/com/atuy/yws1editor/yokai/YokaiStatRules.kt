package com.atuy.yws1editor.yokai

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
