package com.atuy.yws1editor.yokai

import java.io.IOException

data class YokaiEncyclopediaEntry(
    val id: Long,
    val number: Int,
    val name: String,
    val met: Boolean,
    val owned: Boolean,
    val isNew: Boolean,
)

class YokaiEncyclopediaCodec {
    companion object {
        const val FLAG_PAYLOAD_OFFSET = 0x00F4
        const val FLAG_PAYLOAD_SIZE = 0x0500
        const val MET_FLAGS_OFFSET = FLAG_PAYLOAD_OFFSET + 0x0400
        const val OWNED_FLAGS_OFFSET = FLAG_PAYLOAD_OFFSET + 0x0420
        const val NEW_FLAGS_OFFSET = FLAG_PAYLOAD_OFFSET + 0x0440
        const val FLAG_SET_SIZE = 0x20
    }

    fun decode(gameData: ByteArray, masterData: YokaiMasterData): List<YokaiEncyclopediaEntry> {
        requireFlagRanges(gameData)
        return masterData.nameById.mapNotNull { (id, name) ->
            val number = masterData.numberById[id] ?: return@mapNotNull null
            if (!isSupportedNumber(number)) return@mapNotNull null
            YokaiEncyclopediaEntry(
                id = id,
                number = number,
                name = name,
                met = readFlag(gameData, MET_FLAGS_OFFSET, number),
                owned = readFlag(gameData, OWNED_FLAGS_OFFSET, number),
                isNew = readFlag(gameData, NEW_FLAGS_OFFSET, number),
            )
        }.sortedBy { it.number }
    }

    fun applyEntries(
        gameData: ByteArray,
        entries: List<YokaiEncyclopediaEntry>,
    ): ByteArray {
        requireFlagRanges(gameData)
        val out = gameData.copyOf()
        entries.forEach { entry ->
            if (!isSupportedNumber(entry.number)) {
                throw IOException("妖怪大辞典番号が不正です: ${entry.number}")
            }
            writeFlag(out, MET_FLAGS_OFFSET, entry.number, entry.met)
            writeFlag(out, OWNED_FLAGS_OFFSET, entry.number, entry.owned)
            writeFlag(out, NEW_FLAGS_OFFSET, entry.number, entry.isNew)
        }
        return out
    }

    private fun requireFlagRanges(gameData: ByteArray) {
        SaveDataBinary.requireRange(gameData, FLAG_PAYLOAD_OFFSET, FLAG_PAYLOAD_SIZE, "FlagStatus領域")
        SaveDataBinary.requireRange(gameData, MET_FLAGS_OFFSET, FLAG_SET_SIZE, "妖怪大辞典の遭遇flag")
        SaveDataBinary.requireRange(gameData, OWNED_FLAGS_OFFSET, FLAG_SET_SIZE, "妖怪大辞典の所持flag")
        SaveDataBinary.requireRange(gameData, NEW_FLAGS_OFFSET, FLAG_SET_SIZE, "妖怪大辞典のNEW flag")
    }

    private fun isSupportedNumber(number: Int): Boolean {
        return number in 1 until FLAG_SET_SIZE * 8
    }

    private fun readFlag(data: ByteArray, baseOffset: Int, number: Int): Boolean {
        val offset = baseOffset + number / 8
        val mask = 1 shl (number % 8)
        return (data[offset].toInt() and mask) != 0
    }

    private fun writeFlag(data: ByteArray, baseOffset: Int, number: Int, enabled: Boolean) {
        val offset = baseOffset + number / 8
        val mask = 1 shl (number % 8)
        val current = data[offset].toInt() and 0xff
        data[offset] = if (enabled) {
            (current or mask).toByte()
        } else {
            (current and mask.inv()).toByte()
        }
    }
}
