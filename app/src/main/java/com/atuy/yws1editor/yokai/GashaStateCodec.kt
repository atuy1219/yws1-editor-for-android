package com.atuy.yws1editor.yokai

import java.io.IOException

data class GashaStateEntry(
    val index: Int,
    val words: List<Long>,
    val rawEntry: ByteArray,
)

data class GashaSlotInfo(
    val index: Int,
    val name: String,
    val rateListKey: Long?,
    val note: String,
)

class GashaStateCodec {
    companion object {
        const val OFFSET = 0xA68C
        const val ENTRY_COUNT = 32
        const val STRIDE = 16
        const val REGION_SIZE = ENTRY_COUNT * STRIDE

        val SLOT_INFO: Map<Int, GashaSlotInfo> = listOf(
            GashaSlotInfo(0, "赤コイン", 0x9A89BF7CL, "通常色コイン"),
            GashaSlotInfo(1, "黄色コイン", 0x0380EEC6L, "通常色コイン"),
            GashaSlotInfo(2, "オレンジコイン", 0x7487DE50L, "通常色コイン"),
            GashaSlotInfo(3, "桃コイン", 0xEAE34BF3L, "サンプルで確認済み"),
            GashaSlotInfo(4, "緑コイン", 0x9DE47B65L, "通常色コイン"),
            GashaSlotInfo(5, "青コイン", 0x04ED2ADFL, "通常色コイン"),
            GashaSlotInfo(6, "紫コイン", 0x73EA1A49L, "通常色コイン"),
            GashaSlotInfo(7, "水色コイン", 0xE35507D8L, "通常色コイン"),
            GashaSlotInfo(8, "わくわくコイン", 0xBC514DBAL, "テーブル内容から確定"),
            GashaSlotInfo(9, "5つ星コイン", 0xCB567D2CL, "テーブル内容から確定"),
            GashaSlotInfo(10, "スペシャルコイン", 0x5BE960BDL, "テーブル内容から確定"),
            GashaSlotInfo(11, "サファイアコイン", 0xBB3C89A3L, "スマホ版未使用・3DS版名残"),
            GashaSlotInfo(12, "エメラルドコイン", 0xCC3BB935L, "スマホ版未使用・3DS版名残"),
            GashaSlotInfo(13, "ルビーコイン", 0x525F2C96L, "スマホ版未使用・3DS版名残"),
            GashaSlotInfo(14, "トパーズコイン", 0x25581C00L, "スマホ版未使用・3DS版名残"),
            GashaSlotInfo(15, "ダイヤコイン", 0x2235D819L, "スマホ版未使用・3DS版名残"),
            GashaSlotInfo(16, "一日一回ガチャ", 0x0000000AL, "一日一回ガチャ候補"),
        ).associateBy { it.index }
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
