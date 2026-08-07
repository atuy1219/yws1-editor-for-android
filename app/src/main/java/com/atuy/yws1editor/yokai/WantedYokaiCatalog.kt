package com.atuy.yws1editor.yokai

internal data class WantedYokaiDefinition(
    val parameterId: String,
    val id: Long,
    val name: String,
    val parentName: String,
    val detail: YokaiMasterDetail,
)

/**
 * Android版の指名手配妖怪。
 *
 * id は CHARA_PARAM_INFO field0 のParamID。ゲーム本体はこのIDで指名手配固有の
 * 基礎ステータスと成長パターンを参照するが、表示名とモデルは field1 のBaseIDから
 * 通常妖怪へ解決される。
 *
 * あきす老師・悪オトン・てぐせブーンは赤鬼・青鬼・黒鬼とParamIDを共有するため除外する。
 */
internal object WantedYokaiCatalog {
    val entries: List<WantedYokaiDefinition> = listOf(
        wanted("para_ah721", 0xA7FD35D8L, "ニセノコ", "ツチノコ", 29, 24, 22, 25, 24, 0, 0, 0, 0, 0, 8),
        wanted("para_y531_02", 0x08477FC1L, "キモ爺", "みちび鬼", 28, 30, 19, 24, 24, 2, 2, 3, 0, 2, 6),
        wanted("para_y321_02", 0xE37EB56CL, "黒ニャン", "トホホギス", 23, 3, 5, 32, 19, 0, 3, 0, 0, 0, 4),
        wanted("para_y843", 0x3042FB66L, "パクロ婆", "まむし行司", 36, 32, 22, 33, 29, 0, 2, 3, 2, 0, 8),
        wanted("para_pv041", 0x19AE1A45L, "通り馬", "しょうブシ", 102, 1, 1, 24, 29, 2, 2, 0, 3, 0, 1),
        wanted("para_y836", 0x0F69992EL, "うすっぺライオン", "ズキュキュン太", 31, 22, 39, 26, 36, 0, 3, 2, 3, 2, 4),
        wanted("para_y295b", 0xE460BDDEL, "がきおんな", "キンカク", 41, 29, 22, 36, 24, 0, 0, 3, 2, 0, 3),
        wanted("para_pv231", 0x556B58ECL, "いわくちゃん", "しゃれこ婦人", 72, 1, 1, 25, 24, 0, 2, 3, 2, 0, 3),
        wanted("para_y834", 0xE167F802L, "なまくら", "アニ鬼", 38, 41, 22, 32, 28, 1, 1, 0, 2, 2, 3),
        wanted("para_y541_02", 0xBA67A3D1L, "キザ天狗", "ヨコドリ", 60, 25, 28, 28, 23, 2, 2, 2, 2, 0, 7),
        wanted("para_y092_04", 0xE9ECFC08L, "デマまる", "なまはげ", 50, 50, 50, 50, 50, 2, 2, 2, 2, 2, 1),
        wanted("para_y832", 0x08045D37L, "くされモチ", "カブキ猿", 37, 35, 27, 27, 26, 2, 2, 2, 3, 0, 1),
        wanted("para_y413b", 0x9942143CL, "ワルスギス", "こめ爺", 34, 28, 37, 26, 29, 2, 2, 1, 0, 2, 5),
        wanted("para_y841", 0xDE4C9A4AL, "じゃまと", "デビビル", 26, 37, 29, 33, 32, 2, 2, 3, 3, 0, 6),
        wanted("para_y833", 0x7F036DA1L, "いかサマ士", "むりだ城", 41, 32, 24, 37, 25, 2, 0, 3, 2, 3, 3),
        wanted("para_y092_03", 0x778869ABL, "シッカク", "なまはげ", 50, 50, 50, 50, 50, 2, 2, 2, 2, 2, 1),
        wanted("para_y838", 0xE8D1B429L, "無茶むしゃ", "三途の犬", 35, 38, 28, 30, 33, 1, 1, 2, 0, 2, 7),
        wanted("para_c001", 0x8B97B90DL, "ブレルりん", "ケータ", 999, 999, 999, 999, 999, 0, 0, 0, 0, 0, 0),
        wanted("para_y791_02", 0x0F3FC66BL, "絶不蝶", "オロチ", 35, 32, 38, 25, 42, 0, 0, 2, 0, 2, 8),
        wanted("para_y835", 0x9660C894L, "とんま将軍", "ししコマ", 34, 24, 39, 23, 39, 2, 3, 2, 0, 2, 4),
        wanted("para_pv561", 0x2D53BA2CL, "どろボーイ", "グレるりん", 72, 1, 1, 25, 21, 2, 2, 3, 0, 0, 3),
        wanted("para_y021_02", 0x65EAC7C2L, "ボーどろ", "ちからモチ", 60, 60, 30, 26, 31, 2, 1, 0, 2, 2, 1),
        wanted("para_pv501", 0x7B091DAAL, "青いらん", "雷オトン", 120, 1, 1, 32, 34, 2, 2, 3, 3, 0, 5),
        wanted("para_y151_02", 0x1C96C877L, "悪メン犬", "バク", 60, 30, 35, 25, 30, 2, 2, 2, 0, 2, 4),
        wanted("para_y561_02", 0xC0A7F0B1L, "えせガッパ", "グレるりん", 31, 37, 20, 27, 24, 0, 2, 3, 0, 0, 3),
        wanted("para_y681_02", 0xF9033C7EL, "ダマさん", "ネクラマテング", 60, 30, 40, 25, 31, 2, 2, 2, 0, 2, 6),
        wanted("para_y611b", 0x017DBE35L, "腹黒郎", "フゥミン", 35, 26, 36, 27, 38, 2, 3, 2, 3, 2, 7),
        wanted("para_y760b", 0xA595FE94L, "ペテン老師", "はらわシェル", 33, 24, 28, 43, 26, 1, 0, 2, 1, 0, 8),
        wanted("para_ah271", 0xDC410376L, "荒らすん蛇", "ふじのやま", 27, 30, 22, 29, 20, 0, 0, 0, 0, 0, 3),
        wanted("para_y641_02", 0x3CF3D17FL, "じゃまガッパ", "ドンヨリーヌ", 90, 11, 22, 16, 30, 2, 0, 0, 0, 0, 7),
        wanted("para_y521_02", 0x35275671L, "だまししコマ", "ネガティブーン", 32, 26, 21, 22, 24, 0, 2, 3, 0, 2, 6),
        wanted("para_y691_02", 0xC46315CEL, "サギ王子", "ぎしんあん鬼", 27, 29, 21, 22, 24, 2, 2, 0, 0, 0, 6),
        wanted("para_y840", 0xA94BAADCL, "こそどろ帽", "さいの目入道", 35, 32, 24, 33, 30, 0, 2, 3, 2, 0, 5),
        wanted("para_ah341", 0xF6AE3A82L, "立ちヨミテング", "ノガッパ", 27, 22, 28, 23, 25, 0, 0, 0, 0, 0, 4),
        wanted("para_y092_01", 0x99860887L, "どろこ婦人", "なまはげ", 50, 50, 50, 50, 50, 2, 2, 2, 2, 2, 1),
        wanted("para_y837", 0x786EA9B8L, "クロノブシ", "おしっしょう", 37, 26, 28, 26, 35, 2, 3, 0, 3, 2, 7),
        wanted("para_y842", 0x4745CBF0L, "パチモ天", "トジコウモリ", 33, 32, 34, 23, 35, 2, 3, 0, 3, 2, 6),
        wanted("para_pv061", 0x2B9878C7L, "ペテン師匠", "メラメライオン", 90, 1, 1, 26, 29, 0, 2, 0, 0, 0, 1),
        wanted("para_y092_02", 0x008F593DL, "クロカブト", "なまはげ", 50, 50, 50, 50, 50, 2, 2, 2, 2, 2, 1),
        wanted("para_ah141", 0xF52AEEECL, "のぞキング", "バクロ婆", 27, 20, 31, 20, 25, 0, 0, 0, 0, 0, 2),
        wanted("para_y831", 0x910D0C8DL, "だましんぼう", "ゲンマ将軍", 42, 34, 28, 32, 21, 2, 2, 3, 2, 3, 1),
        wanted("para_y011_02", 0x224ABD12L, "ねつぞウナギ", "ぶようじん坊", 32, 28, 19, 15, 26, 2, 2, 3, 0, 0, 1),
        wanted("para_ah021", 0xA2B2235DL, "ほらふきザメ", "ちからモチ", 34, 29, 16, 30, 21, 0, 0, 0, 0, 0, 1),
        wanted("para_ah331", 0xB9EFAC45L, "イビビル", "ジバニャン", 28, 14, 20, 22, 28, 0, 0, 0, 0, 0, 4),
        wanted("para_y091b", 0x2A05B051L, "どろこんぶ", "くしゃ武者", 36, 38, 21, 31, 40, 0, 2, 3, 0, 0, 1),
        wanted("para_ah351", 0xEFB50BC3L, "わりゅーくん", "コマさん", 26, 23, 11, 21, 26, 0, 0, 0, 0, 0, 4),
        wanted("para_ah211", 0x8A1BA4F0L, "まきあげ貝", "だるだるま", 31, 24, 18, 25, 25, 0, 0, 0, 0, 0, 3),
        wanted("para_y839", 0x9FD684BFL, "もぐりちゃん", "ヒョウヘンヌ", 31, 29, 36, 29, 32, 0, 3, 2, 0, 2, 5),
        wanted("para_pv751", 0x05FA3D81L, "フゥビン", "ミチクサメ", 102, 1, 1, 28, 25, 2, 2, 3, 0, 3, 8),
    )

    val byId: Map<Long, WantedYokaiDefinition> = entries.associateBy { it.id }

    private fun wanted(
        parameterId: String,
        id: Long,
        name: String,
        parentName: String,
        hp: Int,
        power: Int,
        spirit: Int,
        defense: Int,
        speed: Int,
        growHp: Int,
        growPower: Int,
        growSpirit: Int,
        growDefense: Int,
        growSpeed: Int,
        yokaiClass: Int,
    ) = WantedYokaiDefinition(
        parameterId = parameterId,
        id = id,
        name = name,
        parentName = parentName,
        detail = YokaiMasterDetail(
            baseStats = Stat5(hp, power, spirit, defense, speed),
            growPattern = Stat5(growHp, growPower, growSpirit, growDefense, growSpeed),
            yokaiClass = yokaiClass,
        ),
    )
}
