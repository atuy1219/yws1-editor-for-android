package com.atuy.yws1editor.yokai

import java.io.IOException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class InventoryCodecTest {
    private val codec = InventoryCodec()

    @Test
    fun decodesFirstAndLastEntriesWithLittleEndianIdsAndMaximumCounts() {
        val data = fixture()
        putU32(data, InventoryCodec.ITEM_OFFSET, 0x00001001)
        putU32(data, InventoryCodec.ITEM_OFFSET + 4, 0x89ABCDEF)
        data[InventoryCodec.ITEM_OFFSET + 8] = 7
        val lastItem = InventoryCodec.ITEM_OFFSET + (InventoryCodec.ITEM_COUNT - 1) * InventoryCodec.ITEM_STRIDE
        putU32(data, lastItem, 0x000010FF)
        putU32(data, lastItem + 4, 0x12345678)
        data[lastItem + 8] = 99

        putU32(data, InventoryCodec.EQUIPMENT_OFFSET, 0x00002000)
        putU32(data, InventoryCodec.EQUIPMENT_OFFSET + 4, 0x01020304)
        data[InventoryCodec.EQUIPMENT_OFFSET + 8] = 1
        val lastEquipment = InventoryCodec.EQUIPMENT_OFFSET +
            (InventoryCodec.EQUIPMENT_COUNT - 1) * InventoryCodec.EQUIPMENT_STRIDE
        putU32(data, lastEquipment, 0x00002063)
        putU32(data, lastEquipment + 4, 0xFEDCBA98)
        data[lastEquipment + 8] = 3
        data[lastEquipment + 12] = 2

        putU32(data, InventoryCodec.KEY_ITEM_OFFSET, 0x00003000)
        putU32(data, InventoryCodec.KEY_ITEM_OFFSET + 4, 0x0A0B0C0D)
        val lastKey = InventoryCodec.KEY_ITEM_OFFSET +
            (InventoryCodec.KEY_ITEM_COUNT - 1) * InventoryCodec.KEY_ITEM_STRIDE
        putU32(data, lastKey, 0x00003095)
        putU32(data, lastKey + 4, 0x80000001)

        val decoded = codec.decode(data)

        assertEquals(InventoryCodec.ITEM_COUNT, decoded.items.size)
        assertEquals(InventoryCodec.EQUIPMENT_COUNT, decoded.equipment.size)
        assertEquals(InventoryCodec.KEY_ITEM_COUNT, decoded.keyItems.size)
        assertEquals(0x89ABCDEFL, decoded.items.first().itemId)
        assertEquals(7, decoded.items.first().quantity)
        assertEquals(0x12345678L, decoded.items.last().itemId)
        assertEquals(99, decoded.items.last().quantity)
        assertEquals(0x01020304L, decoded.equipment.first().itemId)
        assertEquals(0xFEDCBA98L, decoded.equipment.last().itemId)
        assertEquals(3, decoded.equipment.last().ownedCount)
        assertEquals(2, decoded.equipment.last().equippedCount)
        assertEquals(0x0A0B0C0DL, decoded.keyItems.first().itemId)
        assertEquals(0x80000001L, decoded.keyItems.last().itemId)
    }

    @Test
    fun emptyEntriesAreReportedWithoutBeingDiscarded() {
        val decoded = codec.decode(fixture())

        assertFalse(decoded.items.first().isUsed)
        assertFalse(decoded.equipment.first().isUsed)
        assertFalse(decoded.keyItems.first().isUsed)
        assertEquals(0, decoded.items.first().quantity)
    }

    @Test
    fun rejectsTruncatedDataWithoutIndexOutOfBoundsException() {
        assertThrows(IOException::class.java) {
            codec.decode(ByteArray(InventoryCodec.KEY_ITEM_OFFSET + 7))
        }
    }

    @Test
    fun replacesOnlyExistingItemQuantityLowByte() {
        val data = fixture().also {
            putU32(it, InventoryCodec.ITEM_OFFSET, 0x00001000)
            putU32(it, InventoryCodec.ITEM_OFFSET + 4, 0x11223344)
            it[InventoryCodec.ITEM_OFFSET + 8] = 4
            it[InventoryCodec.ITEM_OFFSET + 9] = 0x55
            it[InventoryCodec.ITEM_OFFSET + 10] = 0x66
            it[InventoryCodec.ITEM_OFFSET + 11] = 0x77
        }

        val changed = codec.replaceItemQuantity(data, entryIndex = 0, newQuantity = 9, maximumQuantity = 20)

        assertEquals(4, data[InventoryCodec.ITEM_OFFSET + 8].toInt())
        assertEquals(9, changed[InventoryCodec.ITEM_OFFSET + 8].toInt())
        val expected = data.copyOf().also { it[InventoryCodec.ITEM_OFFSET + 8] = 9 }
        assertArrayEquals(expected, changed)
        assertEquals(0x55, changed[InventoryCodec.ITEM_OFFSET + 9].toInt() and 0xff)
    }

    @Test
    fun itemQuantityRequiresExistingEntryAndItemSpecificLimit() {
        val empty = fixture()
        assertThrows(IOException::class.java) {
            codec.replaceItemQuantity(empty, 0, 1, 99)
        }

        val used = fixture().also { putU32(it, InventoryCodec.ITEM_OFFSET + 4, 1) }
        assertThrows(IOException::class.java) {
            codec.replaceItemQuantity(used, 0, 11, 10)
        }
        assertThrows(IOException::class.java) {
            codec.replaceItemQuantity(used, 0, 1, 128)
        }
    }

    @Test
    fun rejectsEquipmentOwnedCountBelowEquippedCount() {
        val data = fixture().also {
            putU32(it, InventoryCodec.EQUIPMENT_OFFSET + 4, 0x10203040)
            it[InventoryCodec.EQUIPMENT_OFFSET + 8] = 1
            it[InventoryCodec.EQUIPMENT_OFFSET + 12] = 2
        }

        val error = assertThrows(IOException::class.java) { codec.decode(data) }
        assertTrue(error.message.orEmpty().contains("装備中数未満"))
    }

    @Test
    fun rejectsNegativeSerializedCounts() {
        val data = fixture().also { it[InventoryCodec.ITEM_OFFSET + 8] = 0xff.toByte() }
        assertThrows(IOException::class.java) { codec.decode(data) }
    }

    private fun fixture(): ByteArray = ByteArray(
        InventoryCodec.KEY_ITEM_OFFSET + InventoryCodec.KEY_ITEM_COUNT * InventoryCodec.KEY_ITEM_STRIDE,
    )

    private fun putU32(data: ByteArray, offset: Int, value: Long) {
        repeat(4) { index -> data[offset + index] = (value ushr (index * 8)).toByte() }
    }
}
