package anxyis.morphe.patches

import anxyis.morphe.patches.alightmotion.amzNoPopupPatch
import anxyis.morphe.patches.alightmotion.dialogs.moddedByDialogPatch
import anxyis.morphe.patches.alightmotion.dialogs.nativeServerGatePatch
import anxyis.morphe.patches.alightmotion.dialogs.projectWizardPatch
import anxyis.morphe.patches.all.dialogs.updatesRequiredPatch
import anxyis.morphe.patches.all.prefs.seedPreferencesPatch
import anxyis.morphe.patches.shared.HexPatchBuilder
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class RegressionTests {

    @Test
    fun `verify master composite patch dependencies`() {
        val deps = amzNoPopupPatch.dependencies
        assertEquals(5, deps.size, "AMZ Master composite patch must include all 5 mechanisms")
        assertTrue(deps.contains(updatesRequiredPatch), "Must contain M1 updatesRequiredPatch")
        assertTrue(deps.contains(nativeServerGatePatch), "Must contain M2 nativeServerGatePatch")
        assertTrue(deps.contains(seedPreferencesPatch), "Must contain M3 seedPreferencesPatch")
        assertTrue(deps.contains(projectWizardPatch), "Must contain M7 projectWizardPatch")
        assertTrue(deps.contains(moddedByDialogPatch), "Must contain M8 moddedByDialogPatch")
    }

    @Test
    fun `verify HexPatchBuilder pattern matching`() {
        val testData = byteArrayOf(0x00, 0x11, 0x22, 0x33, 0x44, 0x55)
        val pattern = byteArrayOf(0x22, 0x33, 0x44)
        val match = HexPatchBuilder.findPattern(testData, pattern)
        assertEquals(2, match, "Hex pattern must match at offset 2")
    }
}
