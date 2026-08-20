package anxyis.morphe.patches.alightmotion

import anxyis.morphe.patches.alightmotion.daemon.popupDismisserPatch
import anxyis.morphe.patches.all.dialogs.updatesRequiredPatch
import anxyis.morphe.patches.all.prefs.seedPreferencesPatch
import app.morphe.patcher.patch.bytecodePatch

val alightMotionProNoPopupPatch = bytecodePatch(
    name = "Alight Motion Pro Popup Suppression (Complete Suite)",
    description = "Eliminates all startup, update, and modded-by popups in Alight Motion Pro (BangAlbin mod).",
    default = true
) {
    compatibleWith(Constants.COMPATIBILITY_AM_PRO)

    dependsOn(
        updatesRequiredPatch,
        seedPreferencesPatch,
        popupDismisserPatch
    )
}
