package anxyis.morphe.patches.alightmotion

import anxyis.morphe.patches.alightmotion.daemon.popupDismisserPatch
import anxyis.morphe.patches.alightmotion.dialogs.moddedByDialogPatch
import anxyis.morphe.patches.alightmotion.dialogs.projectWizardPatch
import anxyis.morphe.patches.all.dialogs.updatesRequiredPatch
import anxyis.morphe.patches.all.prefs.seedPreferencesPatch
import app.morphe.patcher.patch.bytecodePatch

val amzNoPopupPatch = bytecodePatch(
    name = "After Motion Z+ Popup Suppression (Complete Suite)",
    description = "Eliminates all startup, update, and modded-by popups in After Motion Z+ (v5.0.273).",
    default = true
) {
    compatibleWith(Constants.COMPATIBILITY_AMZ)

    dependsOn(
        moddedByDialogPatch,
        projectWizardPatch,
        updatesRequiredPatch,
        seedPreferencesPatch,
        popupDismisserPatch
    )
}
