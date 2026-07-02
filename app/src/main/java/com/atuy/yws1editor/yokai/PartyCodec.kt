package com.atuy.yws1editor.yokai

import java.io.IOException

class PartyCodec {
    companion object {
        const val PARTY_SIZE = 6
        private const val BLOCK_MARKER = 0x0000FFFE
        private const val OUTER_TAG = 0xF1
        private const val DATASET_TAG = 0xF3
        private const val PARTY_TAG = 0x08
        private const val PARTY_COLLECTION_TAG = 0x0A
        private const val BLOCK_HEADER_SIZE = 8
        private const val BLOCK_TRAILER_SIZE = 4
        private const val PARTY_HANDLES_SIZE = PARTY_SIZE * Int.SIZE_BYTES
    }

    fun decode(gameData: ByteArray, entries: List<YokaiEntry>): List<PartyMemberEntry> {
        val payloadOffset = findPartyCollectionPayload(gameData) ?: return emptyList()
        if (payloadOffset + PARTY_HANDLES_SIZE > gameData.size) {
            throw IOException("パーティ領域が短すぎます")
        }

        val entriesByHandle = entries.associateBy { it.handle }
        return (0 until PARTY_SIZE).map { position ->
            val handle = readUInt32Le(gameData, payloadOffset + position * Int.SIZE_BYTES)
            val entry = entriesByHandle[handle]
            PartyMemberEntry(
                position = position,
                yokaiHandle = handle,
                yokaiSlot = entry?.slot,
                yokaiName = entry?.name ?: if (handle == 0L) "未設定" else "不明 ${formatU32(handle)}",
            )
        }
    }

    fun replacePartyMembers(gameData: ByteArray, handles: List<Long>): ByteArray {
        val payloadOffset = findPartyCollectionPayload(gameData)
            ?: throw IOException("パーティ領域が見つかりません")
        if (payloadOffset + PARTY_HANDLES_SIZE > gameData.size) {
            throw IOException("パーティ領域が短すぎます")
        }

        val out = gameData.copyOf()
        repeat(PARTY_SIZE) { position ->
            writeUInt32Le(out, payloadOffset + position * Int.SIZE_BYTES, handles.getOrElse(position) { 0L })
        }
        return out
    }

    private fun findPartyCollectionPayload(gameData: ByteArray): Int? {
        val outer = readBlockHeaderOrNull(gameData, 0) ?: return null
        if (outer.tag != OUTER_TAG) return null

        val outerPayload = 0 + BLOCK_HEADER_SIZE
        val outerEnd = checkedBlockPayloadEnd(gameData, 0, outer.size)
        val dataset = findChildBlock(gameData, outerPayload, outerEnd, DATASET_TAG) ?: return null
        val datasetPayload = dataset.offset + BLOCK_HEADER_SIZE
        val datasetEnd = checkedBlockPayloadEnd(gameData, dataset.offset, dataset.size)
        val party = findChildBlock(gameData, datasetPayload, datasetEnd, PARTY_TAG) ?: return null
        val partyPayload = party.offset + BLOCK_HEADER_SIZE
        val partyEnd = checkedBlockPayloadEnd(gameData, party.offset, party.size)
        val collection = findChildBlock(gameData, partyPayload, partyEnd, PARTY_COLLECTION_TAG) ?: return null
        return collection.offset + BLOCK_HEADER_SIZE
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

    private fun formatU32(value: Long): String = "0x%08X".format(value and 0xFFFFFFFFL)

    private data class BlockHeader(
        val offset: Int,
        val tag: Int,
        val size: Int,
    )
}
