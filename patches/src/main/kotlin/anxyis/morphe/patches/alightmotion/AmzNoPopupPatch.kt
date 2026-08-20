package anxyis.morphe.patches.alightmotion

import anxyis.morphe.patches.alightmotion.daemon.popupDismisserPatch
import anxyis.morphe.patches.alightmotion.dialogs.moddedByDialogPatch
import anxyis.morphe.patches.alightmotion.dialogs.nativeServerGatePatch
import anxyis.morphe.patches.alightmotion.dialogs.projectWizardPatch
import anxyis.morphe.patches.all.dialogs.updatesRequiredPatch
import anxyis.morphe.patches.all.prefs.seedPreferencesPatch
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.bytecodePatch

val amzNoPopupPatch = bytecodePatch(
    name = "AMZ Popup Suppression (Complete Suite)",
    description = "Comprehensively eliminates all startup, update, wizard, and modded-by popups in After Motion Z+ (Satriyaid mod of Alight Motion).",
    default = true
) {
    compatibleWith(
        Compatibility(
            name = "After Motion Z+ (Satriyaid)",
            packageName = Constants.PACKAGE_NAME,
            targets = listOf(AppTarget(Constants.TARGET_VERSION_AMZ), AppTarget(Constants.TARGET_VERSION_SHORT))
        )
    )

    dependsOn(
        updatesRequiredPatch,
        nativeServerGatePatch,
        seedPreferencesPatch,
        projectWizardPatch,
        moddedByDialogPatch,
        popupDismisserPatch
    )

    execute {
        println("Master composite patch AMZ Popup Suppression completed successfully.")
    }
}
