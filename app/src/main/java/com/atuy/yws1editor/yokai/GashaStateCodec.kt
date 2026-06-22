package com.atuy.yws1editor.yokai

import java.io.IOException

data class GashaStateEntry(
    val index: Int,
    val words: List<Long>,
    val rawEntry: ByteArray,
)

class GashaStateCodec {
    companion object {
        const val OFFSET = 0xA68C
        const val ENTRY_COUNT = 32
        const val STRIDE = 16
        const val REGION_SIZE = ENTRY_COUNT * STRIDE
    }

    fun decode(gameData: ByteArray): List<GashaStateEntry> {
        SaveDataBinary.requireRange(gameData, OFFSET, REGION_SIZE, "ガシャstate領域")
        return List(ENTRY_COUNT) { index ->
            val offset = OFFSET + index * STRIDE
            GashaStateEntry(
                index = index,
                words = List(4) { word -> SaveDataBinary.readUInt32Le(gameData, offset + word * 4) },
                rawEntry = gameData.copyOfRange(offset, offset + STRIDE),
            )
        }
    }

    fun backup(gameData: ByteArray): ByteArray {
        SaveDataBinary.requireRange(gameData, OFFSET, REGION_SIZE, "ガシャstate領域")
        return gameData.copyOfRange(OFFSET, OFFSET + REGION_SIZE)
    }

    fun restore(gameData: ByteArray, backup: ByteArray): ByteArray {
        SaveDataBinary.requireRange(gameData, OFFSET, REGION_SIZE, "ガシャstate領域")
        if (backup.size != REGION_SIZE) throw IOException("ガシャstateバックアップのサイズが不正です")
        return gameData.copyOf().also { backup.copyInto(it, OFFSET) }
    }
}
