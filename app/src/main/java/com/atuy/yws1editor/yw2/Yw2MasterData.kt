package com.atuy.yws1editor.yw2

import android.content.Context
import android.util.Xml
import com.atuy.yws1editor.R
import org.xmlpull.v1.XmlPullParser

data class Yw2NamedId(
    val id: Long,
    val name: String,
)

class Yw2MasterData(context: Context) {
    val yokai: List<Yw2NamedId> = load(context, R.raw.yw2_youkai_ja)
    val items: List<Yw2NamedId> =
        (load(context, R.raw.yw2_creature_ja) + load(context, R.raw.yw2_consume_ja))
            .distinctBy { it.id }
            .sortedBy { it.name }
    val equipment: List<Yw2NamedId> = load(context, R.raw.yw2_equipment_ja).sortedBy { it.name }
    val important: List<Yw2NamedId> = load(context, R.raw.yw2_important_ja).sortedBy { it.name }
    val souls: List<Yw2NamedId> = load(context, R.raw.yw2_soul_ja).sortedBy { it.name }
    val attitudes: List<Yw2NamedId> = load(context, R.raw.yw2_ai_ja)

    private val yokaiById = yokai.associateBy { it.id }
    private val itemById = items.associateBy { it.id }
    private val equipmentById = equipment.associateBy { it.id }
    private val importantById = important.associateBy { it.id }
    private val soulById = souls.associateBy { it.id }

    fun yokaiName(id: Long): String = yokaiById[id]?.name ?: "ID:$id"

    private val twoEquipmentSlotNames = setOf(
        "寝ブタ",
        "万尾獅子",
        "ちからモチ",
        "やきモチ",
        "さきがけの助",
        "ばか頭巾",
        "かぜカモ",
        "ズルズルづる",
        "のっぺら坊",
        "アペリカン",
        "ドキ土器",
        "あせっか鬼",
        "びきゃく",
        "一つ目小僧",
        "みちび鬼",
        "ガ鬼",
        "ぎしんあん鬼",
        "ジコチュウ",
        "こおりんぼう",
        "トホホギス",
        "ホリュウ",
        "ツチノコパンダ",
    )

    fun equipmentSlotCount(id: Long): Int {
        val baseName = yokaiById[id]?.name?.substringBefore(" (") ?: return 1
        return if (baseName in twoEquipmentSlotNames) 2 else 1
    }

    fun inventoryName(kind: Yw2InventoryKind, id: Long): String = when (kind) {
        Yw2InventoryKind.ITEM -> itemById[id]
        Yw2InventoryKind.EQUIPMENT -> equipmentById[id]
        Yw2InventoryKind.IMPORTANT -> importantById[id]
        Yw2InventoryKind.SOUL -> soulById[id]
    }?.name ?: "ID:$id"

    fun entries(kind: Yw2InventoryKind): List<Yw2NamedId> = when (kind) {
        Yw2InventoryKind.ITEM -> items
        Yw2InventoryKind.EQUIPMENT -> equipment
        Yw2InventoryKind.IMPORTANT -> important
        Yw2InventoryKind.SOUL -> souls
    }

    companion object {
        private fun load(context: Context, rawId: Int): List<Yw2NamedId> {
            context.resources.openRawResource(rawId).use { input ->
                val parser = Xml.newPullParser().apply {
                    setInput(input, "UTF-8")
                }
                val out = ArrayList<Yw2NamedId>()
                while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                    if (parser.eventType == XmlPullParser.START_TAG && parser.name == "item") {
                        val id = parser.getAttributeValue(null, "id")?.toLongOrNull()
                        val name = parser.getAttributeValue(null, "name")
                        if (id != null && !name.isNullOrBlank()) out += Yw2NamedId(id, name)
                    }
                    parser.next()
                }
                return out
            }
        }
    }
}
