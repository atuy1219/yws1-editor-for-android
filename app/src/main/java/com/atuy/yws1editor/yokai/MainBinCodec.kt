package com.atuy.yws1editor.yokai

import java.io.IOException

class MainBinCodec {

    private companion object {
        const val HEADER_SIZE = 0x40
        const val RECORD_SIZE = 0x20
        const val HEADER_RECORD_TABLE_OFFSET = 0x08
        const val HEADER_RECORD_COUNT_OFFSET = 0x0C
        const val HEADER_RECORD_TABLE_SIZE_OFFSET = 0x10
        const val HEADER_OUTER_CRC_OFFSET = 0x04
        const val MAX_RECORDS = 16
        const val RECORD_NAME_CRC_OFFSET = 0x00
        const val RECORD_PAYLOAD_OFFSET = 0x04
        const val RECORD_PAYLOAD_SIZE_OFFSET = 0x08
        const val PAYLOAD_TRAILER_SIZE = 8

        val TARGET_NAMES = listOf("head.yw", "game0.yw", "game1.yw", "game2.yw", "game3.yw")
    }

    private val crcTable: IntArray = IntArray(256) { i ->
        var c = i
        repeat(8) {
            c = if ((c and 1) != 0) 0xEDB88320.toInt() xor (c ushr 1) else c ushr 1
        }
        c
    }

    private val targetNameByCrc: Map<Int, String> = TARGET_NAMES.associateBy { name ->
        calcCrc32(name.toByteArray(Charsets.UTF_8))
    }
    private val primes: IntArray = generatePrimes()

    fun decode(mainBin: ByteArray): MainBinDecoded {
        val layout = parseLayout(mainBin)
        val sections = linkedMapOf<String, MainSection>()

        for (record in layout.records) {
            val name = targetNameByCrc[record.nameCrc] ?: continue
            if (record.payloadSize <= PAYLOAD_TRAILER_SIZE) {
                throw IOException("$name のペイロードが短すぎます")
            }

            val encryptedSize = record.payloadSize - PAYLOAD_TRAILER_SIZE
            val encryptedEnd = record.payloadStart + encryptedSize
            val storedCrc = readIntLe(mainBin, encryptedEnd)
            val seed = readIntLe(mainBin, encryptedEnd + Int.SIZE_BYTES)
            if (seed == 0) {
                throw IOException("$name のseedが不正です")
            }

            val encrypted = mainBin.copyOfRange(record.payloadStart, encryptedEnd)
            val actualCrc = calcCrc32(encrypted)
            if (storedCrc != actualCrc) {
                throw IOException("$name の内部CRCが一致しません")
            }

            sections[name] = MainSection(
                name = name,
                headerPos = record.descriptorStart,
                offset = record.payloadOffset,
                size = record.payloadSize,
                seed = seed,
                decryptedData = cipherData(encrypted, seed),
            )
        }

        return MainBinDecoded(rawData = mainBin.copyOf(), sections = sections)
    }

    /** Re-encodes every recognized section while preserving record order, unknown records, and seeds. */
    fun encode(decoded: MainBinDecoded): ByteArray {
        val out = decoded.rawData.copyOf()
        val layout = parseLayout(out)
        for (section in decoded.sections.values) {
            writeSection(out, layout, section.name, section.decryptedData, section.seed)
        }
        updateOuterCrc(out)
        return out
    }

    fun replaceSection(
        decoded: MainBinDecoded,
        targetName: String,
        newDecryptedData: ByteArray,
    ): ByteArray {
        val section = decoded.sections[targetName] ?: throw IOException("$targetName が存在しません")
        val out = decoded.rawData.copyOf()
        val layout = parseLayout(out)
        writeSection(out, layout, targetName, newDecryptedData, section.seed)
        updateOuterCrc(out)
        return out
    }

    private fun writeSection(
        out: ByteArray,
        layout: ContainerLayout,
        targetName: String,
        newDecryptedData: ByteArray,
        seed: Int,
    ) {
        if (seed == 0) throw IOException("$targetName のseedが不正です")
        val expectedNameCrc = calcCrc32(targetName.toByteArray(Charsets.UTF_8))
        val record = layout.records.singleOrNull { it.nameCrc == expectedNameCrc }
            ?: throw IOException("$targetName のレコードが存在しません")
        val expectedEncryptedSize = record.payloadSize - PAYLOAD_TRAILER_SIZE
        if (expectedEncryptedSize < 0 || newDecryptedData.size != expectedEncryptedSize) {
            throw IOException("$targetName のサイズが一致しません")
        }

        val encrypted = cipherData(newDecryptedData, seed)
        encrypted.copyInto(out, record.payloadStart)
        val trailerStart = record.payloadStart + encrypted.size
        writeIntLe(out, trailerStart, calcCrc32(encrypted))
        writeIntLe(out, trailerStart + Int.SIZE_BYTES, seed)
    }

    private fun parseLayout(mainBin: ByteArray): ContainerLayout {
        if (mainBin.size < HEADER_SIZE) {
            throw IOException("main.bin が短すぎます")
        }

        val storedOuterCrc = readIntLe(mainBin, HEADER_OUTER_CRC_OFFSET)
        val actualOuterCrc = calcCrc32(mainBin, HEADER_SIZE, mainBin.size)
        if (storedOuterCrc != actualOuterCrc) {
            throw IOException("main.bin の外側CRCが一致しません")
        }

        val tableOffset = readUInt32Le(mainBin, HEADER_RECORD_TABLE_OFFSET)
        val recordCount = readUInt32Le(mainBin, HEADER_RECORD_COUNT_OFFSET)
        val tableSize = readUInt32Le(mainBin, HEADER_RECORD_TABLE_SIZE_OFFSET)
        if (recordCount > MAX_RECORDS.toLong()) {
            throw IOException("main.bin のレコード数が不正です: $recordCount")
        }
        val descriptorBytes = checkedMultiply(recordCount, RECORD_SIZE.toLong(), "記述子テーブルサイズ")
        val descriptorRelativeEnd = checkedAdd(tableOffset, descriptorBytes, "記述子テーブル終端")
        if (descriptorRelativeEnd > tableSize) {
            throw IOException("main.bin の記述子テーブルがtableSizeを越えています")
        }

        val descriptorStartLong = checkedAdd(HEADER_SIZE.toLong(), tableOffset, "記述子テーブル位置")
        val descriptorEndLong = checkedAdd(descriptorStartLong, descriptorBytes, "記述子テーブル終端")
        val payloadBaseLong = checkedAdd(HEADER_SIZE.toLong(), tableSize, "ペイロード基点")
        if (descriptorEndLong > mainBin.size.toLong()) {
            throw IOException("main.bin の記述子テーブルが範囲外です")
        }
        if (descriptorEndLong > payloadBaseLong) {
            throw IOException("main.bin の記述子テーブルがペイロード基点を越えています")
        }
        val descriptorStart = checkedPosition(descriptorStartLong, mainBin.size, "記述子テーブル")
        val payloadBase = checkedPosition(payloadBaseLong, mainBin.size, "ペイロード基点")

        val records = ArrayList<ContainerRecord>(recordCount.toInt())
        val seenNameCrc = HashSet<Int>()
        repeat(recordCount.toInt()) { index ->
            val descriptor = descriptorStart + index * RECORD_SIZE
            if (descriptor < 0 || descriptor.toLong() + RECORD_SIZE > descriptorEndLong) {
                throw IOException("main.bin の記述子が範囲外です")
            }

            val nameCrc = readIntLe(mainBin, descriptor + RECORD_NAME_CRC_OFFSET)
            if (!seenNameCrc.add(nameCrc)) {
                throw IOException("main.bin に重複レコードがあります")
            }
            val payloadOffsetLong = readUInt32Le(mainBin, descriptor + RECORD_PAYLOAD_OFFSET)
            val payloadSizeLong = readUInt32Le(mainBin, descriptor + RECORD_PAYLOAD_SIZE_OFFSET)
            val payloadStartLong = payloadBase.toLong() + payloadOffsetLong
            val payloadEndLong = payloadStartLong + payloadSizeLong
            if (
                payloadStartLong < payloadBase.toLong() ||
                payloadEndLong < payloadStartLong ||
                payloadEndLong > mainBin.size.toLong()
            ) {
                throw IOException("main.bin のレコード[$index]が範囲外です")
            }

            records += ContainerRecord(
                descriptorStart = descriptor,
                nameCrc = nameCrc,
                payloadOffset = payloadOffsetLong.toIntChecked("ペイロード相対オフセット"),
                payloadSize = payloadSizeLong.toIntChecked("ペイロードサイズ"),
                payloadStart = payloadStartLong.toIntChecked("ペイロード位置"),
            )
        }

        records.asSequence()
            .filter { it.payloadSize > 0 }
            .sortedBy { it.payloadStart }
            .zipWithNext()
            .forEach { (left, right) ->
                if (left.payloadStart.toLong() + left.payloadSize > right.payloadStart.toLong()) {
                    throw IOException("main.bin のレコード範囲が重複しています")
                }
            }

        return ContainerLayout(records)
    }

    private fun checkedPosition(value: Long, fileSize: Int, label: String): Int {
        if (value < 0 || value > fileSize.toLong()) throw IOException("main.bin の${label}が範囲外です")
        return value.toInt()
    }

    private fun checkedAdd(left: Long, right: Long, label: String): Long {
        if (left < 0 || right < 0 || left > Long.MAX_VALUE - right) {
            throw IOException("main.bin の${label}でオーバーフローしました")
        }
        return left + right
    }

    private fun checkedMultiply(left: Long, right: Long, label: String): Long {
        if (left < 0 || right < 0 || (right != 0L && left > Long.MAX_VALUE / right)) {
            throw IOException("main.bin の${label}でオーバーフローしました")
        }
        return left * right
    }

    private fun Long.toIntChecked(label: String): Int {
        if (this < 0 || this > Int.MAX_VALUE.toLong()) throw IOException("main.bin の${label}が大きすぎます")
        return toInt()
    }

    private fun updateOuterCrc(data: ByteArray) {
        writeIntLe(data, HEADER_OUTER_CRC_OFFSET, calcCrc32(data, HEADER_SIZE, data.size))
    }

    private fun calcCrc32(data: ByteArray): Int = calcCrc32(data, 0, data.size)

    private fun calcCrc32(data: ByteArray, startIndex: Int, endIndex: Int): Int {
        var crc = 0xFFFFFFFF.toInt()
        for (index in startIndex until endIndex) {
            crc = crcTable[(crc xor data[index].toInt()) and 0xFF] xor (crc ushr 8)
        }
        return crc xor 0xFFFFFFFF.toInt()
    }

    private fun generatePrimes(): IntArray {
        val count = 256
        val list = ArrayList<Int>(count)
        var num = 3
        while (list.size < count) {
            var isPrime = true
            var i = 2
            while (i * i <= num) {
                if (num % i == 0) {
                    isPrime = false
                    break
                }
                i++
            }
            if (isPrime) list.add(num)
            num += 2
        }
        return list.toIntArray()
    }

    private fun makeSbox(seed: Int): IntArray {
        require(seed != 0) { "seed must not be zero" }
        val box = IntArray(256) { it }
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

            val idx1 = (s3 ushr 8) and 0xFF
            val idx2 = s3 and 0xFF
            if (idx1 != idx2) {
                val value1 = box[idx1]
                val value2 = box[idx2]
                val tmp = box[value1]
                box[value1] = box[value2]
                box[value2] = tmp
            }
        }
        return box
    }

    private fun cipherData(data: ByteArray, seed: Int): ByteArray {
        val sbox = makeSbox(seed)
        val out = data.copyOf()
        var multiplier = 0
        for (i in out.indices) {
            val blockIndex = (i ushr 8) and 0xFF
            val byteIndex = i and 0xFF
            if (byteIndex == 0) multiplier = primes[sbox[blockIndex]]
            val keyIndex = (multiplier * (byteIndex + 1)) and 0xFF
            out[i] = (out[i].toInt() xor sbox[keyIndex]).toByte()
        }
        return out
    }

    private fun readUInt32Le(data: ByteArray, offset: Int): Long =
        readIntLe(data, offset).toLong() and 0xFFFFFFFFL

    private fun readIntLe(data: ByteArray, offset: Int): Int {
        if (offset < 0 || offset.toLong() + Int.SIZE_BYTES > data.size.toLong()) {
            throw IOException("main.bin の整数フィールドが範囲外です")
        }
        return (data[offset].toInt() and 0xFF) or
            ((data[offset + 1].toInt() and 0xFF) shl 8) or
            ((data[offset + 2].toInt() and 0xFF) shl 16) or
            ((data[offset + 3].toInt() and 0xFF) shl 24)
    }

    private fun writeIntLe(data: ByteArray, offset: Int, value: Int) {
        if (offset < 0 || offset.toLong() + Int.SIZE_BYTES > data.size.toLong()) {
            throw IOException("main.bin の書き込み位置が範囲外です")
        }
        data[offset] = (value and 0xFF).toByte()
        data[offset + 1] = ((value ushr 8) and 0xFF).toByte()
        data[offset + 2] = ((value ushr 16) and 0xFF).toByte()
        data[offset + 3] = ((value ushr 24) and 0xFF).toByte()
    }

    private data class ContainerLayout(val records: List<ContainerRecord>)

    private data class ContainerRecord(
        val descriptorStart: Int,
        val nameCrc: Int,
        val payloadOffset: Int,
        val payloadSize: Int,
        val payloadStart: Int,
    )
}
