package com.atuy.yws1editor.yokai

import java.io.IOException

class PartyCodec {
    companion object {
        const val PARTY_SIZE = 6
        private const val COLLECTION_SIZE = 241
        private const val BLOCK_MARKER = 0x0000FFFE
        private const val OUTER_TAG = 0xF1
        private const val DATASET_TAG = 0xF3
        private const val PARTY_TAG = 0x08
        private const val PARTY_COLLECTION_TAG = 0x0A
        private const val BLOCK_HEADER_SIZE = 8
        private const val BLOCK_TRAILER_SIZE = 4
        private const val COLLECTION_HANDLES_SIZE = COLLECTION_SIZE * Int.SIZE_BYTES
    }

    fun decode(gameData: ByteArray, entries: List<YokaiEntry>): List<PartyMemberEntry> {
        val collection = findPartyCollection(gameData) ?: return emptyList()
        requireCollectionSize(gameData, collection)

        val entriesByHandle = entries.associateBy { it.handle }
        return (0 until PARTY_SIZE).map { position ->
            val handle = readUInt32Le(gameData, collection.payloadOffset + position * Int.SIZE_BYTES)
            val entry = entriesByHandle[handle]
            PartyMemberEntry(
                position = position,
                yokaiHandle = handle,
                yokaiSlot = entry?.slot,
                yokaiName = entry?.name ?: if (handle == 0L) "未設定" else "不明 ${formatU32(handle)}",
            )
        }
    }

    /**
     * Rebuilds the six active slots using the same effective operation as
     * ywPartyCollection::Swap: handles are exchanged inside the complete
     * 241-slot collection instead of overwriting only the active slots.
     */
    fun replacePartyMembers(gameData: ByteArray, handles: List<Long>): ByteArray {
        val collection = findPartyCollection(gameData)
            ?: throw IOException("パーティ領域が見つかりません")
        requireCollectionSize(gameData, collection)

        val desired = List(PARTY_SIZE) { position ->
            handles.getOrElse(position) { 0L }.normalizedU32()
        }
        validateDesiredParty(desired)

        val slots = MutableList(COLLECTION_SIZE) { index ->
            readUInt32Le(gameData, collection.payloadOffset + index * Int.SIZE_BYTES)
        }
        val originalCounts = slots.groupingBy { it }.eachCount()

        repeat(PARTY_SIZE) { position ->
            val wanted = desired[position]
            if (slots[position] == wanted) return@repeat

            val sourcePosition = findSwapSource(slots, position, wanted)
            val displaced = slots[position]
            slots[position] = slots[sourcePosition]
            slots[sourcePosition] = displaced
        }

        if (slots.take(PARTY_SIZE) != desired) {
            throw IOException("パーティの並び替えに失敗しました")
        }
        if (slots.groupingBy { it }.eachCount() != originalCounts) {
            throw IOException("パーティ変更により所属ハンドルの集合が変化しました")
        }

        val out = gameData.copyOf()
        slots.forEachIndexed { index, handle ->
            writeUInt32Le(out, collection.payloadOffset + index * Int.SIZE_BYTES, handle)
        }
        return out
    }

    private fun validateDesiredParty(desired: List<Long>) {
        val duplicate = desired.asSequence()
            .filter { it != 0L }
            .groupingBy { it }
            .eachCount()
            .entries
            .firstOrNull { it.value > 1 }
            ?.key
        if (duplicate != null) {
            throw IOException("同じ妖怪 ${formatU32(duplicate)} を複数のパーティ枠には設定できません")
        }
    }

    private fun findSwapSource(slots: List<Long>, targetPosition: Int, wanted: Long): Int {
        if (wanted == 0L) {
            return (PARTY_SIZE until COLLECTION_SIZE).firstOrNull { slots[it] == 0L }
                ?: throw IOException("控えに空きがないためパーティから外せません")
        }

        // Prefer the reserve occurrence. This also avoids disturbing another
        // active slot when reading a previously corrupted save with duplicates.
        val reservePosition = (PARTY_SIZE until COLLECTION_SIZE).firstOrNull { slots[it] == wanted }
        if (reservePosition != null) return reservePosition

        return (0 until PARTY_SIZE).firstOrNull {
            it != targetPosition && slots[it] == wanted
        } ?: throw IOException("指定された妖怪 ${formatU32(wanted)} は所属一覧に存在しません")
    }

    private fun findPartyCollection(gameData: ByteArray): PartyCollection? {
        val outer = readBlockHeaderOrNull(gameData, 0) ?: return null
        if (outer.tag != OUTER_TAG) return null

        val outerPayload = BLOCK_HEADER_SIZE
        val outerEnd = checkedBlockPayloadEnd(gameData, 0, outer.size)
        val dataset = findChildBlock(gameData, outerPayload, outerEnd, DATASET_TAG) ?: return null
        val datasetPayload = dataset.offset + BLOCK_HEADER_SIZE
        val datasetEnd = checkedBlockPayloadEnd(gameData, dataset.offset, dataset.size)
        val party = findChildBlock(gameData, datasetPayload, datasetEnd, PARTY_TAG) ?: return null
        val partyPayload = party.offset + BLOCK_HEADER_SIZE
        val partyEnd = checkedBlockPayloadEnd(gameData, party.offset, party.size)
        val collection = findChildBlock(gameData, partyPayload, partyEnd, PARTY_COLLECTION_TAG) ?: return null
        return PartyCollection(
            payloadOffset = collection.offset + BLOCK_HEADER_SIZE,
            payloadSize = collection.size,
        )
    }

    private fun requireCollectionSize(data: ByteArray, collection: PartyCollection) {
        if (collection.payloadSize < COLLECTION_HANDLES_SIZE) {
            throw IOException("パーティ所属領域が短すぎます")
        }
        val end = collection.payloadOffset.toLong() + COLLECTION_HANDLES_SIZE
        if (end > data.size.toLong()) {
            throw IOException("パーティ所属領域が範囲外です")
        }
    }

    private fun findChildBlock(data: ByteArray, start: Int, endExclusive: Int, tag: Int): BlockHeader? {
        var offset = start
        while (offset + BLOCK_HEADER_SIZE <= endExclusive) {
            val header = readBlockHeaderOrNull(data, offset) ?: return null
            val next = checkedBlockTotalEnd(data, offset, header.size)
            if (next > endExclusive) {
                throw IOException("パーティブロックのサイズが範囲外です")
            }
            if (header.tag == tag) return header
            offset = next
        }
        return null
    }

    private fun readBlockHeaderOrNull(data: ByteArray, offset: Int): BlockHeader? {
        if (offset < 0 || offset + BLOCK_HEADER_SIZE > data.size) return null
        val marker = readUInt32Le(data, offset).toInt()
        if (marker != BLOCK_MARKER) return null
        val packed = readUInt32Le(data, offset + Int.SIZE_BYTES)
        return BlockHeader(
            offset = offset,
            tag = (packed and 0xFFL).toInt(),
            size = (packed ushr 8).toIntChecked("ブロックサイズ"),
        )
    }

    private fun checkedBlockPayloadEnd(data: ByteArray, offset: Int, size: Int): Int {
        val payloadEnd = offset.toLong() + BLOCK_HEADER_SIZE + size.toLong()
        if (payloadEnd < 0 || payloadEnd > data.size.toLong()) {
            throw IOException("パーティブロックのpayloadが範囲外です")
        }
        return payloadEnd.toInt()
    }

    private fun checkedBlockTotalEnd(data: ByteArray, offset: Int, size: Int): Int {
        val totalEnd = offset.toLong() + BLOCK_HEADER_SIZE + size.toLong() + BLOCK_TRAILER_SIZE
        if (totalEnd < 0 || totalEnd > data.size.toLong()) {
            throw IOException("パーティブロックが範囲外です")
        }
        return totalEnd.toInt()
    }

    private fun Long.toIntChecked(label: String): Int {
        if (this < 0 || this > Int.MAX_VALUE.toLong()) throw IOException("${label}が大きすぎます")
        return toInt()
    }

    private fun Long.normalizedU32(): Long = this and 0xFFFFFFFFL

    private fun readUInt32Le(data: ByteArray, offset: Int): Long {
        if (offset < 0 || offset + Int.SIZE_BYTES > data.size) {
            throw IOException("パーティ領域の読み込み位置が範囲外です")
        }
        return (data[offset].toLong() and 0xFFL) or
            ((data[offset + 1].toLong() and 0xFFL) shl 8) or
            ((data[offset + 2].toLong() and 0xFFL) shl 16) or
            ((data[offset + 3].toLong() and 0xFFL) shl 24)
    }

    private fun writeUInt32Le(data: ByteArray, offset: Int, value: Long) {
        if (offset < 0 || offset + Int.SIZE_BYTES > data.size) {
            throw IOException("パーティ領域の書き込み位置が範囲外です")
        }
        val v = value.toInt()
        data[offset] = (v and 0xFF).toByte()
        data[offset + 1] = ((v ushr 8) and 0xFF).toByte()
        data[offset + 2] = ((v ushr 16) and 0xFF).toByte()
        data[offset + 3] = ((v ushr 24) and 0xFF).toByte()
    }

    private fun formatU32(value: Long): String = "0x%08X".format(value.normalizedU32())

    private data class PartyCollection(
        val payloadOffset: Int,
        val payloadSize: Int,
    )

    private data class BlockHeader(
        val offset: Int,
        val tag: Int,
        val size: Int,
    )
}
