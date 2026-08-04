package com.atuy.yws1editor.yokai

import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class PartyCodecTest {
    private val codec = PartyCodec()

    @Test
    fun decodeReadsCurrentPartyHandles() {
        val handles = handles(1, 2, 3, 4, 5, 6)
        val gameData = buildGameData(collection(active = handles))
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
    fun replacePartyMembersReordersActiveSlotsWithoutChangingCollectionMembers() {
        val originalHandles = handles(1, 2, 3, 4, 5, 6)
        val replacementHandles = originalHandles.reversed()
        val originalCollection = collection(active = originalHandles)
        val gameData = buildGameData(originalCollection)

        val updated = codec.replacePartyMembers(gameData, replacementHandles, originalHandles)
        val updatedCollection = readCollectionHandles(updated)

        assertEquals(replacementHandles, updatedCollection.take(PartyCodec.PARTY_SIZE))
        assertEquals(originalCollection.groupingBy { it }.eachCount(), updatedCollection.groupingBy { it }.eachCount())
    }

    @Test
    fun replacePartyMembersSwapsReserveMemberWithDisplacedPartyMember() {
        val originalParty = handles(1, 2, 3, 4, 5, 6)
        val reserveMember = handle(7)
        val originalCollection = collection(
            active = originalParty,
            reserve = listOf(reserveMember),
        )
        val replacement = listOf(
            reserveMember,
            originalParty[1],
            originalParty[2],
            originalParty[3],
            originalParty[4],
            originalParty[5],
        )

        val updated = codec.replacePartyMembers(buildGameData(originalCollection), replacement, originalCollection.filter { it != 0L })
        val updatedCollection = readCollectionHandles(updated)

        assertEquals(replacement, updatedCollection.take(PartyCodec.PARTY_SIZE))
        assertEquals(originalParty[0], updatedCollection[PartyCodec.PARTY_SIZE])
        assertEquals(originalCollection.groupingBy { it }.eachCount(), updatedCollection.groupingBy { it }.eachCount())
    }

    @Test
    fun replacePartyMembersMovesRemovedMemberToEmptyReserveSlot() {
        val originalParty = handles(1, 2, 3, 4, 5, 6)
        val originalCollection = collection(active = originalParty)
        val replacement = listOf(
            originalParty[0],
            0L,
            originalParty[2],
            originalParty[3],
            originalParty[4],
            originalParty[5],
        )

        val updated = codec.replacePartyMembers(buildGameData(originalCollection), replacement, originalCollection.filter { it != 0L })
        val updatedCollection = readCollectionHandles(updated)

        assertEquals(replacement, updatedCollection.take(PartyCodec.PARTY_SIZE))
        assertEquals(originalParty[1], updatedCollection[PartyCodec.PARTY_SIZE])
        assertEquals(originalCollection.groupingBy { it }.eachCount(), updatedCollection.groupingBy { it }.eachCount())
    }

    @Test
    fun replacePartyMembersRestoresValidHandleMissingFromCollection() {
        val originalParty = handles(1, 2, 3, 4, 5, 6)
        val droppedHandle = 0x002D002CL
        val originalCollection = collection(active = originalParty)
        val replacement = originalParty.toMutableList().apply { this[0] = droppedHandle }

        val updated = codec.replacePartyMembers(
            gameData = buildGameData(originalCollection),
            handles = replacement,
            validHandles = originalParty + droppedHandle,
        )
        val updatedCollection = readCollectionHandles(updated)

        assertEquals(replacement, updatedCollection.take(PartyCodec.PARTY_SIZE))
        assertEquals(originalParty[0], updatedCollection[PartyCodec.PARTY_SIZE])
        assertEquals(1, updatedCollection.count { it == droppedHandle })
        assertEquals(
            originalCollection.filter { it != 0L }.toSet() + droppedHandle,
            updatedCollection.filter { it != 0L }.toSet(),
        )
    }

    @Test
    fun replacePartyMembersRejectsDuplicatePartyHandles() {
        val originalParty = handles(1, 2, 3, 4, 5, 6)
        val duplicateParty = listOf(
            originalParty[0],
            originalParty[0],
            originalParty[2],
            originalParty[3],
            originalParty[4],
            originalParty[5],
        )

        assertIOExceptionContains("複数のパーティ枠") {
            codec.replacePartyMembers(buildGameData(collection(active = originalParty)), duplicateParty, originalParty)
        }
    }

    @Test
    fun replacePartyMembersRejectsHandleOutsidePartyCollection() {
        val originalParty = handles(1, 2, 3, 4, 5, 6)
        val replacement = originalParty.toMutableList().apply { this[0] = handle(99) }

        assertIOExceptionContains("妖怪スロットに存在しません") {
            codec.replacePartyMembers(buildGameData(collection(active = originalParty)), replacement, originalParty)
        }
    }

    @Test
    fun replacePartyMembersDoesNotIncreaseDuplicatesInPreviouslyCorruptedCollection() {
        val originalCollection = collection(
            active = listOf(handle(1), handle(2), handle(3), handle(4), handle(4), handle(6)),
            reserve = listOf(handle(5), handle(4)),
        )
        val replacement = handles(1, 2, 3, 4, 5, 6)

        val updated = codec.replacePartyMembers(buildGameData(originalCollection), replacement, originalCollection.filter { it != 0L })
        val updatedCollection = readCollectionHandles(updated)

        assertEquals(replacement, updatedCollection.take(PartyCodec.PARTY_SIZE))
        assertEquals(originalCollection.groupingBy { it }.eachCount(), updatedCollection.groupingBy { it }.eachCount())
    }

    @Test
    fun decodeReturnsEmptyWhenPartyBlockIsMissing() {
        assertTrue(codec.decode(ByteArray(32), emptyList()).isEmpty())
    }

    private fun assertIOExceptionContains(expected: String, block: () -> Unit) {
        try {
            block()
            fail("IOException が発生しませんでした")
        } catch (e: IOException) {
            assertTrue("実際のメッセージ: ${e.message}", e.message.orEmpty().contains(expected))
        }
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

    private fun handle(number: Int): Long {
        return ((number.toLong() shl 16) or (number - 1).toLong()) and 0xFFFFFFFFL
    }

    private fun handles(vararg numbers: Int): List<Long> = numbers.map(::handle)

    private fun collection(
        active: List<Long>,
        reserve: List<Long> = emptyList(),
    ): List<Long> {
        require(active.size == PartyCodec.PARTY_SIZE)
        return MutableList(COLLECTION_SIZE) { 0L }.apply {
            active.forEachIndexed { index, handle -> this[index] = handle }
            reserve.forEachIndexed { index, handle -> this[PartyCodec.PARTY_SIZE + index] = handle }
        }
    }

    private fun buildGameData(collectionHandles: List<Long>): ByteArray {
        require(collectionHandles.size == COLLECTION_SIZE)
        val sub09 = block(0x09, ByteArray(0x18))
        val collectionPayload = ByteArray(COLLECTION_SIZE * Int.SIZE_BYTES)
        collectionHandles.forEachIndexed { index, handle ->
            writeUInt32Le(collectionPayload, index * Int.SIZE_BYTES, handle)
        }
        val partyPayload = sub09 + block(0x0A, collectionPayload)
        val datasetPayload = block(0x01, ByteArray(4)) + block(0x08, partyPayload)
        val outerPayload = block(0xF2, ByteArray(4)) + block(0xF3, datasetPayload)
        return block(0xF1, outerPayload)
    }

    private fun readCollectionHandles(data: ByteArray): List<Long> {
        val offset = partyPayloadOffset()
        return (0 until COLLECTION_SIZE).map { readUInt32Le(data, offset + it * Int.SIZE_BYTES) }
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

    private companion object {
        const val COLLECTION_SIZE = 241
    }
}
