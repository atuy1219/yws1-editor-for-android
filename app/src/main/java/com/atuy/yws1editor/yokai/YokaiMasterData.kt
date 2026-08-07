package com.atuy.yws1editor.yokai

import android.content.Context
import com.atuy.yws1editor.R

data class YokaiMasterDetail(
    val baseStats: Stat5,
    val growPattern: Stat5,
    val yokaiClass: Int,
)

data class YokaiAttitude(
    val id: Int,
    val name: String,
)

data class YokaiMasterData(
    val nameById: Map<Long, String>,
    val numberById: Map<Long, Int>,
    val detailById: Map<Long, YokaiMasterDetail>,
    val attitudes: List<YokaiAttitude>,
) {
    companion object {
        val EMPTY = YokaiMasterData(
            nameById = emptyMap(),
            numberById = emptyMap(),
            detailById = emptyMap(),
            attitudes = emptyList(),
        )
    }
}

internal object YokaiMasterIdCorrection {
    private const val LEGACY_KOME_JII_ID = 0x9942143CL
    const val KOME_JII_ID = 0x442FF647L

    fun normalize(id: Long, name: String): Long {
        return if (id == LEGACY_KOME_JII_ID && name == "こめ爺") KOME_JII_ID else id
    }
}

object YokaiMasterLoader {

    private val xmlItemRegex = Regex("""<item\s+id="(\d+)"\s+name="([^"]+)"""")
    private val jsEntryRegex = Regex(
        pattern = """\{"num":\s*(\d+),.*?"bs":\s*\[(\d+),\s*(\d+),\s*(\d+),\s*(\d+),\s*(\d+)\],\s*"growPat":\s*\[(\d+),\s*(\d+),\s*(\d+),\s*(\d+),\s*(\d+)\].*?"class":\s*(\d+)""",
        options = setOf(RegexOption.DOT_MATCHES_ALL),
    )
    private val fixedAttitudes = listOf(
        YokaiAttitude(1, "短気"),
        YokaiAttitude(2, "れいせい"),
        YokaiAttitude(3, "しんちょう"),
        YokaiAttitude(4, "やさしい"),
        YokaiAttitude(5, "いやらしい"),
        YokaiAttitude(6, "協力的"),
        YokaiAttitude(7, "荒くれ"),
        YokaiAttitude(8, "ずのう的"),
        YokaiAttitude(9, "動じない"),
        YokaiAttitude(10, "情け深い"),
        YokaiAttitude(11, "非道"),
        YokaiAttitude(12, "けんしん的"),
    )

    fun load(context: Context): YokaiMasterData {
        return runCatching {
            val xmlText = context.resources.openRawResource(R.raw.youkai_ja_jp)
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }
            val jsText = context.resources.openRawResource(R.raw.calc)
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }

            val ordered = mutableListOf<Pair<Long, String>>()
            xmlItemRegex.findAll(xmlText).forEach { match ->
                val id = match.groupValues[1].toLongOrNull() ?: return@forEach
                val name = match.groupValues[2]
                ordered.add(id to name)
            }

            val normalizedOrdered = ordered.map { (id, name) ->
                YokaiMasterIdCorrection.normalize(id, name) to name
            }
            val standardNameById = normalizedOrdered.toMap()
            val nameById = buildMap {
                putAll(standardNameById)
                WantedYokaiCatalog.entries.forEach { wanted ->
                    putIfAbsent(wanted.id, wanted.name)
                }
            }
            val numberById = normalizedOrdered.mapIndexed { index, pair ->
                pair.first to (index + 1)
            }.toMap()

            val detailByNumber = mutableMapOf<Int, YokaiMasterDetail>()
            jsEntryRegex.findAll(jsText).forEach { match ->
                val num = match.groupValues[1].toIntOrNull() ?: return@forEach
                val hp = match.groupValues[2].toIntOrNull() ?: return@forEach
                val power = match.groupValues[3].toIntOrNull() ?: return@forEach
                val spirit = match.groupValues[4].toIntOrNull() ?: return@forEach
                val defense = match.groupValues[5].toIntOrNull() ?: return@forEach
                val speed = match.groupValues[6].toIntOrNull() ?: return@forEach
                val growHp = match.groupValues[7].toIntOrNull() ?: return@forEach
                val growPower = match.groupValues[8].toIntOrNull() ?: return@forEach
                val growSpirit = match.groupValues[9].toIntOrNull() ?: return@forEach
                val growDefense = match.groupValues[10].toIntOrNull() ?: return@forEach
                val growSpeed = match.groupValues[11].toIntOrNull() ?: return@forEach
                val yokaiClass = match.groupValues[12].toIntOrNull() ?: return@forEach

                detailByNumber[num] = YokaiMasterDetail(
                    baseStats = Stat5(
                        hp = hp,
                        power = power,
                        spirit = spirit,
                        defense = defense,
                        speed = speed,
                    ),
                    growPattern = Stat5(
                        hp = growHp,
                        power = growPower,
                        spirit = growSpirit,
                        defense = growDefense,
                        speed = growSpeed,
                    ),
                    yokaiClass = yokaiClass,
                )
            }

            val standardDetailById = numberById.mapNotNull { (id, num) ->
                detailByNumber[num]?.let { id to it }
            }.toMap()
            val detailById = buildMap {
                putAll(standardDetailById)
                WantedYokaiCatalog.entries.forEach { wanted ->
                    put(wanted.id, wanted.detail)
                }
            }

            YokaiMasterData(
                nameById = nameById,
                numberById = numberById,
                detailById = detailById,
                attitudes = fixedAttitudes,
            )
        }.getOrElse {
            YokaiMasterData.EMPTY
        }
    }
}
