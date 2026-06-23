package com.atuy.yws1editor.yokai

import android.content.Context
import com.atuy.yws1editor.R

data class SaveDomainMasterData(
    val itemNames: Map<Long, String>,
    val equipmentNames: Map<Long, String>,
    val keyItemNames: Map<Long, String>,
    val sasuraiEncounterOptions: List<SasuraiEncounterOption>,
) {
    companion object {
        val EMPTY = SaveDomainMasterData(emptyMap(), emptyMap(), emptyMap(), emptyList())
    }
}

object SaveDomainMasterLoader {
    private val itemRegex = Regex("""<item\s+id="(\d+)"\s+name="([^"]+)"""")

    fun load(context: Context): SaveDomainMasterData {
        return SaveDomainMasterData(
            itemNames = loadNames(context, R.raw.consume_ja_jp) +
                loadNames(context, R.raw.creature_ja_jp),
            equipmentNames = loadNames(context, R.raw.equipment_ja_jp),
            keyItemNames = loadNames(context, R.raw.important_ja_jp),
            sasuraiEncounterOptions = loadSasuraiEncounters(context),
        )
    }

    private fun loadNames(context: Context, resourceId: Int): Map<Long, String> {
        val text = context.resources.openRawResource(resourceId)
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
        return itemRegex.findAll(text).mapNotNull { match ->
            val id = match.groupValues[1].toLongOrNull() ?: return@mapNotNull null
            id to match.groupValues[2]
        }.toMap()
    }

    private fun loadSasuraiEncounters(context: Context): List<SasuraiEncounterOption> {
        val lines = context.resources.openRawResource(R.raw.sasurai_encounter_ja_jp)
            .bufferedReader(Charsets.UTF_8)
            .use { it.readLines() }
        return lines.drop(1).mapNotNull { line ->
            val columns = line.split('\t')
            if (columns.size < 9) return@mapNotNull null
            val encounterId = parseHexUInt32(columns[0]) ?: return@mapNotNull null
            val representativeYokaiId = parseHexUInt32(columns[1]) ?: return@mapNotNull null
            val yokaiIds = listOf(columns[3], columns[5], columns[7]).map { value ->
                parseHexUInt32(value) ?: 0L
            }
            SasuraiEncounterOption(
                encounterId = encounterId,
                representativeYokaiId = representativeYokaiId,
                representativeName = columns[2],
                yokaiIds = yokaiIds,
                yokaiNames = listOf(columns[4], columns[6], columns[8]),
            )
        }
    }

    private fun parseHexUInt32(value: String): Long? {
        val normalized = value.removePrefix("0x").removePrefix("0X")
        return normalized.toLongOrNull(16)?.let { it and 0xFFFF_FFFFL }
    }
}
