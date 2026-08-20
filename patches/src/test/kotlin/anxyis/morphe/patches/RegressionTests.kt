package anxyis.morphe.patches

import anxyis.morphe.patches.alightmotion.amzNoPopupPatch
import anxyis.morphe.patches.alightmotion.alightMotionProNoPopupPatch
import anxyis.morphe.patches.alightmotion.daemon.popupDismisserPatch
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RegressionTests {

    @Test
    fun testSuiteDependencies() {
        assertTrue(
            alightMotionProNoPopupPatch.dependencies.contains(popupDismisserPatch),
            "AM Pro suite must include PopupDismisser"
        )
        assertTrue(
            amzNoPopupPatch.dependencies.contains(popupDismisserPatch),
            "AMZ suite must include PopupDismisser"
        )
    }
}
