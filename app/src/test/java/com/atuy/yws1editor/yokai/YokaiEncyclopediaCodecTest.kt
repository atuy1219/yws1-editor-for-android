package com.atuy.yws1editor.yokai

import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class YokaiEncyclopediaCodecTest {
    private val codec = YokaiEncyclopediaCodec()
    private val komajiroId = 0x250C25C6L
    private val masterData = YokaiMasterData(
        nameById = mapOf(komajiroId to "コマじろう"),
        numberById = mapOf(komajiroId to 101),
        detailById = emptyMap(),
        attitudes = emptyList(),
    )

    @Test
    fun decodesKnownKomajiroOwnedFlagLocation() {
        val data = fixture()
        data[YokaiEncyclopediaCodec.OWNED_FLAGS_OFFSET + 101 / 8] = 0x20

        val entry = codec.decode(data, masterData).single()

        assertEquals(101, entry.number)
        assertFalse(entry.met)
        assertTrue(entry.owned)
        assertFalse(entry.isNew)
    }

    @Test
    fun applyEntriesUpdatesOnlyDictionaryFlagSets() {
        val data = fixture()
        data[YokaiEncyclopediaCodec.MET_FLAGS_OFFSET - 1] = 0x55
        val updated = codec.applyEntries(
            data,
            listOf(
                YokaiEncyclopediaEntry(
                    id = komajiroId,
                    number = 101,
                    name = "コマじろう",
                    met = true,
                    owned = true,
                    isNew = false,
                ),
            ),
        )

        val byteIndex = 101 / 8
        assertEquals(0x20, updated[YokaiEncyclopediaCodec.MET_FLAGS_OFFSET + byteIndex].toInt() and 0xff)
        assertEquals(0x20, updated[YokaiEncyclopediaCodec.OWNED_FLAGS_OFFSET + byteIndex].toInt() and 0xff)
        assertEquals(0x00, updated[YokaiEncyclopediaCodec.NEW_FLAGS_OFFSET + byteIndex].toInt() and 0xff)
        assertEquals(0x55, updated[YokaiEncyclopediaCodec.MET_FLAGS_OFFSET - 1].toInt() and 0xff)
    }

    @Test
    fun rejectsShortData() {
        assertThrows(IOException::class.java) {
            codec.decode(ByteArray(YokaiEncyclopediaCodec.FLAG_PAYLOAD_OFFSET), masterData)
        }
    }

    private fun fixture(): ByteArray {
        return ByteArray(YokaiEncyclopediaCodec.FLAG_PAYLOAD_OFFSET + YokaiEncyclopediaCodec.FLAG_PAYLOAD_SIZE)
    }
}
