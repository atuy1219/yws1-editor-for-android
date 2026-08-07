package com.atuy.yws1editor.yokai

internal data class WantedYokaiDefinition(
    val parameterId: String,
    val id: Long,
    val name: String,
)

/**
 * Android版で通常妖怪とは別のキャラクターパラメータを持つ指名手配妖怪。
 *
 * 妖怪IDは内部パラメータIDのCRC32。ここにある妖怪は仲間妖怪の編集候補にだけ追加し、
 * 通常妖怪の図鑑番号には割り当てない。
 *
 * あきす老師・悪オトン・てぐせブーンはAndroid版でそれぞれ赤鬼・青鬼・黒鬼と
 * 同じ para_y991 / para_y992 / para_y993 を共有し、保存妖怪IDだけでは区別できないため除外する。
 */
internal object WantedYokaiCatalog {
    val entries: List<WantedYokaiDefinition> = listOf(
        WantedYokaiDefinition(parameterId = "para_ah721", id = 0xA7FD35D8L, name = "ニセノコ"),
        WantedYokaiDefinition(parameterId = "para_y531_02", id = 0x08477FC1L, name = "キモ爺"),
        WantedYokaiDefinition(parameterId = "para_y321_02", id = 0xE37EB56CL, name = "黒ニャン"),
        WantedYokaiDefinition(parameterId = "para_y843", id = 0x3042FB66L, name = "パクロ婆"),
        WantedYokaiDefinition(parameterId = "para_pv041", id = 0x19AE1A45L, name = "通り馬"),
        WantedYokaiDefinition(parameterId = "para_y836", id = 0x0F69992EL, name = "うすっぺライオン"),
        WantedYokaiDefinition(parameterId = "para_y295b", id = 0xE460BDDEL, name = "がきおんな"),
        WantedYokaiDefinition(parameterId = "para_pv231", id = 0x556B58ECL, name = "いわくちゃん"),
        WantedYokaiDefinition(parameterId = "para_y834", id = 0xE167F802L, name = "なまくら"),
        WantedYokaiDefinition(parameterId = "para_y541_02", id = 0xBA67A3D1L, name = "キザ天狗"),
        WantedYokaiDefinition(parameterId = "para_y092_04", id = 0xE9ECFC08L, name = "デマまる"),
        WantedYokaiDefinition(parameterId = "para_y832", id = 0x08045D37L, name = "くされモチ"),
        WantedYokaiDefinition(parameterId = "para_y413b", id = 0x9942143CL, name = "ワルスギス"),
        WantedYokaiDefinition(parameterId = "para_y841", id = 0xDE4C9A4AL, name = "じゃまと"),
        WantedYokaiDefinition(parameterId = "para_y833", id = 0x7F036DA1L, name = "いかサマ士"),
        WantedYokaiDefinition(parameterId = "para_y092_03", id = 0x778869ABL, name = "シッカク"),
        WantedYokaiDefinition(parameterId = "para_y838", id = 0xE8D1B429L, name = "無茶むしゃ"),
        WantedYokaiDefinition(parameterId = "para_c001", id = 0x8B97B90DL, name = "ブレルりん"),
        WantedYokaiDefinition(parameterId = "para_y791_02", id = 0x0F3FC66BL, name = "絶不蝶"),
        WantedYokaiDefinition(parameterId = "para_y835", id = 0x9660C894L, name = "とんま将軍"),
        WantedYokaiDefinition(parameterId = "para_pv561", id = 0x2D53BA2CL, name = "どろボーイ"),
        WantedYokaiDefinition(parameterId = "para_y021_02", id = 0x65EAC7C2L, name = "ボーどろ"),
        WantedYokaiDefinition(parameterId = "para_pv501", id = 0x7B091DAAL, name = "青いらん"),
        WantedYokaiDefinition(parameterId = "para_y151_02", id = 0x1C96C877L, name = "悪メン犬"),
        WantedYokaiDefinition(parameterId = "para_y561_02", id = 0xC0A7F0B1L, name = "えせガッパ"),
        WantedYokaiDefinition(parameterId = "para_y681_02", id = 0xF9033C7EL, name = "ダマさん"),
        WantedYokaiDefinition(parameterId = "para_y611b", id = 0x017DBE35L, name = "腹黒郎"),
        WantedYokaiDefinition(parameterId = "para_y760b", id = 0xA595FE94L, name = "ペテン老師"),
        WantedYokaiDefinition(parameterId = "para_ah271", id = 0xDC410376L, name = "荒らすん蛇"),
        WantedYokaiDefinition(parameterId = "para_y641_02", id = 0x3CF3D17FL, name = "じゃまガッパ"),
        WantedYokaiDefinition(parameterId = "para_y521_02", id = 0x35275671L, name = "だまししコマ"),
        WantedYokaiDefinition(parameterId = "para_y691_02", id = 0xC46315CEL, name = "サギ王子"),
        WantedYokaiDefinition(parameterId = "para_y840", id = 0xA94BAADCL, name = "こそどろ帽"),
        WantedYokaiDefinition(parameterId = "para_ah341", id = 0xF6AE3A82L, name = "立ちヨミテング"),
        WantedYokaiDefinition(parameterId = "para_y092_01", id = 0x99860887L, name = "どろこ婦人"),
        WantedYokaiDefinition(parameterId = "para_y837", id = 0x786EA9B8L, name = "クロノブシ"),
        WantedYokaiDefinition(parameterId = "para_y842", id = 0x4745CBF0L, name = "パチモ天"),
        WantedYokaiDefinition(parameterId = "para_pv061", id = 0x2B9878C7L, name = "ペテン師匠"),
        WantedYokaiDefinition(parameterId = "para_y092_02", id = 0x008F593DL, name = "クロカブト"),
        WantedYokaiDefinition(parameterId = "para_ah141", id = 0xF52AEEECL, name = "のぞキング"),
        WantedYokaiDefinition(parameterId = "para_y831", id = 0x910D0C8DL, name = "だましんぼう"),
        WantedYokaiDefinition(parameterId = "para_y011_02", id = 0x224ABD12L, name = "ねつぞウナギ"),
        WantedYokaiDefinition(parameterId = "para_ah021", id = 0xA2B2235DL, name = "ほらふきザメ"),
        WantedYokaiDefinition(parameterId = "para_ah331", id = 0xB9EFAC45L, name = "イビビル"),
        WantedYokaiDefinition(parameterId = "para_y091b", id = 0x2A05B051L, name = "どろこんぶ"),
        WantedYokaiDefinition(parameterId = "para_ah351", id = 0xEFB50BC3L, name = "わりゅーくん"),
        WantedYokaiDefinition(parameterId = "para_ah211", id = 0x8A1BA4F0L, name = "まきあげ貝"),
        WantedYokaiDefinition(parameterId = "para_y839", id = 0x9FD684BFL, name = "もぐりちゃん"),
        WantedYokaiDefinition(parameterId = "para_pv751", id = 0x05FA3D81L, name = "フゥビン"),
    )
}
