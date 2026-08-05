from pathlib import Path


def replace_once(path: str, old: str, new: str) -> None:
    file = Path(path)
    text = file.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{path}: expected one match, found {count}: {old[:80]!r}")
    file.write_text(text.replace(old, new, 1), encoding="utf-8")


parser = "app/src/main/java/com/atuy/yws1editor/yokai/YokaiParser.kt"
replace_once(
    parser,
    """            val iva = Stat5(
                hp = game0Data[base + 0x60].toInt() and 0xFF,
                power = game0Data[base + 0x61].toInt() and 0xFF,
                spirit = game0Data[base + 0x62].toInt() and 0xFF,
                defense = game0Data[base + 0x63].toInt() and 0xFF,
                speed = game0Data[base + 0x64].toInt() and 0xFF,
            )""",
    """            // CalcParamBase reads IVA with LDRSB, so Byte.toInt() must sign-extend it.
            val iva = Stat5(
                hp = game0Data[base + 0x60].toInt(),
                power = game0Data[base + 0x61].toInt(),
                spirit = game0Data[base + 0x62].toInt(),
                defense = game0Data[base + 0x63].toInt(),
                speed = game0Data[base + 0x64].toInt(),
            )""",
)
replace_once(
    parser,
    """            val ivb = intArrayOf(
                game0Data[base + 0x65].toInt() and 0xFF,
                game0Data[base + 0x66].toInt() and 0xFF,
                game0Data[base + 0x67].toInt() and 0xFF,
                game0Data[base + 0x68].toInt() and 0xFF,
                game0Data[base + 0x69].toInt() and 0xFF,
            )""",
    """            // GetPrmRandom/GetPrmEvolve and CalcParamBase split these bytes into
            // unsigned low/high nibbles. Both IVB groups therefore remain in 0..15.
            val ivb = intArrayOf(
                game0Data[base + 0x65].toInt() and 0xFF,
                game0Data[base + 0x66].toInt() and 0xFF,
                game0Data[base + 0x67].toInt() and 0xFF,
                game0Data[base + 0x68].toInt() and 0xFF,
                game0Data[base + 0x69].toInt() and 0xFF,
            )""",
)
replace_once(
    parser,
    """            val cb = Stat5(
                hp = game0Data[base + 0x6A].toInt() and 0xFF,
                power = game0Data[base + 0x6B].toInt() and 0xFF,
                spirit = game0Data[base + 0x6C].toInt() and 0xFF,
                defense = game0Data[base + 0x6D].toInt() and 0xFF,
                speed = game0Data[base + 0x6E].toInt() and 0xFF,
            )""",
    """            // CalcParamBase reads CB with LDRSB as well.
            val cb = Stat5(
                hp = game0Data[base + 0x6A].toInt(),
                power = game0Data[base + 0x6B].toInt(),
                spirit = game0Data[base + 0x6C].toInt(),
                defense = game0Data[base + 0x6D].toInt(),
                speed = game0Data[base + 0x6E].toInt(),
            )""",
)
replace_once(
    parser,
    """            writeStat5(out, base + 0x60, entry.iva, max = 255)
            writePackedNibbleStat5(out, base + 0x65, entry.ivb1, entry.ivb2)
            writeStat5(out, base + 0x6A, entry.cb, max = 255)""",
    """            writeSignedByteStat5(out, base + 0x60, entry.iva)
            writePackedNibbleStat5(out, base + 0x65, entry.ivb1, entry.ivb2)
            writeSignedByteStat5(out, base + 0x6A, entry.cb)""",
)
replace_once(
    parser,
    """    private fun writeStat5(data: ByteArray, offset: Int, stat: Stat5, max: Int) {
        val values = stat.values()
        for (i in values.indices) {
            data[offset + i] = clamp(values[i], 0, max).toByte()
        }
    }""",
    """    private fun writeSignedByteStat5(data: ByteArray, offset: Int, stat: Stat5) {
        val values = stat.values()
        for (i in values.indices) {
            data[offset + i] = clamp(
                values[i],
                Byte.MIN_VALUE.toInt(),
                Byte.MAX_VALUE.toInt(),
            ).toByte()
        }
    }""",
)

view_model = "app/src/main/java/com/atuy/yws1editor/MainViewModel.kt"
replace_once(
    view_model,
    """        private const val CHEAT_STAT_MAX = 255
        private const val NORMAL_LEVEL_MAX = 99""",
    """        private const val CHEAT_LEVEL_MAX = 255
        private const val SIGNED_STAT_MIN = -128
        private const val SIGNED_STAT_MAX = 127
        private const val NORMAL_LEVEL_MAX = 99""",
)
replace_once(
    view_model,
    "val max = if (_uiState.value.isCheatMode) CHEAT_STAT_MAX else NORMAL_LEVEL_MAX",
    "val max = if (_uiState.value.isCheatMode) CHEAT_LEVEL_MAX else NORMAL_LEVEL_MAX",
)
replace_once(
    view_model,
    """                        val updated = applyStatUpdate(
                            stat = entry.iva,
                            index = index,
                            requested = value,
                            cellMax = CHEAT_STAT_MAX,
                            totalMax = null,
                        )""",
    """                        val updated = applyStatUpdate(
                            stat = entry.iva,
                            index = index,
                            requested = value,
                            cellMin = SIGNED_STAT_MIN,
                            cellMax = SIGNED_STAT_MAX,
                            totalMax = null,
                        )""",
)
replace_once(
    view_model,
    """                    val updated = applyStatUpdate(
                        stat = masked,
                        index = index,
                        requested = value,
                        cellMax = CHEAT_STAT_MAX,
                        totalMax = NORMAL_IVA_TOTAL_MAX,
                    )""",
    """                    val updated = applyStatUpdate(
                        stat = masked,
                        index = index,
                        requested = value,
                        cellMax = SIGNED_STAT_MAX,
                        totalMax = NORMAL_IVA_TOTAL_MAX,
                    )""",
)
replace_once(
    view_model,
    """                StatGroup.CB -> {
                    val updated = applyStatUpdate(
                        stat = entry.cb,
                        index = index,
                        requested = value,
                        cellMax = CHEAT_STAT_MAX,
                        totalMax = if (isCheatMode) null else NORMAL_CB_TOTAL_MAX,
                    )
                    entry.copy(cb = updated)
                }""",
    """                StatGroup.CB -> {
                    val updated = applyStatUpdate(
                        stat = entry.cb,
                        index = index,
                        requested = value,
                        cellMin = if (isCheatMode) SIGNED_STAT_MIN else 0,
                        cellMax = SIGNED_STAT_MAX,
                        totalMax = if (isCheatMode) null else NORMAL_CB_TOTAL_MAX,
                    )
                    entry.copy(cb = updated)
                }""",
)
replace_once(
    view_model,
    """        requested: Int,
        cellMax: Int,
        totalMax: Int?,
    ): Stat5 {
        if (index !in 0..4) return stat
        val clampedRequested = clampToRange(requested, cellMax)""",
    """        requested: Int,
        cellMin: Int = 0,
        cellMax: Int,
        totalMax: Int?,
    ): Stat5 {
        if (index !in 0..4) return stat
        val clampedRequested = requested.coerceIn(cellMin, cellMax)""",
)
replace_once(
    view_model,
    """            CHEAT_STAT_MAX,
            NORMAL_IVA_TOTAL_MAX,""",
    """            SIGNED_STAT_MAX,
            NORMAL_IVA_TOTAL_MAX,""",
)
replace_once(
    view_model,
    "val normalizedCb = normalizeStatForNormal(entry.cb, CHEAT_STAT_MAX, NORMAL_CB_TOTAL_MAX)",
    "val normalizedCb = normalizeStatForNormal(entry.cb, SIGNED_STAT_MAX, NORMAL_CB_TOTAL_MAX)",
)
if "CHEAT_STAT_MAX" in Path(view_model).read_text(encoding="utf-8"):
    raise SystemExit("MainViewModel.kt: stale CHEAT_STAT_MAX reference")

activity = "app/src/main/java/com/atuy/yws1editor/MainActivity.kt"
replace_once(
    activity,
    """                    ivaInputMax = if (isCheatMode) 255 else 8,
                    cbInputMax = if (isCheatMode) 255 else 20,""",
    """                    ivaInputMin = if (isCheatMode) -128 else 0,
                    ivaInputMax = if (isCheatMode) 127 else 8,
                    cbInputMin = if (isCheatMode) -128 else 0,
                    cbInputMax = if (isCheatMode) 127 else 20,""",
)
replace_once(
    activity,
    """    levelInputMax: Int,
    ivaInputMax: Int,
    cbInputMax: Int,
    onCardClick: (Int) -> Unit,""",
    """    levelInputMax: Int,
    ivaInputMin: Int,
    ivaInputMax: Int,
    cbInputMin: Int,
    cbInputMax: Int,
    onCardClick: (Int) -> Unit,""",
)
replace_once(
    activity,
    '            Text("チートモード (LV/IVA/CBは最大255、IVB1/IVB2は15固定)")',
    '            Text("チートモード (LVは最大255、IVA/CBは-128〜127、IVB1/IVB2は0〜15)")',
)
replace_once(
    activity,
    """                                levelInputMax = levelInputMax,
                                ivaInputMax = ivaInputMax,
                                cbInputMax = cbInputMax,""",
    """                                levelInputMax = levelInputMax,
                                ivaInputMin = ivaInputMin,
                                ivaInputMax = ivaInputMax,
                                cbInputMin = cbInputMin,
                                cbInputMax = cbInputMax,""",
)
replace_once(
    activity,
    """    levelInputMax: Int,
    ivaInputMax: Int,
    cbInputMax: Int,
    onYokaiChange: (Long) -> Unit,""",
    """    levelInputMax: Int,
    ivaInputMin: Int,
    ivaInputMax: Int,
    cbInputMin: Int,
    cbInputMax: Int,
    onYokaiChange: (Long) -> Unit,""",
)
replace_once(
    activity,
    """            stat = entry.iva,
            max = ivaInputMax,
            editableMask = ivaEditableMask,""",
    """            stat = entry.iva,
            max = ivaInputMax,
            min = ivaInputMin,
            editableMask = ivaEditableMask,""",
)
replace_once(
    activity,
    '        StatusEditableRow(label = "CB", stat = entry.cb, max = cbInputMax, onValueChange = { i, v -> onStatChange(StatGroup.CB, i, v) })',
    '        StatusEditableRow(label = "CB", stat = entry.cb, max = cbInputMax, min = cbInputMin, onValueChange = { i, v -> onStatChange(StatGroup.CB, i, v) })',
)
replace_once(
    activity,
    """    stat: Stat5,
    max: Int,
    editableMask: List<Boolean>? = null,""",
    """    stat: Stat5,
    max: Int,
    min: Int = 0,
    editableMask: List<Boolean>? = null,""",
)
replace_once(
    activity,
    """                    value = value,
                    max = max,
                    modifier = Modifier""",
    """                    value = value,
                    max = max,
                    min = min,
                    modifier = Modifier""",
)
replace_once(
    activity,
    """private fun CompactNumberField(
    value: Int,
    max: Int,
    modifier: Modifier = Modifier,""",
    """private fun CompactNumberField(
    value: Int,
    max: Int,
    min: Int = 0,
    modifier: Modifier = Modifier,""",
)
replace_once(
    activity,
    """            val clamped = when {
                parsed < 0 -> 0
                parsed > max -> max
                else -> parsed
            }""",
    """            val clamped = parsed.coerceIn(min, max)""",
)
replace_once(
    activity,
    """                val fixed = when {
                    parsed == null -> 0
                    parsed < 0 -> 0
                    parsed > max -> max
                    else -> parsed
                }""",
    """                val fixed = parsed?.coerceIn(min, max) ?: 0.coerceIn(min, max)""",
)
replace_once(
    activity,
    "keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),\n        singleLine = true,\n    )\n}",
    """keyboardOptions = KeyboardOptions(
            keyboardType = if (min < 0) KeyboardType.Text else KeyboardType.Number,
        ),
        singleLine = true,
    )
}""",
)

test = Path("app/src/test/java/com/atuy/yws1editor/yokai/YokaiParserTest.kt")
test.parent.mkdir(parents=True, exist_ok=True)
test.write_text(
    """package com.atuy.yws1editor.yokai

import org.junit.Assert.assertEquals
import org.junit.Test

class YokaiParserTest {
    private val parser = YokaiParser()
    private val yokaiStart = 0x1D40
    private val recordSize = 0x7C

    @Test
    fun parseTreatsIvaAndCbAsSignedBytesAndIvbAsUnsignedNibbles() {
        val data = newRecord()
        writeBytes(data, yokaiStart + 0x60, listOf(-128, -1, 0, 1, 127))
        writeBytes(data, yokaiStart + 0x65, listOf(0xF0, 0xE1, 0xD2, 0xC3, 0xB4))
        writeBytes(data, yokaiStart + 0x6A, listOf(127, 1, 0, -1, -128))

        val entry = parser.parse(data).single()

        assertEquals(Stat5(-128, -1, 0, 1, 127), entry.iva)
        assertEquals(Stat5(0, 1, 2, 3, 4), entry.ivb1)
        assertEquals(Stat5(15, 14, 13, 12, 11), entry.ivb2)
        assertEquals(Stat5(127, 1, 0, -1, -128), entry.cb)
    }

    @Test
    fun applyEntriesClampsSignedBytesAndPacksUnsignedNibbles() {
        val data = newRecord()
        val original = parser.parse(data).single()
        val edited = original.copy(
            iva = Stat5(-129, -128, -1, 127, 128),
            ivb1 = Stat5(-1, 0, 1, 15, 16),
            ivb2 = Stat5(16, 15, 14, 0, -1),
            cb = Stat5(128, 127, 1, -128, -129),
        )

        val reparsed = parser.parse(parser.applyEntries(data, listOf(edited))).single()

        assertEquals(Stat5(-128, -128, -1, 127, 127), reparsed.iva)
        assertEquals(Stat5(0, 0, 1, 15, 15), reparsed.ivb1)
        assertEquals(Stat5(15, 15, 14, 0, 0), reparsed.ivb2)
        assertEquals(Stat5(127, 127, 1, -128, -128), reparsed.cb)
    }

    private fun newRecord(): ByteArray {
        val data = ByteArray(yokaiStart + recordSize)
        writeIntLe(data, yokaiStart, 1)
        writeIntLe(data, yokaiStart + 0x04, 1)
        return data
    }

    private fun writeBytes(data: ByteArray, offset: Int, values: List<Int>) {
        values.forEachIndexed { index, value -> data[offset + index] = value.toByte() }
    }

    private fun writeIntLe(data: ByteArray, offset: Int, value: Int) {
        data[offset] = value.toByte()
        data[offset + 1] = (value ushr 8).toByte()
        data[offset + 2] = (value ushr 16).toByte()
        data[offset + 3] = (value ushr 24).toByte()
    }
}
""",
    encoding="utf-8",
)

Path(".github/workflows/apply-signed-stats.yml").unlink()
Path(".github/scripts/apply_signed_stats.py").unlink()
