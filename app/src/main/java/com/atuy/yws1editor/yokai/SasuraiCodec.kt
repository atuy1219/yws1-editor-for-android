package com.atuy.yws1editor.yokai

data class SasuraiYokaiSummary(
    val nickname: String,
    val equipmentConfigId: Long,
    val yokaiId: Long,
    val rawSummary: ByteArray,
)

data class SasuraiEncounterOption(
    val encounterId: Long,
    val representativeYokaiId: Long,
    val representativeName: String,
    val yokaiIds: List<Long>,
    val yokaiNames: List<String>,
)

data class SasuraiResident(
    val index: Int,
    val isUsed: Boolean,
    val sequence: Int,
    val state: Int,
    val encounterId: Long,
    val displayName: String,
    val yokai: List<SasuraiYokaiSummary>,
    val statusByte: Int,
    val rawEntry: ByteArray,
)

class SasuraiCodec {
    companion object {
        const val OFFSET = 0x9610
        const val ENTRY_COUNT = 9
        const val STRIDE = 0x19C
        const val REGION_SIZE = ENTRY_COUNT * STRIDE

        private const val SEQUENCE_FIELD = 0x00
        private const val STATE_FIELD = 0x02
        private const val ENCOUNTER_ID_FIELD = 0x04
        private const val DISPLAY_NAME_FIELD = 0x08
        private const val DISPLAY_NAME_SIZE = 64
        private val YOKAI_FIELDS = intArrayOf(0x4c, 0xbc, 0x12c)
        private const val YOKAI_SIZE = 0x70
        private const val YOKAI_NAME_FIELD = 0x00
        private const val YOKAI_NAME_SIZE = 64
        private const val YOKAI_EQUIPMENT_FIELD = 0x40
        private const val YOKAI_ID_FIELD = 0x50
        private const val STATUS_FIELD = 0x198
    }

    fun decode(gameData: ByteArray): List<SasuraiResident> {
        SaveDataBinary.requireRange(gameData, OFFSET, REGION_SIZE, "さすらい荘領域")
        return List(ENTRY_COUNT) { index ->
            val offset = OFFSET + index * STRIDE
            val summaries = YOKAI_FIELDS.map { relative ->
                val summary = offset + relative
                SasuraiYokaiSummary(
                    nickname = readNullTerminatedUtf8(gameData, summary + YOKAI_NAME_FIELD, YOKAI_NAME_SIZE),
                    equipmentConfigId = SaveDataBinary.readUInt32Le(
                        gameData,
                        summary + YOKAI_EQUIPMENT_FIELD,
                    ),
                    yokaiId = SaveDataBinary.readUInt32Le(gameData, summary + YOKAI_ID_FIELD),
                    rawSummary = gameData.copyOfRange(summary, summary + YOKAI_SIZE),
                )
            }
            SasuraiResident(
                index = index,
                isUsed = SaveDataBinary.readUInt32Le(gameData, offset) != 0L,
                sequence = SaveDataBinary.readUInt16Le(gameData, offset + SEQUENCE_FIELD),
                state = SaveDataBinary.readUInt16Le(gameData, offset + STATE_FIELD),
                encounterId = SaveDataBinary.readUInt32Le(gameData, offset + ENCOUNTER_ID_FIELD),
                displayName = readNullTerminatedUtf8(gameData, offset + DISPLAY_NAME_FIELD, DISPLAY_NAME_SIZE),
                yokai = summaries,
                statusByte = gameData[offset + STATUS_FIELD].toInt() and 0xff,
                rawEntry = gameData.copyOfRange(offset, offset + STRIDE),
            )
        }
    }

    fun replaceResidentEntry(gameData: ByteArray, index: Int, rawEntry: ByteArray): ByteArray {
        requireResidentIndex(index)
        if (rawEntry.size != STRIDE) {
            throw java.io.IOException("さすらい荘entryサイズが不正です")
        }
        SaveDataBinary.requireRange(gameData, OFFSET, REGION_SIZE, "さすらい荘領域")
        val out = gameData.copyOf()
        rawEntry.copyInto(out, OFFSET + index * STRIDE)
        return out
    }

    fun replaceEncounter(resident: SasuraiResident, option: SasuraiEncounterOption): SasuraiResident {
        requireResidentIndex(resident.index)
        if (!resident.isUsed) return resident
        if (resident.encounterId == option.encounterId) return resident

        val raw = resident.rawEntry.copyOf()
        writeUInt32Le(raw, ENCOUNTER_ID_FIELD, option.encounterId)
        YOKAI_FIELDS.forEachIndexed { index, relative ->
            val yokaiId = option.yokaiIds.getOrNull(index) ?: 0L
            writeUInt32Le(raw, relative + YOKAI_ID_FIELD, yokaiId)
        }

        return decodeResident(resident.index, raw)
    }

    private fun decodeResident(index: Int, rawEntry: ByteArray): SasuraiResident {
        if (rawEntry.size != STRIDE) {
            throw java.io.IOException("さすらい荘entryサイズが不正です")
        }
        val summaries = YOKAI_FIELDS.map { relative ->
            SasuraiYokaiSummary(
                nickname = readNullTerminatedUtf8(rawEntry, relative + YOKAI_NAME_FIELD, YOKAI_NAME_SIZE),
                equipmentConfigId = SaveDataBinary.readUInt32Le(rawEntry, relative + YOKAI_EQUIPMENT_FIELD),
                yokaiId = SaveDataBinary.readUInt32Le(rawEntry, relative + YOKAI_ID_FIELD),
                rawSummary = rawEntry.copyOfRange(relative, relative + YOKAI_SIZE),
            )
        }
        return SasuraiResident(
            index = index,
            isUsed = SaveDataBinary.readUInt32Le(rawEntry, 0) != 0L,
            sequence = SaveDataBinary.readUInt16Le(rawEntry, SEQUENCE_FIELD),
            state = SaveDataBinary.readUInt16Le(rawEntry, STATE_FIELD),
            encounterId = SaveDataBinary.readUInt32Le(rawEntry, ENCOUNTER_ID_FIELD),
            displayName = readNullTerminatedUtf8(rawEntry, DISPLAY_NAME_FIELD, DISPLAY_NAME_SIZE),
            yokai = summaries,
            statusByte = rawEntry[STATUS_FIELD].toInt() and 0xff,
            rawEntry = rawEntry.copyOf(),
        )
    }

    private fun readNullTerminatedUtf8(data: ByteArray, offset: Int, size: Int): String {
        SaveDataBinary.requireRange(data, offset, size, "文字列")
        var end = offset
        while (end < offset + size && data[end] != 0.toByte()) end++
        return data.copyOfRange(offset, end).toString(Charsets.UTF_8)
    }

    private fun requireResidentIndex(index: Int) {
        if (index !in 0 until ENTRY_COUNT) {
            throw java.io.IOException("さすらい荘entry indexが不正です")
        }
    }

    private fun writeUInt32Le(data: ByteArray, offset: Int, value: Long) {
        SaveDataBinary.requireRange(data, offset, 4, "u32")
        repeat(4) { index ->
            data[offset + index] = ((value ushr (index * 8)) and 0xff).toByte()
        }
    }
}
