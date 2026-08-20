package anxyis.morphe.patches.alightmotion.daemon

import anxyis.morphe.patches.alightmotion.ApplicationOnCreateFingerprint
import anxyis.morphe.patches.alightmotion.Constants
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

val popupDismisserPatch = bytecodePatch(
    name = "Popup Dismisser Daemon",
    description = "Injects background runtime dialog dismisser and preference seeder to eliminate all modder popups.",
    default = true
) {
    compatibleWith(Constants.COMPATIBILITY_AM_PRO, Constants.COMPATIBILITY_AMZ)

    extendWith("extensions/classes.dex")

    execute {
        ApplicationOnCreateFingerprint.methodOrNull?.addInstructions(
            0,
            """
                invoke-static {}, Lcom/alightcreative/app/motion/persist/PopupDismisser;->onStart()V
            """.trimIndent()
        )
    }
}
