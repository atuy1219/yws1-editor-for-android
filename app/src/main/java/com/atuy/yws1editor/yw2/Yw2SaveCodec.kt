package com.atuy.yws1editor.yw2

import java.io.IOException
import java.nio.charset.Charset

data class Yw2Stats(
    val hp: Int,
    val power: Int,
    val spirit: Int,
    val defense: Int,
    val speed: Int,
) {
    fun values(): List<Int> = listOf(hp, power, spirit, defense, speed)
}

data class Yw2Yokai(
    val slot: Int,
    val num1: Int,
    val num2: Int,
    val typeId: Long,
    val nickname: String,
    val attackLevel: Int,
    val techniqueLevel: Int,
    val soultimateLevel: Int,
    val experience: Long,
    val ownerId: Long,
    val iv: Yw2Stats,
    val ev: Yw2Stats,
    val sportsClub: Yw2Stats,
    val level: Int,
    val loafLevel: Int,
    val attitude: Int,
)

enum class Yw2InventoryKind(
    val label: String,
    val sectionId: Int,
    val recordSize: Int,
    val maxEntries: Int,
) {
    ITEM("どうぐ", 0x04, 0x0C, 430),
    EQUIPMENT("そうび", 0x05, 0x10, 90),
    IMPORTANT("だいじなもの", 0x06, 0x08, 180),
    SOUL("魂", 0x13, 0x0C, 100),
}

data class Yw2InventoryEntry(
    val kind: Yw2InventoryKind,
    val slot: Int,
    val num1: Int,
    val num2: Int,
    val typeId: Long,
    val amount: Int = 1,
    val used: Int = 0,
    val experience: Int = 0,
    val level: Int = 0,
)

object Yw2SaveCodec {
    const val YOKAI_RECORD_SIZE = 0x5C
    const val YOKAI_MAX = 406
    private const val YOKAI_SECTION_ID = 0x07
    private const val MONEY_SECTION_ID = 0x09
    private const val MONEY_SECTION_OFFSET = 0x14

    fun parseYokai(data: ByteArray): List<Yw2Yokai> {
        val base = sectionDataStart(data, YOKAI_SECTION_ID, YOKAI_RECORD_SIZE * YOKAI_MAX)
        val out = ArrayList<Yw2Yokai>()
        for (slot in 0 until YOKAI_MAX) {
            val o = base + slot * YOKAI_RECORD_SIZE
            if (o + YOKAI_RECORD_SIZE > data.size) break
            val typeId = readU32(data, o + 4)
            if (typeId == 0L) continue
            val num2 = readU16(data, o + 2)
            val packed = data[o + 84].toInt() and 0xFF
            out += Yw2Yokai(
                slot = slot,
                num1 = readU16(data, o),
                num2 = num2,
                typeId = typeId,
                nickname = readGameString(data, o + 8, 24),
                attackLevel = data[o + 42].toInt() and 0xFF,
                techniqueLevel = data[o + 46].toInt() and 0xFF,
                soultimateLevel = data[o + 50].toInt() and 0xFF,
                experience = readU32(data, o + 52),
                ownerId = readU32(data, o + 60),
                iv = readStats(data, o + 64),
                ev = readStats(data, o + 69),
                sportsClub = readSignedStats(data, o + 74),
                level = data[o + 79].toInt() and 0xFF,
                loafLevel = (packed ushr 4) and 0xF,
                attitude = packed and 0xF,
            )
        }
        return out
    }

    fun updateYokai(data: ByteArray, value: Yw2Yokai): ByteArray {
        validateIv(value.iv)
        validateEv(value.ev)
        if (value.slot !in 0 until YOKAI_MAX) throw IOException("妖怪スロットが範囲外です")
        val base = sectionDataStart(data, YOKAI_SECTION_ID, YOKAI_RECORD_SIZE * YOKAI_MAX)
        val o = base + value.slot * YOKAI_RECORD_SIZE
        if (o + YOKAI_RECORD_SIZE > data.size) throw IOException("妖怪レコードがセーブ範囲外です")

        return data.copyOf().also { out ->
            writeU16(out, o, value.num1)
            writeU16(out, o + 2, value.num2)
            writeU32(out, o + 4, value.typeId)
            writeGameString(out, o + 8, 24, value.nickname)
            out[o + 42] = value.attackLevel.coerceIn(0, 255).toByte()
            out[o + 46] = value.techniqueLevel.coerceIn(0, 255).toByte()
            out[o + 50] = value.soultimateLevel.coerceIn(0, 255).toByte()
            writeU32(out, o + 52, value.experience)
            writeU32(out, o + 60, value.ownerId)
            writeStats(out, o + 64, value.iv)
            writeStats(out, o + 69, value.ev)
            validateSportsClub(value.sportsClub)
            writeSignedStats(out, o + 74, value.sportsClub)
            out[o + 79] = value.level.coerceIn(1, 99).toByte()
            out[o + 84] = (
                ((value.loafLevel.coerceIn(0, 15) shl 4) or value.attitude.coerceIn(0, 15))
            ).toByte()
        }
    }

    fun parseInventory(data: ByteArray, kind: Yw2InventoryKind): List<Yw2InventoryEntry> {
        val base = sectionDataStart(data, kind.sectionId, kind.recordSize * kind.maxEntries)
        val out = ArrayList<Yw2InventoryEntry>()

        for (slot in 0 until kind.maxEntries) {
            val o = base + slot * kind.recordSize
            if (o + kind.recordSize > data.size) break

            val typeId = readU32(data, o + 4)
            if (typeId == 0L) continue

            val num1 = readU16(data, o)
            val num2 = readU16(data, o + 2)
            out += when (kind) {
                Yw2InventoryKind.ITEM -> Yw2InventoryEntry(
                    kind = kind,
                    slot = slot,
                    num1 = num1,
                    num2 = num2,
                    typeId = typeId,
                    amount = data[o + 8].toInt() and 0xFF,
                )
                Yw2InventoryKind.EQUIPMENT -> Yw2InventoryEntry(
                    kind = kind,
                    slot = slot,
                    num1 = num1,
                    num2 = num2,
                    typeId = typeId,
                    amount = data[o + 8].toInt() and 0xFF,
                    used = data[o + 12].toInt() and 0xFF,
                )
                Yw2InventoryKind.IMPORTANT -> Yw2InventoryEntry(
                    kind = kind,
                    slot = slot,
                    num1 = num1,
                    num2 = num2,
                    typeId = typeId,
                )
                Yw2InventoryKind.SOUL -> Yw2InventoryEntry(
                    kind = kind,
                    slot = slot,
                    num1 = num1,
                    num2 = num2,
                    typeId = typeId,
                    experience = readU16(data, o + 8),
                    level = data[o + 10].toInt() and 0xFF,
                    used = data[o + 11].toInt() and 0xFF,
                )
            }
        }
        return out
    }

    fun updateInventory(data: ByteArray, entry: Yw2InventoryEntry): ByteArray {
        val kind = entry.kind
        if (entry.slot !in 0 until kind.maxEntries) {
            throw IOException("アイテムスロットが範囲外です")
        }

        val base = sectionDataStart(data, kind.sectionId, kind.recordSize * kind.maxEntries)
        val o = base + entry.slot * kind.recordSize
        if (o + kind.recordSize > data.size) {
            throw IOException("アイテムレコードがセーブ範囲外です")
        }

        return data.copyOf().also { out ->
            writeU16(out, o, entry.num1)
            writeU16(out, o + 2, entry.num2)
            writeU32(out, o + 4, entry.typeId)
            when (kind) {
                Yw2InventoryKind.ITEM -> {
                    out[o + 8] = entry.amount.coerceIn(0, 255).toByte()
                }
                Yw2InventoryKind.EQUIPMENT -> {
                    out[o + 8] = entry.amount.coerceIn(0, 255).toByte()
                    out[o + 12] = entry.used.coerceIn(0, 255).toByte()
                }
                Yw2InventoryKind.IMPORTANT -> Unit
                Yw2InventoryKind.SOUL -> {
                    writeU16(out, o + 8, entry.experience.coerceIn(0, 65535))
                    out[o + 10] = entry.level.coerceIn(1, 10).toByte()
                    out[o + 11] = entry.used.coerceIn(0, 255).toByte()
                }
            }
        }
    }

    fun readMoney(data: ByteArray): Long {
        val base = sectionDataStart(data, MONEY_SECTION_ID, MONEY_SECTION_OFFSET + 4)
        return readU32(data, base + MONEY_SECTION_OFFSET)
    }

    fun updateMoney(data: ByteArray, money: Long): ByteArray {
        if (money !in 0..0xFFFF_FFFFL) throw IOException("所持金が範囲外です")
        val base = sectionDataStart(data, MONEY_SECTION_ID, MONEY_SECTION_OFFSET + 4)
        return data.copyOf().also {
            writeU32(it, base + MONEY_SECTION_OFFSET, money)
        }
    }

    fun validateIv(stats: Yw2Stats) {
        if (stats.values().any { it !in 0..255 }) throw IOException("個体値が0..255の範囲外です")
        if (stats.hp % 2 != 0) throw IOException("個体値HPは偶数である必要があります")
        val total = stats.hp / 2 + stats.power + stats.spirit + stats.defense + stats.speed
        if (total != 40) throw IOException("個体値は HP/2 + ちから + ようりょく + まもり + すばやさ = 40 にしてください")
    }

    fun validateEv(stats: Yw2Stats) {
        if (stats.values().any { it !in 0..255 }) throw IOException("育成値が0..255の範囲外です")
        if (stats.hp % 2 != 0) throw IOException("育成値HPは偶数である必要があります")
        val total = stats.hp / 2 + stats.power + stats.spirit + stats.defense + stats.speed
        if (total > 20) throw IOException("育成値は HP/2 + ちから + ようりょく + まもり + すばやさ <= 20 にしてください")
    }

    fun validateSportsClub(stats: Yw2Stats) {
        if (stats.values().any { it !in -10..25 }) {
            throw IOException("スポーツクラブ補正は各能力 -10..25 の範囲にしてください")
        }
    }

    private fun sectionDataStart(data: ByteArray, sectionId: Int, minimumSize: Int): Int {
        // Yw2Crypto.Decoded keeps a 0x20-byte compatibility header before the
        // decrypted save body. Yo-kai Watch 2 stores each domain as:
        //   FFFE.... / [size:24 | id:8] / section body / FEFF....
        // This matches ykw-editors' SaveManager::parseSavedata().
        val first = 0x20
        val last = data.size - 12
        if (last < first) throw IOException("YW2セーブ本体が短すぎます")

        for (header in first..last) {
            if (readU16(data, header) != 0xFFFE) continue

            val descriptor = readU32(data, header + 4)
            if ((descriptor and 0xFF).toInt() != sectionId) continue

            val size = (descriptor ushr 8).toInt()
            if (size < minimumSize) continue

            val body = header + 8
            val footer = body + size
            if (footer + 4 > data.size) continue
            if (readU16(data, footer) != 0xFEFF) continue

            return body
        }

        throw IOException(
            "YW2の${sectionId.toString(16).uppercase()}セクションを特定できません"
        )
    }

    private fun readStats(data: ByteArray, offset: Int) = Yw2Stats(
        hp = data[offset].toInt() and 0xFF,
        power = data[offset + 1].toInt() and 0xFF,
        spirit = data[offset + 2].toInt() and 0xFF,
        defense = data[offset + 3].toInt() and 0xFF,
        speed = data[offset + 4].toInt() and 0xFF,
    )

    private fun writeStats(data: ByteArray, offset: Int, value: Yw2Stats) {
        value.values().forEachIndexed { index, v -> data[offset + index] = v.coerceIn(0, 255).toByte() }
    }

    private fun readSignedStats(data: ByteArray, offset: Int) = Yw2Stats(
        hp = data[offset].toInt(),
        power = data[offset + 1].toInt(),
        spirit = data[offset + 2].toInt(),
        defense = data[offset + 3].toInt(),
        speed = data[offset + 4].toInt(),
    )

    private fun writeSignedStats(data: ByteArray, offset: Int, value: Yw2Stats) {
        value.values().forEachIndexed { index, v ->
            data[offset + index] = v.coerceIn(-128, 127).toByte()
        }
    }

    private fun readU16(data: ByteArray, offset: Int): Int =
        (data[offset].toInt() and 0xFF) or ((data[offset + 1].toInt() and 0xFF) shl 8)

    private fun writeU16(data: ByteArray, offset: Int, value: Int) {
        data[offset] = (value and 0xFF).toByte()
        data[offset + 1] = ((value ushr 8) and 0xFF).toByte()
    }

    private fun readU32(data: ByteArray, offset: Int): Long =
        (data[offset].toLong() and 0xFF) or
            ((data[offset + 1].toLong() and 0xFF) shl 8) or
            ((data[offset + 2].toLong() and 0xFF) shl 16) or
            ((data[offset + 3].toLong() and 0xFF) shl 24)

    private fun writeU32(data: ByteArray, offset: Int, value: Long) {
        data[offset] = (value and 0xFF).toByte()
        data[offset + 1] = ((value ushr 8) and 0xFF).toByte()
        data[offset + 2] = ((value ushr 16) and 0xFF).toByte()
        data[offset + 3] = ((value ushr 24) and 0xFF).toByte()
    }

    private val gameCharset: Charset by lazy { Charset.forName("Shift_JIS") }

    private fun readGameString(data: ByteArray, offset: Int, length: Int): String {
        var end = offset
        val limit = minOf(data.size, offset + length)
        while (end < limit && data[end].toInt() != 0) end++
        return runCatching { String(data, offset, end - offset, gameCharset) }.getOrDefault("")
    }

    private fun writeGameString(data: ByteArray, offset: Int, length: Int, text: String) {
        for (i in 0 until length) data[offset + i] = 0
        val encoded = StringBuilder()
        for (ch in text) {
            val candidate = encoded.toString() + ch
            if (candidate.toByteArray(gameCharset).size >= length) break
            encoded.append(ch)
        }
        val bytes = encoded.toString().toByteArray(gameCharset)
        bytes.copyInto(data, offset, 0, minOf(bytes.size, length - 1))
    }
}
