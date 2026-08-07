package com.atuy.yws1editor.yokai

import org.junit.Assert.assertEquals
import org.junit.Test

class YokaiMasterIdCorrectionTest {

    @Test
    fun correctsLegacyKomeJiiIdThatCollidesWithWarusugisu() {
        assertEquals(
            YokaiMasterIdCorrection.KOME_JII_ID,
            YokaiMasterIdCorrection.normalize(0x9942143CL, "こめ爺"),
        )
    }

    @Test
    fun leavesOtherYokaiIdsUnchanged() {
        assertEquals(
            0x9942143CL,
            YokaiMasterIdCorrection.normalize(0x9942143CL, "ワルスギス"),
        )
    }
}
