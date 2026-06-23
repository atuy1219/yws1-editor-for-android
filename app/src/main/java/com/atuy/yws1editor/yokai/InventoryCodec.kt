package com.atuy.yws1editor.yokai

import java.io.IOException

data class InventoryItemEntry(
    val index: Int,
    val handle: Long,
    val itemId: Long,
    val quantity: Int,
    val rawEntry: ByteArray,
) {
    val isUsed: Boolean get() = itemId != 0L
}

data class EquipmentEntry(
    val index: Int,
    val handle: Long,
    val itemId: Long,
    val ownedCount: Int,
    val equippedCount: Int,
    val rawEntry: ByteArray,
) {
    val isUsed: Boolean get() = itemId != 0L
}

data class KeyItemEntry(
    val index: Int,
    val handle: Long,
    val itemId: Long,
    val rawEntry: ByteArray,
) {
    val isUsed: Boolean get() = itemId != 0L
}

data class InventorySnapshot(
    val items: List<InventoryItemEntry>,
    val equipment: List<EquipmentEntry>,
    val keyItems: List<KeyItemEntry>,
)

class InventoryCodec {
    companion object {
        const val ITEM_OFFSET = 0x0628
        const val ITEM_COUNT = 256
        const val ITEM_STRIDE = 12
        const val EQUIPMENT_OFFSET = 0x1234
        const val EQUIPMENT_COUNT = 100
        const val EQUIPMENT_STRIDE = 16
        const val KEY_ITEM_OFFSET = 0x1880
        const val KEY_ITEM_COUNT = 150
        const val KEY_ITEM_STRIDE = 8

        private const val HANDLE_FIELD = 0x00
        private const val ID_FIELD = 0x04
        private const val QUANTITY_FIELD = 0x08
        private const val EQUIPPED_FIELD = 0x0c
        private const val SERIALIZED_SIGNED_BYTE_MAX = 127
    }

    fun decode(gameData: ByteArray): InventorySnapshot {
        SaveDataBinary.requireRange(gameData, ITEM_OFFSET, ITEM_COUNT * ITEM_STRIDE, "どうぐ領域")
        SaveDataBinary.requireRange(
            gameData,
            EQUIPMENT_OFFSET,
            EQUIPMENT_COUNT * EQUIPMENT_STRIDE,
            "そうび領域",
        )
        SaveDataBinary.requireRange(
            gameData,
            KEY_ITEM_OFFSET,
            KEY_ITEM_COUNT * KEY_ITEM_STRIDE,
            "だいじなもの領域",
        )

        val items = List(ITEM_COUNT) { index ->
            val offset = ITEM_OFFSET + index * ITEM_STRIDE
            InventoryItemEntry(
                index = index,
                handle = SaveDataBinary.readUInt32Le(gameData, offset + HANDLE_FIELD),
                itemId = SaveDataBinary.readUInt32Le(gameData, offset + ID_FIELD),
                quantity = readSignedCount(gameData, offset + QUANTITY_FIELD, "どうぐ[$index]"),
                rawEntry = gameData.copyOfRange(offset, offset + ITEM_STRIDE),
            )
        }
        val equipment = List(EQUIPMENT_COUNT) { index ->
            val offset = EQUIPMENT_OFFSET + index * EQUIPMENT_STRIDE
            val owned = readSignedCount(gameData, offset + QUANTITY_FIELD, "そうび[$index]の所持数")
            val equipped = readSignedCount(gameData, offset + EQUIPPED_FIELD, "そうび[$index]の装備中数")
            val itemId = SaveDataBinary.readUInt32Le(gameData, offset + ID_FIELD)
            if (itemId != 0L && owned < equipped) {
                throw IOException("そうび[$index]の所持数が装備中数未満です")
            }
            EquipmentEntry(
                index = index,
                handle = SaveDataBinary.readUInt32Le(gameData, offset + HANDLE_FIELD),
                itemId = itemId,
                ownedCount = owned,
                equippedCount = equipped,
                rawEntry = gameData.copyOfRange(offset, offset + EQUIPMENT_STRIDE),
            )
        }
        val keyItems = List(KEY_ITEM_COUNT) { index ->
            val offset = KEY_ITEM_OFFSET + index * KEY_ITEM_STRIDE
            KeyItemEntry(
                index = index,
                handle = SaveDataBinary.readUInt32Le(gameData, offset + HANDLE_FIELD),
                itemId = SaveDataBinary.readUInt32Le(gameData, offset + ID_FIELD),
                rawEntry = gameData.copyOfRange(offset, offset + KEY_ITEM_STRIDE),
            )
        }
        return InventorySnapshot(items, equipment, keyItems)
    }

    /**
     * Changes only the low byte of an existing item's serialized quantity.
     * Limited to 0-99.
     */
    fun replaceItemQuantity(
        gameData: ByteArray,
        entryIndex: Int,
        newQuantity: Int,
    ): ByteArray {
        if (entryIndex !in 0 until ITEM_COUNT) throw IOException("どうぐentry番号が不正です")
        SaveDataBinary.requireRange(gameData, ITEM_OFFSET, ITEM_COUNT * ITEM_STRIDE, "どうぐ領域")
        if (newQuantity !in 0..99) {
            throw IOException("どうぐ数量は0～99の範囲で指定してください")
        }

        val offset = ITEM_OFFSET + entryIndex * ITEM_STRIDE
        if (SaveDataBinary.readUInt32Le(gameData, offset + ID_FIELD) == 0L) {
            throw IOException("空欄のどうぐentryは変更できません")
        }
        val out = gameData.copyOf()
        out[offset + QUANTITY_FIELD] = newQuantity.toByte()
        return out
    }

    /**
     * Changes the owned count of equipment. The equipped count must remain <= owned count.
     * Limited to 0-99.
     */
    fun replaceEquipmentOwnedCount(
        gameData: ByteArray,
        entryIndex: Int,
        newOwnedCount: Int,
    ): ByteArray {
        if (entryIndex !in 0 until EQUIPMENT_COUNT) throw IOException("そうびentry番号が不正です")
        SaveDataBinary.requireRange(gameData, EQUIPMENT_OFFSET, EQUIPMENT_COUNT * EQUIPMENT_STRIDE, "そうび領域")
        if (newOwnedCount !in 0..99) {
            throw IOException("そうび所持数は0～99の範囲で指定してください")
        }

        val offset = EQUIPMENT_OFFSET + entryIndex * EQUIPMENT_STRIDE
        val itemId = SaveDataBinary.readUInt32Le(gameData, offset + ID_FIELD)
        if (itemId == 0L) {
            throw IOException("空欄のそうびentryは変更できません")
        }
        val equippedCount = readSignedCount(gameData, offset + EQUIPPED_FIELD, "そうび[$entryIndex]の装備中数")
        if (newOwnedCount < equippedCount) {
            throw IOException("そうび[$entryIndex]の所持数は装備中数(${equippedCount})以上である必要があります")
        }

        val out = gameData.copyOf()
        out[offset + QUANTITY_FIELD] = newOwnedCount.toByte()
        return out
    }

    private fun readSignedCount(data: ByteArray, offset: Int, label: String): Int {
        SaveDataBinary.requireRange(data, offset, 1, label)
        val value = data[offset].toInt()
        if (value < 0) throw IOException("$label が負数です")
        return value
    }
}
