package com.atuy.yws1editor.yokai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PartyCodecTest {
    private val codec = PartyCodec()

    @Test
    fun decodeReadsCurrentPartyHandles() {
        val handles = listOf(0x00010000L, 0x00020001L, 0x00030002L, 0x00040003L, 0x00050004L, 0x00060005L)
        val gameData = buildGameData(handles)
        val entries = handles.mapIndexed { index, handle ->
            entry(slot = index, handle = handle, name = "妖怪$index")
        }

        val members = codec.decode(gameData, entries)

        assertEquals(6, members.size)
        assertEquals("妖怪0", members[0].yokaiName)
        assertEquals(2, members[2].yokaiSlot)
        assertEquals(handles, members.map { it.yokaiHandle })
    }

    @Test
    fun replacePartyMembersUpdatesOnlyCurrentPartyHandles() {
        val originalHandles = listOf(0x00010000L, 0x00020001L, 0x00030002L, 0x00040003L, 0x00050004L, 0x00060005L)
        val replacementHandles = originalHandles.reversed()
        val gameData = buildGameData(originalHandles)

        val updated = codec.replacePartyMembers(gameData, replacementHandles)

        assertEquals(replacementHandles, readPartyHandles(updated))
        assertEquals(0x7F7F7F7FL, readUInt32Le(updated, partyPayloadOffset() + 6 * Int.SIZE_BYTES))
    }

    @Test
    fun decodeReturnsEmptyWhenPartyBlockIsMissing() {
        assertTrue(codec.decode(ByteArray(32), emptyList()).isEmpty())
    }

    private fun entry(slot: Int, handle: Long, name: String): YokaiEntry {
        return YokaiEntry(
            slot = slot,
            handle = handle,
            id = slot.toLong() + 1,
            name = name,
            level = 1,
            attackLevel = 1,
            techniqueLevel = 1,
            soultimateLevel = 1,
            attitudeId = 0,
            majimeCorrection = 0,
            stateFlags = 0,
            iva = Stat5(0, 0, 0, 0, 0),
            ivb1 = Stat5(0, 0, 0, 0, 0),
            ivb2 = Stat5(0, 0, 0, 0, 0),
            cb = Stat5(0, 0, 0, 0, 0),
        )
    }

    private fun buildGameData(handles: List<Long>): ByteArray {
        val sub09 = block(0x09, ByteArray(0x18))
        val collectionPayload = ByteArray(0x3C4) { 0x7F }
        handles.forEachIndexed { index, handle ->
            writeUInt32Le(collectionPayload, index * Int.SIZE_BYTES, handle)
        }
        val partyPayload = sub09 + block(0x0A, collectionPayload)
        val datasetPayload = block(0x01, ByteArray(4)) + block(0x08, partyPayload)
        val outerPayload = block(0xF2, ByteArray(4)) + block(0xF3, datasetPayload)
        return block(0xF1, outerPayload)
    }

    private fun readPartyHandles(data: ByteArray): List<Long> {
        val offset = partyPayloadOffset()
        return (0 until 6).map { readUInt32Le(data, offset + it * Int.SIZE_BYTES) }
    }

    private fun partyPayloadOffset(): Int {
        val outerPayload = 8
        val versionBlockTotal = 8 + 4 + 4
        val datasetBlockHeader = outerPayload + versionBlockTotal
        val datasetPayload = datasetBlockHeader + 8
        val flagBlockTotal = 8 + 4 + 4
        val partyBlockHeader = datasetPayload + flagBlockTotal
        val partyPayload = partyBlockHeader + 8
        val sub09Total = 8 + 0x18 + 4
        return partyPayload + sub09Total + 8
    }

    private fun block(tag: Int, payload: ByteArray): ByteArray {
        val out = ByteArray(8 + payload.size + 4)
        writeUInt32Le(out, 0, 0x0000FFFEL)
        writeUInt32Le(out, 4, ((payload.size.toLong() shl 8) or tag.toLong()))
        payload.copyInto(out, 8)
        writeUInt32Le(out, 8 + payload.size, 0x0000FFFEL)
        return out
    }

    private fun readUInt32Le(data: ByteArray, offset: Int): Long {
        return (data[offset].toLong() and 0xFFL) or
            ((data[offset + 1].toLong() and 0xFFL) shl 8) or
            ((data[offset + 2].toLong() and 0xFFL) shl 16) or
            ((data[offset + 3].toLong() and 0xFFL) shl 24)
    }

    private fun writeUInt32Le(data: ByteArray, offset: Int, value: Long) {
        val v = value.toInt()
        data[offset] = (v and 0xFF).toByte()
        data[offset + 1] = ((v ushr 8) and 0xFF).toByte()
        data[offset + 2] = ((v ushr 16) and 0xFF).toByte()
        data[offset + 3] = ((v ushr 24) and 0xFF).toByte()
    }
}
