package com.atuy.yws1editor.yokai

import java.io.IOException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class MainBinCodecTest {

    private val codec = MainBinCodec()

    @Test
    fun decodeEncodeRoundTripPreservesBytes() {
        val input = buildMainBin(
            listOf(
                fixture("head.yw", 0x10203040, 48),
                fixture("game0.yw", 0x12345678, 96),
                fixture("game1.yw", 0x23456789, 72),
                fixture("unknown.yw", 0x3456789A, 20),
            )
        )

        val decoded = codec.decode(input)
        val encoded = codec.encode(decoded)

        assertEquals(setOf("head.yw", "game0.yw", "game1.yw"), decoded.sections.keys)
        assertArrayEquals(input, encoded)
    }

    @Test
    fun decodeMatchesIeCCodeKnownVector() {
        val plaintext = ByteArray(32) { it.toByte() }
        val encrypted = hexToBytes("3bc84abb06aebf18261a68f0b2877ded801450c5d94e1cce55ff13ccb4599b2a")
        val input = buildMainBin(
            listOf(FixtureRecord("game0.yw", 0x12345678, plaintext, encrypted))
        )

        val decoded = codec.decode(input)

        assertArrayEquals(plaintext, decoded.sections.getValue("game0.yw").decryptedData)
        assertArrayEquals(input, codec.encode(decoded))
    }

    @Test
    fun decodeRejectsCorruptedInnerCrc() {
        val input = buildMainBin(listOf(fixture("game0.yw", 0x12345678, 40)))
        val payloadStart = payloadStart(input, descriptorIndex = 0)
        val payloadSize = readIntLe(input, 0x48)
        writeIntLe(input, payloadStart + payloadSize - 8, 0x76543210)
        updateOuterCrc(input)

        val error = assertThrows(IOException::class.java) { codec.decode(input) }
        assertTrue(error.message.orEmpty().contains("内部CRC"))
    }

    @Test
    fun decodeRejectsCorruptedOuterCrc() {
        val input = buildMainBin(listOf(fixture("game0.yw", 0x12345678, 40)))
        input[input.lastIndex] = (input.last().toInt() xor 0x01).toByte()

        val error = assertThrows(IOException::class.java) { codec.decode(input) }
        assertTrue(error.message.orEmpty().contains("外側CRC"))
    }

    @Test
    fun game3RecordIsDecodedWithoutSpecialOffsets() {
        val game3 = fixture("game3.yw", 0x55667788, 88)
        val input = buildMainBin(
            listOf(
                fixture("head.yw", 0x10203040, 24),
                fixture("game0.yw", 0x12345678, 32),
                fixture("game1.yw", 0x23456789, 40),
                fixture("game2.yw", 0x3456789A, 48),
                game3,
            )
        )

        val decoded = codec.decode(input)
        val replacement = game3.plaintext.copyOf().also { it[7] = 0x5A }
        val updated = codec.replaceSection(decoded, "game3.yw", replacement)
        val verified = codec.decode(updated)

        assertArrayEquals(replacement, verified.sections.getValue("game3.yw").decryptedData)
        assertEquals(calcCrc32(updated, 0x40, updated.size), readIntLe(updated, 0x04))
    }

    @Test
    fun recordsAreFoundByNameCrcWhenOrderChanges() {
        val records = listOf(
            fixture("game2.yw", 0x3456789A, 34),
            fixture("unknown.yw", 0x456789AB, 18),
            fixture("head.yw", 0x10203040, 38),
            fixture("game3.yw", 0x55667788, 42),
            fixture("game0.yw", 0x12345678, 46),
            fixture("game1.yw", 0x23456789, 50),
        )
        val input = buildMainBin(records)

        val decoded = codec.decode(input)

        for (record in records.filter { it.name != "unknown.yw" }) {
            assertArrayEquals(record.plaintext, decoded.sections.getValue(record.name).decryptedData)
        }
    }

    @Test
    fun decodeRejectsOutOfRangeOffset() {
        val input = buildMainBin(listOf(fixture("game0.yw", 0x12345678, 40)))
        writeIntLe(input, 0x44, Int.MAX_VALUE)
        updateOuterCrc(input)

        val error = assertThrows(IOException::class.java) { codec.decode(input) }
        assertTrue(error.message.orEmpty().contains("範囲外"))
    }

    @Test
    fun decodeRejectsTruncatedFile() {
        val complete = buildMainBin(listOf(fixture("game0.yw", 0x12345678, 40)))
        val truncated = complete.copyOf(complete.size - 3)

        assertThrows(IOException::class.java) { codec.decode(truncated) }
    }

    @Test
    fun decodeRejectsDuplicateRecordNames() {
        val input = buildMainBin(
            listOf(
                fixture("game0.yw", 0x12345678, 40),
                fixture("game0.yw", 0x23456789, 44),
            )
        )

        val error = assertThrows(IOException::class.java) { codec.decode(input) }
        assertTrue(error.message.orEmpty().contains("重複レコード"))
    }

    @Test
    fun decodeRejectsOverlappingPayloadRanges() {
        val input = buildMainBin(
            listOf(
                fixture("head.yw", 0x10203040, 40),
                fixture("game0.yw", 0x12345678, 44),
            )
        )
        writeIntLe(input, 0x60 + 4, 0)
        updateOuterCrc(input)

        val error = assertThrows(IOException::class.java) { codec.decode(input) }
        assertTrue(error.message.orEmpty().contains("範囲が重複"))
    }

    @Test
    fun nonZeroTableOffsetIsAccepted() {
        val input = buildMainBin(
            listOf(fixture("head.yw", 0x10203040, 40)),
            tableOffset = 0x10,
        )

        val decoded = codec.decode(input)

        assertArrayEquals(input, codec.encode(decoded))
        assertArrayEquals(
            ByteArray(40) { index -> (index * 37 + "head.yw".length * 11).toByte() },
            decoded.sections.getValue("head.yw").decryptedData,
        )
    }

    @Test
    fun descriptorPaddingBeforeAndAfterIsAccepted() {
        val input = buildMainBin(
            listOf(
                fixture("head.yw", 0x10203040, 40),
                fixture("game0.yw", 0x12345678, 44),
            ),
            tableOffset = 0x18,
            trailingTablePadding = 0x28,
        )

        val decoded = codec.decode(input)

        assertEquals(setOf("head.yw", "game0.yw"), decoded.sections.keys)
        assertArrayEquals(input, codec.encode(decoded))
    }

    @Test
    fun tableSizeMayBeLargerThanDescriptorBytes() {
        val input = buildMainBin(
            listOf(fixture("game1.yw", 0x23456789, 36)),
            trailingTablePadding = 0x40,
        )

        assertArrayEquals(input, codec.encode(codec.decode(input)))
    }

    @Test
    fun decodeRejectsDescriptorTableBeyondTableSize() {
        val input = buildMainBin(listOf(fixture("game0.yw", 0x12345678, 40)))
        writeIntLe(input, 0x08, 1)
        updateOuterCrc(input)

        val error = assertThrows(IOException::class.java) { codec.decode(input) }
        assertTrue(error.message.orEmpty().contains("tableSize"))
    }

    @Test
    fun tableHeaderBoundaryValuesAreHandledSafely() {
        val empty = buildMainBin(emptyList())
        assertTrue(codec.decode(empty).sections.isEmpty())
        assertArrayEquals(empty, codec.encode(codec.decode(empty)))

        val maxTableOffset = empty.copyOf().also {
            writeIntLe(it, 0x08, -1)
            writeIntLe(it, 0x10, -1)
            updateOuterCrc(it)
        }
        assertThrows(IOException::class.java) { codec.decode(maxTableOffset) }

        val maxTableSize = empty.copyOf().also {
            writeIntLe(it, 0x10, -1)
            updateOuterCrc(it)
        }
        assertThrows(IOException::class.java) { codec.decode(maxTableSize) }

        val maxRecordCount = empty.copyOf().also {
            writeIntLe(it, 0x0C, -1)
            updateOuterCrc(it)
        }
        assertThrows(IOException::class.java) { codec.decode(maxRecordCount) }
    }

    private fun fixture(name: String, seed: Int, size: Int): FixtureRecord {
        val plain = ByteArray(size) { index -> (index * 37 + name.length * 11).toByte() }
        return FixtureRecord(name, seed, plain, referenceCipher(plain, seed))
    }

    private fun buildMainBin(
        records: List<FixtureRecord>,
        tableOffset: Int = 0,
        trailingTablePadding: Int = 0,
    ): ByteArray {
        val tableSize = tableOffset + records.size * 0x20 + trailingTablePadding
        val payloadBase = 0x40 + tableSize
        val payloadSize = records.sumOf { it.encrypted.size + 8 }
        val out = ByteArray(payloadBase + payloadSize)
        writeIntLe(out, 0x00, 0x314E4942)
        writeIntLe(out, 0x08, tableOffset)
        writeIntLe(out, 0x0C, records.size)
        writeIntLe(out, 0x10, tableSize)

        var relativeOffset = 0
        records.forEachIndexed { index, record ->
            val descriptor = 0x40 + tableOffset + index * 0x20
            val fullPayloadSize = record.encrypted.size + 8
            writeIntLe(out, descriptor, calcCrc32(record.name.toByteArray(Charsets.UTF_8)))
            writeIntLe(out, descriptor + 4, relativeOffset)
            writeIntLe(out, descriptor + 8, fullPayloadSize)
            val start = payloadBase + relativeOffset
            record.encrypted.copyInto(out, start)
            writeIntLe(out, start + record.encrypted.size, calcCrc32(record.encrypted))
            writeIntLe(out, start + record.encrypted.size + 4, record.seed)
            relativeOffset += fullPayloadSize
        }
        updateOuterCrc(out)
        return out
    }

    private fun payloadStart(data: ByteArray, descriptorIndex: Int): Int {
        val tableSize = readIntLe(data, 0x10)
        val descriptor = 0x40 + readIntLe(data, 0x08) + descriptorIndex * 0x20
        return 0x40 + tableSize + readIntLe(data, descriptor + 4)
    }

    private fun updateOuterCrc(data: ByteArray) {
        writeIntLe(data, 0x04, calcCrc32(data, 0x40, data.size))
    }

    private fun calcCrc32(data: ByteArray, start: Int = 0, end: Int = data.size): Int {
        var crc = 0xFFFFFFFF.toInt()
        for (index in start until end) {
            repeat(8) { bit ->
                if (bit == 0) crc = crc xor (data[index].toInt() and 0xFF)
                crc = if ((crc and 1) != 0) 0xEDB88320.toInt() xor (crc ushr 1) else crc ushr 1
            }
        }
        return crc xor 0xFFFFFFFF.toInt()
    }

    private fun referenceCipher(data: ByteArray, seed: Int): ByteArray {
        require(seed != 0)
        val sbox = IntArray(256) { it }
        var s0 = (seed xor (seed ushr 30)) * 0x6C078965 + 1
        var s1 = (s0 xor (s0 ushr 30)) * 0x6C078965 + 2
        var s2 = (s1 xor (s1 ushr 30)) * 0x6C078965 + 3
        var s3 = 0x03DF95B3
        repeat(4096) {
            val t = s0 xor (s0 shl 11)
            s0 = s1
            s1 = s2
            s2 = s3
            s3 = s3 xor (s3 ushr 19) xor t xor (t ushr 8)
            val first = (s3 ushr 8) and 0xFF
            val second = s3 and 0xFF
            if (first != second) {
                val firstValue = sbox[first]
                val secondValue = sbox[second]
                val tmp = sbox[firstValue]
                sbox[firstValue] = sbox[secondValue]
                sbox[secondValue] = tmp
            }
        }

        val primes = generatePrimes()
        val out = data.copyOf()
        var multiplier = 0
        for (index in out.indices) {
            val byteIndex = index and 0xFF
            if (byteIndex == 0) multiplier = primes[sbox[(index ushr 8) and 0xFF]]
            val keyIndex = (multiplier * (byteIndex + 1)) and 0xFF
            out[index] = (out[index].toInt() xor sbox[keyIndex]).toByte()
        }
        return out
    }

    private fun generatePrimes(): IntArray {
        val result = ArrayList<Int>(256)
        var candidate = 3
        while (result.size < 256) {
            var prime = true
            var divisor = 2
            while (divisor * divisor <= candidate) {
                if (candidate % divisor == 0) {
                    prime = false
                    break
                }
                divisor++
            }
            if (prime) result += candidate
            candidate += 2
        }
        return result.toIntArray()
    }

    private fun readIntLe(data: ByteArray, offset: Int): Int =
        (data[offset].toInt() and 0xFF) or
            ((data[offset + 1].toInt() and 0xFF) shl 8) or
            ((data[offset + 2].toInt() and 0xFF) shl 16) or
            ((data[offset + 3].toInt() and 0xFF) shl 24)

    private fun writeIntLe(data: ByteArray, offset: Int, value: Int) {
        data[offset] = value.toByte()
        data[offset + 1] = (value ushr 8).toByte()
        data[offset + 2] = (value ushr 16).toByte()
        data[offset + 3] = (value ushr 24).toByte()
    }

    private fun hexToBytes(value: String): ByteArray =
        ByteArray(value.length / 2) { index -> value.substring(index * 2, index * 2 + 2).toInt(16).toByte() }

    private data class FixtureRecord(
        val name: String,
        val seed: Int,
        val plaintext: ByteArray,
        val encrypted: ByteArray,
    )
}
