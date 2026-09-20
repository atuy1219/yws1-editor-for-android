package com.atuy.yws1editor.yw2

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Yw2SaveCodecTest {
    @Test
    fun parsesAndUpdatesYokaiAndInventory() {
        val data = ByteArray(0x12000)

        val itemSection = putSection(
            data = data,
            header = 0x100,
            id = 0x04,
            size = Yw2InventoryKind.ITEM.recordSize * Yw2InventoryKind.ITEM.maxEntries,
        )
        putU16(data, itemSection, 0)
        putU16(data, itemSection + 2, 1)
        putU32(data, itemSection + 4, 0x12345678)
        data[itemSection + 8] = 7

        val equipmentSection = putSection(
            data = data,
            header = 0x1600,
            id = 0x05,
            size = Yw2InventoryKind.EQUIPMENT.recordSize * Yw2InventoryKind.EQUIPMENT.maxEntries,
        )
        putU16(data, equipmentSection, 0x1000)
        putU16(data, equipmentSection + 2, 1)
        putU32(data, equipmentSection + 4, 0x11223344)
        data[equipmentSection + 8] = 2
        data[equipmentSection + 12] = 1

        val importantSection = putSection(
            data = data,
            header = 0x1C00,
            id = 0x06,
            size = Yw2InventoryKind.IMPORTANT.recordSize * Yw2InventoryKind.IMPORTANT.maxEntries,
        )
        putU16(data, importantSection, 0x2000)
        putU16(data, importantSection + 2, 1)
        putU32(data, importantSection + 4, 0x55667788)

        val soulSection = putSection(
            data = data,
            header = 0x2200,
            id = 0x13,
            size = Yw2InventoryKind.SOUL.recordSize * Yw2InventoryKind.SOUL.maxEntries,
        )
        putU16(data, soulSection, 0x3000)
        putU16(data, soulSection + 2, 1)
        putU32(data, soulSection + 4, 0x01020304)
        putU16(data, soulSection + 8, 123)
        putU16(data, soulSection + 10, 7)

        val y = Yw2SaveCodec.YOKAI_OFFSET
        putU16(data, y, 0)
        putU16(data, y + 2, 1)
        putU32(data, y + 4, 0x10203040)
        "テスト".toByteArray(Charset.forName("Shift_JIS")).copyInto(data, y + 8)
        data[y + 42] = 9
        data[y + 46] = 8
        data[y + 50] = 7
        putU32(data, y + 52, 1234)
        putU32(data, y + 60, 5678)
        byteArrayOf(16, 8, 8, 8, 8).copyInto(data, y + 64)
        byteArrayOf(8, 4, 4, 4, 4).copyInto(data, y + 69)
        data[y + 79] = 99.toByte()
        data[y + 84] = 0x53

        val yokai = Yw2SaveCodec.parseYokai(data).single()
        assertEquals(0x10203040L, yokai.typeId)
        assertEquals("テスト", yokai.nickname)
        assertEquals(99, yokai.level)
        assertEquals(5, yokai.loafLevel)
        assertEquals(3, yokai.attitude)

        val item = Yw2SaveCodec.parseInventory(data, Yw2InventoryKind.ITEM).single()
        assertEquals(7, item.amount)
        assertEquals(0x12345678L, item.typeId)

        val equipment = Yw2SaveCodec.parseInventory(data, Yw2InventoryKind.EQUIPMENT).single()
        assertEquals(2, equipment.amount)
        assertEquals(1, equipment.used)

        val important = Yw2SaveCodec.parseInventory(data, Yw2InventoryKind.IMPORTANT).single()
        assertEquals(0x55667788L, important.typeId)

        val soul = Yw2SaveCodec.parseInventory(data, Yw2InventoryKind.SOUL).single()
        assertEquals(123, soul.experience)
        assertEquals(7, soul.level)

        val changed = Yw2SaveCodec.updateYokai(data, yokai.copy(level = 88, nickname = "ジバ"))
        val reread = Yw2SaveCodec.parseYokai(changed).single()
        assertEquals(88, reread.level)
        assertEquals("ジバ", reread.nickname)

        val changedItem = Yw2SaveCodec.updateInventory(data, item.copy(amount = 99))
        assertEquals(99, Yw2SaveCodec.parseInventory(changedItem, Yw2InventoryKind.ITEM).single().amount)
    }

    @Test
    fun validatesIvAndEvRules() {
        Yw2SaveCodec.validateIv(Yw2Stats(16, 8, 8, 8, 8))
        Yw2SaveCodec.validateEv(Yw2Stats(8, 4, 4, 4, 4))

        assertTrue(runCatching { Yw2SaveCodec.validateIv(Yw2Stats(15, 8, 8, 8, 8)) }.isFailure)
        assertTrue(runCatching { Yw2SaveCodec.validateEv(Yw2Stats(10, 4, 4, 4, 4)) }.isFailure)
    }

    private fun putSection(data: ByteArray, header: Int, id: Int, size: Int): Int {
        putU32(data, header, 0x0000FFFE)
        putU32(data, header + 4, (size shl 8) or id)
        val body = header + 8
        putU32(data, body + size, 0x0000FEFF)
        return body
    }

    private fun putU16(data: ByteArray, o: Int, value: Int) {
        data[o] = value.toByte()
        data[o + 1] = (value ushr 8).toByte()
    }

    private fun putU32(data: ByteArray, o: Int, value: Int) {
        data[o] = value.toByte()
        data[o + 1] = (value ushr 8).toByte()
        data[o + 2] = (value ushr 16).toByte()
        data[o + 3] = (value ushr 24).toByte()
    }
}
