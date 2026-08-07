package com.atuy.yws1editor.yokai

import java.util.zip.CRC32
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class WantedYokaiCatalogTest {

    @Test
    fun containsRepresentativeWantedYokai() {
        val entriesByName = WantedYokaiCatalog.entries.associateBy { it.name }

        assertEquals(0x3042FB66L, entriesByName.getValue("パクロ婆").id)
        assertEquals(0xE37EB56CL, entriesByName.getValue("黒ニャン").id)
        assertEquals(0x9942143CL, entriesByName.getValue("ワルスギス").id)
    }

    @Test
    fun pentenShishoUsesItsOwnCharaParamStats() {
        val penten = WantedYokaiCatalog.entries.single { it.name == "ペテン師匠" }

        assertEquals("メラメライオン", penten.parentName)
        assertEquals(Stat5(90, 1, 1, 26, 29), penten.detail.baseStats)
        assertEquals(Stat5(0, 2, 0, 0, 0), penten.detail.growPattern)
        assertEquals(1, penten.detail.yokaiClass)
    }

    @Test
    fun idsMatchParameterCrc32AndAreUnique() {
        val entries = WantedYokaiCatalog.entries

        assertEquals(49, entries.size)
        assertEquals(entries.size, entries.map { it.id }.toSet().size)
        entries.forEach { entry ->
            assertEquals(
                "${entry.name} (${entry.parameterId})",
                crc32(entry.parameterId),
                entry.id,
            )
        }
    }

    @Test
    fun aliasesThatShareOniIdsAreNotExposed() {
        val names = WantedYokaiCatalog.entries.map { it.name }.toSet()

        assertFalse("あきす老師" in names)
        assertFalse("悪オトン" in names)
        assertFalse("てぐせブーン" in names)
    }

    private fun crc32(value: String): Long {
        return CRC32().apply {
            update(value.toByteArray(Charsets.UTF_8))
        }.value
    }
}
