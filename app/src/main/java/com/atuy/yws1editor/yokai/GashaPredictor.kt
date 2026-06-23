package com.atuy.yws1editor.yokai

import java.io.IOException

data class GashaPrizeEntry(
    val slot: Int,
    val rateListKey: Long,
    val rangeStart: Int,
    val rangeEndExclusive: Int,
    val configKey: Long,
    val prizeKind: String,
    val prizeId: Long,
    val identifier: String,
    val name: String,
    val classWeights: List<Int>,
) {
    val weight: Int get() = rangeEndExclusive - rangeStart
}

data class GashaPrediction(
    val drawIndex: Int,
    val beforeWords: List<Long>,
    val afterWords: List<Long>,
    val oldWModulo: Int,
    val classModulo: Int,
    val classIndex: Int,
    val prize: GashaPrizeEntry,
)

class GashaPredictor(
    prizeEntries: List<GashaPrizeEntry>,
) {
    private val entriesBySlot = prizeEntries.groupBy { it.slot }
    private val totalsBySlot = entriesBySlot.mapValues { (_, entries) ->
        entries.maxOfOrNull { it.rangeEndExclusive } ?: 0
    }

    fun entriesForSlot(slot: Int): List<GashaPrizeEntry> = entriesBySlot[slot].orEmpty()

    fun predict(entry: GashaStateEntry, count: Int): List<GashaPrediction> {
        if (count < 0) throw IOException("予測回数が不正です")
        val predictions = mutableListOf<GashaPrediction>()
        var words = normalizeWords(entry.words)
        repeat(count) { index ->
            val prediction = predictNext(entry.index, words, drawIndex = index + 1)
            predictions += prediction
            words = prediction.afterWords
        }
        return predictions
    }

    fun advance(entry: GashaStateEntry, steps: Int): GashaStateEntry {
        if (steps < 0) throw IOException("ガシャstateを戻す操作は実装していません")
        var words = normalizeWords(entry.words)
        repeat(steps) { words = advanceWords(words) }
        return entry.copy(words = words, rawEntry = encodeWords(words))
    }

    fun advanceCountUntilPrize(
        entry: GashaStateEntry,
        configKey: Long,
        maxSteps: Int = 10_000,
    ): Int? {
        if (maxSteps < 0) throw IOException("探索回数が不正です")
        var words = normalizeWords(entry.words)
        for (drawIndex in 1..maxSteps) {
            val prediction = predictNext(entry.index, words, drawIndex)
            if (prediction.prize.configKey == configKey) {
                return drawIndex - 1
            }
            words = prediction.afterWords
        }
        return null
    }

    private fun predictNext(slot: Int, words: List<Long>, drawIndex: Int): GashaPrediction {
        val entries = entriesBySlot[slot].orEmpty()
        val rateTotal = totalsBySlot[slot] ?: 0
        if (entries.isEmpty() || rateTotal <= 0) {
            throw IOException("slot $slot のガシャ景品テーブルがありません")
        }
        val oldWModulo = (words[3] % rateTotal).toInt()
        val prize = entries.firstOrNull {
            oldWModulo >= it.rangeStart && oldWModulo < it.rangeEndExclusive
        } ?: throw IOException("slot $slot の景品rangeが不正です")
        val afterWords = advanceWords(words)
        val classTotal = prize.classWeights.sum()
        val classModulo = if (classTotal > 0) (afterWords[3] % classTotal).toInt() else 0
        val classIndex = prize.classWeights.runningFold(0, Int::plus)
            .zipWithNext()
            .indexOfFirst { (start, end) -> classModulo >= start && classModulo < end }
            .takeIf { it >= 0 } ?: -1
        return GashaPrediction(
            drawIndex = drawIndex,
            beforeWords = words,
            afterWords = afterWords,
            oldWModulo = oldWModulo,
            classModulo = classModulo,
            classIndex = classIndex,
            prize = prize,
        )
    }

    companion object {
        fun advanceWords(words: List<Long>): List<Long> {
            val normalized = normalizeWords(words)
            val x = normalized[0]
            val y = normalized[1]
            val z = normalized[2]
            val w = normalized[3]
            val t = (x xor ((x shl 11) and 0xFFFF_FFFFL)) and 0xFFFF_FFFFL
            val next = (w xor (w ushr 19) xor t xor (t ushr 8)) and 0xFFFF_FFFFL
            return listOf(y, z, w, next)
        }

        fun encodeWords(words: List<Long>): ByteArray {
            val normalized = normalizeWords(words)
            val out = ByteArray(16)
            normalized.forEachIndexed { wordIndex, value ->
                repeat(4) { byteIndex ->
                    out[wordIndex * 4 + byteIndex] = ((value ushr (byteIndex * 8)) and 0xff).toByte()
                }
            }
            return out
        }

        private fun normalizeWords(words: List<Long>): List<Long> {
            if (words.size != 4) throw IOException("ガシャstate word数が不正です")
            return words.map { it and 0xFFFF_FFFFL }
        }
    }
}
