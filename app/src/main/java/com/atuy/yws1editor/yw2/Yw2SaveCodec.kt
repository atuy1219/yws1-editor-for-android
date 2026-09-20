package com.atuy.yws1editor.yw2

import java.io.IOException

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
    val statCorrection: Yw2Stats,
    val level: Int,
    val loafLevel: Int,
    val attitude: Int,
)

enum class Yw2InventoryKind(
    val label: String,
    val recordSize: Int,
    val maxEntries: Int,
) {
    ITEM("どうぐ", 0x0C, 430),
    EQUIPMENT("そうび", 0x10, 90),
    IMPORTANT("だいじなもの", 0x08, 180),
    SOUL("魂", 0x0C, 100),
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
    const val YOKAI_OFFSET = 0x5108
    const val YOKAI_RECORD_SIZE = 0x5C
    const val YOKAI_MAX = 406
    const val MONEY_OFFSET = 0x108E0

    private val INVENTORY_MAGIC = byteArrayOf(
        0xFE.toByte(), 0x6D, 0x08, 0xFE.toByte(), 0xFF.toByte(), 0x00, 0x00,
        0x03, 0x48, 0x24, 0x00, 0xFE.toByte(), 0xFF.toByte(),
    )

    fun parseYokai(data: ByteArray): List<Yw2Yokai> {
        val out = ArrayList<Yw2Yokai>()
        for (slot in 0 until YOKAI_MAX) {
            val o = YOKAI_OFFSET + slot * YOKAI_RECORD_SIZE
            if (o + YOKAI_RECORD_SIZE > data.size) break
            val num2 = readU16(data, o + 2)
            if (num2 == 0) break
            val packed = data[o + 84].toInt() and 0xFF
            out += Yw2Yokai(
                slot = slot,
                num1 = readU16(data, o),
                num2 = num2,
                typeId = readU32(data, o + 4),
                nickname = readUtf8(data, o + 8, 24),
                attackLevel = data[o + 42].toInt() and 0xFF,
                techniqueLevel = data[o + 46].toInt() and 0xFF,
                soultimateLevel = data[o + 50].toInt() and 0xFF,
                experience = readU32(data, o + 52),
                ownerId = readU32(data, o + 60),
                iv = readStats(data, o + 64),
                ev = readStats(data, o + 69),
                statCorrection = readStats(data, o + 74),
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
        val o = YOKAI_OFFSET + value.slot * YOKAI_RECORD_SIZE
        if (o + YOKAI_RECORD_SIZE > data.size) throw IOException("妖怪レコードがセーブ範囲外です")

        return data.copyOf().also { out ->
            writeU16(out, o, value.num1)
            writeU16(out, o + 2, value.num2)
            writeU32(out, o + 4, value.typeId)
            writeUtf8(out, o + 8, 24, value.nickname)
            out[o + 42] = value.attackLevel.coerceIn(0, 255).toByte()
            out[o + 46] = value.techniqueLevel.coerceIn(0, 255).toByte()
            out[o + 50] = value.soultimateLevel.coerceIn(0, 255).toByte()
            writeU32(out, o + 52, value.experience)
            writeU32(out, o + 60, value.ownerId)
            writeStats(out, o + 64, value.iv)
            writeStats(out, o + 69, value.ev)
            writeStats(out, o + 74, value.statCorrection)
            out[o + 79] = value.level.coerceIn(1, 99).toByte()
            out[o + 84] = (
                ((value.loafLevel.coerceIn(0, 15) shl 4) or value.attitude.coerceIn(0, 15))
            ).toByte()
        }
    }

    fun parseInventory(data: ByteArray, kind: Yw2InventoryKind): List<Yw2InventoryEntry> {
        val base = inventoryBase(data) + when (kind) {
            Yw2InventoryKind.ITEM -> 0
            Yw2InventoryKind.EQUIPMENT -> 0x1434
            Yw2InventoryKind.IMPORTANT -> 0x19E0
            Yw2InventoryKind.SOUL -> 0x1F8C
        }

        val out = ArrayList<Yw2InventoryEntry>()
        for (slot in 0 until kind.maxEntries) {
            val o = base + slot * kind.recordSize
            if (o + kind.recordSize > data.size) break
            val num2 = readU16(data, o + 2)
            if (num2 == 0) break
            out += when (kind) {
                Yw2InventoryKind.ITEM -> Yw2InventoryEntry(
                    kind, slot, readU16(data, o), num2, readU32(data, o + 4),
                    amount = data[o + 8].toInt() and 0xFF,
                )
                Yw2InventoryKind.EQUIPMENT -> Yw2InventoryEntry(
                    kind, slot, readU16(data, o), num2, readU32(data, o + 4),
                    amount = data[o + 8].toInt() and 0xFF,
                    used = data[o + 12].toInt() and 0xFF,
                )
                Yw2InventoryKind.IMPORTANT -> Yw2InventoryEntry(
                    kind, slot, readU16(data, o), num2, readU32(data, o + 4),
                )
                Yw2InventoryKind.SOUL -> Yw2InventoryEntry(
                    kind, slot, readU16(data, o), num2, readU32(data, o + 4),
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
        if (entry.slot !in 0 until kind.maxEntries) throw IOException("アイテムスロットが範囲外です")
        val base = inventoryBase(data) + when (kind) {
            Yw2InventoryKind.ITEM -> 0
            Yw2InventoryKind.EQUIPMENT -> 0x1434
            Yw2InventoryKind.IMPORTANT -> 0x19E0
            Yw2InventoryKind.SOUL -> 0x1F8C
        }
        val o = base + entry.slot * kind.recordSize
        if (o + kind.recordSize > data.size) throw IOException("アイテムレコードがセーブ範囲外です")

        return data.copyOf().also { out ->
            writeU16(out, o, entry.num1)
            writeU16(out, o + 2, entry.num2)
            writeU32(out, o + 4, entry.typeId)
            when (kind) {
                Yw2InventoryKind.ITEM -> out[o + 8] = entry.amount.coerceIn(0, 255).toByte()
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
        if (MONEY_OFFSET + 4 > data.size) throw IOException("所持金領域がセーブ範囲外です")
        return readU32(data, MONEY_OFFSET)
    }

    fun updateMoney(data: ByteArray, money: Long): ByteArray {
        if (money !in 0..0xFFFF_FFFFL) throw IOException("所持金が範囲外です")
        if (MONEY_OFFSET + 4 > data.size) throw IOException("所持金領域がセーブ範囲外です")
        return data.copyOf().also { writeU32(it, MONEY_OFFSET, money) }
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

    private fun inventoryBase(data: ByteArray): Int {
        val limit = minOf(YOKAI_OFFSET, data.size)
        outer@ for (i in 0..(limit - INVENTORY_MAGIC.size).coerceAtLeast(0)) {
            for (j in INVENTORY_MAGIC.indices) {
                if (data[i + j] != INVENTORY_MAGIC[j]) continue@outer
            }
            val result = i + 19
            if (result >= data.size) break
            return result
        }
        throw IOException("YW2の持ち物領域を特定できません")
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

    private fun readUtf8(data: ByteArray, offset: Int, length: Int): String {
        var end = offset
        val limit = minOf(data.size, offset + length)
        while (end < limit && data[end].toInt() != 0) end++
        return runCatching { data.copyOfRange(offset, end).toString(Charsets.UTF_8) }.getOrDefault("")
    }

    private fun writeUtf8(data: ByteArray, offset: Int, length: Int, text: String) {
        for (i in 0 until length) data[offset + i] = 0
        val encoded = StringBuilder()
        for (ch in text) {
            val candidate = encoded.toString() + ch
            if (candidate.toByteArray(Charsets.UTF_8).size >= length) break
            encoded.append(ch)
        }
        val bytes = encoded.toString().toByteArray(Charsets.UTF_8)
        bytes.copyInto(data, offset, 0, minOf(bytes.size, length - 1))
    }
}
