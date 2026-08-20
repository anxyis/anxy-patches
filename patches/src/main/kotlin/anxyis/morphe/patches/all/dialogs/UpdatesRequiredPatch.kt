package anxyis.morphe.patches.all.dialogs

import anxyis.morphe.patches.alightmotion.Constants
import anxyis.morphe.patches.alightmotion.FqAbFingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch

val updatesRequiredPatch = bytecodePatch(
    name = "Updates Required Popup Suppression",
    description = "No-ops fq.ab dialog builder in Firebase Analytics wrapper.",
    default = true
) {
    compatibleWith(Constants.COMPATIBILITY_AMZ, Constants.COMPATIBILITY_AM_PRO)

    execute {
        val method = FqAbFingerprint.methodOrNull
            ?: throw PatchException("Fq.ab fingerprint not found")

        method.addInstruction(0, "return-void")
    }
}
