package anxyis.morphe.patches

import anxyis.morphe.patches.alightmotion.alightMotionProNoPopupPatch
import anxyis.morphe.patches.alightmotion.dialogs.satriyaidModdedDialogDestructionPatch
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RegressionTests {

    @Test
    fun testDestructionInSuite() {
        assertTrue(
            alightMotionProNoPopupPatch.dependencies.contains(satriyaidModdedDialogDestructionPatch),
            "AM Pro suite must include satriyaidModdedDialogDestructionPatch"
        )
    }
}
