package com.atuy.yws1editor.yw2

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Yw2SaveCodecTest {
    @Test
    fun parsesAndUpdatesYokaiAndInventory() {
        val data = ByteArray(0x12000)
        val magic = byteArrayOf(
            0xFE.toByte(), 0x6D, 0x08, 0xFE.toByte(), 0xFF.toByte(), 0x00, 0x00,
            0x03, 0x48, 0x24, 0x00, 0xFE.toByte(), 0xFF.toByte(),
        )
        val magicOffset = 0x100
        magic.copyInto(data, magicOffset)
        val inventory = magicOffset + 19

        // one item
        putU16(data, inventory, 0)
        putU16(data, inventory + 2, 1)
        putU32(data, inventory + 4, 0x12345678)
        data[inventory + 8] = 7

        // one equipment
        val eq = inventory + 0x1434
        putU16(data, eq, 0x1000)
        putU16(data, eq + 2, 1)
        putU32(data, eq + 4, 0x11223344)
        data[eq + 8] = 2
        data[eq + 12] = 1

        val y = Yw2SaveCodec.YOKAI_OFFSET
        putU16(data, y, 0)
        putU16(data, y + 2, 1)
        putU32(data, y + 4, 0x10203040)
        "テスト".toByteArray(Charsets.UTF_8).copyInto(data, y + 8)
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
