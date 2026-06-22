package com.atuy.yws1editor.yokai

import android.content.Context
import com.atuy.yws1editor.R

data class SaveDomainMasterData(
    val itemNames: Map<Long, String>,
    val equipmentNames: Map<Long, String>,
    val keyItemNames: Map<Long, String>,
) {
    companion object {
        val EMPTY = SaveDomainMasterData(emptyMap(), emptyMap(), emptyMap())
    }
}

object SaveDomainMasterLoader {
    private val itemRegex = Regex("""<item\s+id="(\d+)"\s+name="([^"]+)"""")

    fun load(context: Context): SaveDomainMasterData {
        return SaveDomainMasterData(
            itemNames = loadNames(context, R.raw.consume_ja_jp),
            equipmentNames = loadNames(context, R.raw.equipment_ja_jp),
            // The repository does not currently include an authoritative smartphone key-item table.
            keyItemNames = emptyMap(),
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
}
