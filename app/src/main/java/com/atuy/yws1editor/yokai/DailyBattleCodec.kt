package com.atuy.yws1editor.yokai

import java.io.IOException

data class DailyBattleDefinition(
    val name: String,
    val flagIndex: Int,
) {
    val byteOffset: Int
        get() = DailyBattleCodec.GLOBAL_FLAG_DATA_OFFSET + flagIndex / Byte.SIZE_BITS

    val bitMask: Int
        get() = 1 shl (flagIndex and 7)
}

data class DailyBattleEntry(
    val definition: DailyBattleDefinition,
    val foughtToday: Boolean,
)

/**
 * Edits only the once-per-day battle flags stored in ywFlagStatus's global bit array.
 *
 * The friendship/event progression flags next to these bits are intentionally left untouched.
 */
class DailyBattleCodec {
    companion object {
        const val GLOBAL_FLAG_DATA_OFFSET = 0xF4
        const val GLOBAL_FLAG_DATA_SIZE = 0x500

        val DEFINITIONS: List<DailyBattleDefinition> = listOf(
            DailyBattleDefinition("キュウビ", 73),
            DailyBattleDefinition("くしゃ武者", 74),
            DailyBattleDefinition("天狗", 75),
            DailyBattleDefinition("だいだらぼっち", 76),
            DailyBattleDefinition("ヤミまろ", 522),
            DailyBattleDefinition("黄泉ゲンスイ", 527),
            DailyBattleDefinition("死神鳥", 532),
            DailyBattleDefinition("サファイニャン", 537),
            DailyBattleDefinition("エメラルニャン", 542),
            DailyBattleDefinition("ルビーニャン", 547),
            DailyBattleDefinition("トパニャン", 552),
            DailyBattleDefinition("ダイヤニャン", 557),
            DailyBattleDefinition("こめ爺", 562),
        )
    }

    init {
        check(DEFINITIONS.size == 13)
        check(DEFINITIONS.map { it.flagIndex }.distinct().size == DEFINITIONS.size)
    }

    fun decode(gameData: ByteArray): List<DailyBattleEntry> {
        requireFlagRegion(gameData)
        return DEFINITIONS.map { definition ->
            val value = gameData[definition.byteOffset].toInt() and 0xff
            DailyBattleEntry(
                definition = definition,
                foughtToday = value and definition.bitMask != 0,
            )
        }
    }

    fun apply(gameData: ByteArray, entries: List<DailyBattleEntry>): ByteArray {
        requireFlagRegion(gameData)
        val states = entries.associate { it.definition.flagIndex to it.foughtToday }
        val unknownFlags = states.keys - DEFINITIONS.mapTo(mutableSetOf()) { it.flagIndex }
        if (unknownFlags.isNotEmpty()) {
            throw IOException("未対応の日次戦闘フラグです: ${unknownFlags.sorted().joinToString()}")
        }

        return gameData.copyOf().also { output ->
            DEFINITIONS.forEach { definition ->
                val fought = states[definition.flagIndex] ?: return@forEach
                val oldValue = output[definition.byteOffset].toInt() and 0xff
                val newValue = if (fought) {
                    oldValue or definition.bitMask
                } else {
                    oldValue and definition.bitMask.inv()
                }
                output[definition.byteOffset] = newValue.toByte()
            }
        }
    }

    fun setAll(entries: List<DailyBattleEntry>, foughtToday: Boolean): List<DailyBattleEntry> =
        entries.map { it.copy(foughtToday = foughtToday) }

    private fun requireFlagRegion(gameData: ByteArray) {
        SaveDataBinary.requireRange(
            gameData,
            GLOBAL_FLAG_DATA_OFFSET,
            GLOBAL_FLAG_DATA_SIZE,
            "グローバルフラグ領域",
        )
    }
}
