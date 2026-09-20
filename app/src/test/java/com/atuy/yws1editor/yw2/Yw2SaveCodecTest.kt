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
        data[soulSection + 10] = 10
        data[soulSection + 11] = 1

        val yokaiSection = putSection(
            data = data,
            header = 0x3000,
            id = 0x07,
            size = Yw2SaveCodec.YOKAI_RECORD_SIZE * Yw2SaveCodec.YOKAI_MAX,
        )
        // Leave slots 0..2 empty to ensure sparse storage does not stop parsing.
        val y = yokaiSection + Yw2SaveCodec.YOKAI_RECORD_SIZE * 3
        putU16(data, y, 3)
        putU16(data, y + 2, 1)
        putU32(data, y + 4, 0x10203040)
        "テスト".toByteArray(Charset.forName("Shift_JIS")).copyInto(data, y + 8)
        data[y + 42] = 9
        data[y + 46] = 8
        data[y + 50] = 7
        putU16(data, y + 0x20, 0x1000)
        putU16(data, y + 0x22, 1)
        putU16(data, y + 0x24, 0x3000)
        putU16(data, y + 0x26, 1)
        putU32(data, y + 52, 1234)
        putU32(data, y + 60, 5678)
        byteArrayOf(16, 8, 8, 8, 8).copyInto(data, y + 64)
        byteArrayOf(8, 4, 4, 4, 4).copyInto(data, y + 69)
        byteArrayOf((-2).toByte(), 1, 0, (-1).toByte(), 2).copyInto(data, y + 74)
        data[y + 79] = 99.toByte()
        data[y + 84] = 0x53

        val moneySection = putSection(
            data = data,
            header = 0xD000,
            id = 0x09,
            size = 0x100,
        )
        putU32(data, moneySection + 0x14, 123456)

        val yokai = Yw2SaveCodec.parseYokai(data).single()
        assertEquals(3, yokai.slot)
        assertEquals(0x10203040L, yokai.typeId)
        assertEquals("テスト", yokai.nickname)
        assertEquals(99, yokai.level)
        assertEquals(5, yokai.loafLevel)
        assertEquals(3, yokai.attitude)
        assertEquals(Yw2EquipRef(0x1000, 1), yokai.equip1)
        assertEquals(Yw2EquipRef(0x3000, 1), yokai.equip2)
        assertEquals(Yw2Stats(-2, 1, 0, -1, 2), yokai.sportsClub)

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
        assertEquals(10, soul.level)
        assertEquals(1, soul.used)

        val changed = Yw2SaveCodec.updateYokai(data, yokai.copy(level = 88, nickname = "ジバ"))
        val reread = Yw2SaveCodec.parseYokai(changed).single()
        assertEquals(88, reread.level)
        assertEquals("ジバ", reread.nickname)
        assertEquals(Yw2EquipRef(0x1000, 1), reread.equip1)
        assertEquals(Yw2EquipRef(0x3000, 1), reread.equip2)
        assertEquals(Yw2Stats(-2, 1, 0, -1, 2), reread.sportsClub)
        assertEquals(
            1,
            Yw2SaveCodec.parseInventory(changed, Yw2InventoryKind.EQUIPMENT).single().used,
        )
        assertEquals(
            1,
            Yw2SaveCodec.parseInventory(changed, Yw2InventoryKind.SOUL).single().used,
        )

        val unequipped = Yw2SaveCodec.updateYokai(
            data,
            yokai.copy(equip1 = Yw2EquipRef(), equip2 = Yw2EquipRef()),
        )
        assertEquals(
            0,
            Yw2SaveCodec.parseInventory(unequipped, Yw2InventoryKind.EQUIPMENT).single().used,
        )
        assertEquals(
            0,
            Yw2SaveCodec.parseInventory(unequipped, Yw2InventoryKind.SOUL).single().used,
        )

        val sameEquipmentTwice = Yw2SaveCodec.updateYokai(
            data,
            yokai.copy(
                equip1 = Yw2EquipRef(0x1000, 1),
                equip2 = Yw2EquipRef(0x1000, 1),
            ),
        )
        assertEquals(
            2,
            Yw2SaveCodec.parseInventory(
                sameEquipmentTwice,
                Yw2InventoryKind.EQUIPMENT,
            ).single().used,
        )
        assertTrue(
            runCatching {
                Yw2SaveCodec.updateYokai(
                    data,
                    yokai.copy(
                        equip1 = Yw2EquipRef(0x3000, 1),
                        equip2 = Yw2EquipRef(0x3000, 1),
                    ),
                )
            }.isFailure
        )
        val doubleUsedEquipment = Yw2SaveCodec.parseInventory(
            sameEquipmentTwice,
            Yw2InventoryKind.EQUIPMENT,
        ).single()
        assertTrue(
            runCatching {
                Yw2SaveCodec.updateInventory(
                    sameEquipmentTwice,
                    doubleUsedEquipment.copy(amount = 1),
                )
            }.isFailure
        )

        val changedItem = Yw2SaveCodec.updateInventory(data, item.copy(amount = 99))
        assertEquals(99, Yw2SaveCodec.parseInventory(changedItem, Yw2InventoryKind.ITEM).single().amount)

        assertEquals(123456L, Yw2SaveCodec.readMoney(data))
        val changedMoney = Yw2SaveCodec.updateMoney(data, 654321)
        assertEquals(654321L, Yw2SaveCodec.readMoney(changedMoney))
    }

    @Test
    fun validatesIvAndEvRules() {
        Yw2SaveCodec.validateIv(Yw2Stats(16, 8, 8, 8, 8))
        Yw2SaveCodec.validateEv(Yw2Stats(8, 4, 4, 4, 4))
        Yw2SaveCodec.validateSportsClub(Yw2Stats(-10, 25, 0, 5, -3))

        assertTrue(runCatching { Yw2SaveCodec.validateIv(Yw2Stats(15, 8, 8, 8, 8)) }.isFailure)
        assertTrue(runCatching { Yw2SaveCodec.validateEv(Yw2Stats(10, 4, 4, 4, 4)) }.isFailure)
        assertTrue(runCatching { Yw2SaveCodec.validateSportsClub(Yw2Stats(-11, 0, 0, 0, 0)) }.isFailure)
        assertTrue(runCatching { Yw2SaveCodec.validateSportsClub(Yw2Stats(0, 26, 0, 0, 0)) }.isFailure)
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
