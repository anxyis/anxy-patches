package anxyis.morphe.patches.alightmotion.daemon

import anxyis.morphe.patches.alightmotion.Constants
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import org.w3c.dom.Element
import org.w3c.dom.Node

val popupDismisserManifestPatch = resourcePatch(
    name = "Popup Dismisser Manifest Hook",
    description = "Registers NoPopupSeedProvider in AndroidManifest.xml for cold-start OS initialization.",
    default = true
) {
    compatibleWith(Constants.COMPATIBILITY_AM_PRO, Constants.COMPATIBILITY_AMZ)

    execute {
        fun Node.adoptChild(
            tagName: String,
            block: Element.() -> Unit,
        ) {
            val child = ownerDocument.createElement(tagName)
            child.block()
            appendChild(child)
        }

        document("AndroidManifest.xml").use { document ->
            val applicationNode = document.getElementsByTagName("application").item(0) ?: return@use
            applicationNode.adoptChild("provider") {
                setAttribute("android:name", "com.alightcreative.app.motion.persist.NoPopupSeedProvider")
                setAttribute("android:authorities", "com.alightcreative.motion.nopopupseed")
                setAttribute("android:exported", "false")
                setAttribute("android:initOrder", "99")
            }
        }
    }
}

val popupDismisserPatch = bytecodePatch(
    name = "Popup Dismisser Daemon",
    description = "Injects background runtime dialog dismisser and preference seeder to eliminate all modder popups.",
    default = true
) {
    compatibleWith(Constants.COMPATIBILITY_AM_PRO, Constants.COMPATIBILITY_AMZ)

    extendWith("extensions/classes.dex")

    dependsOn(popupDismisserManifestPatch)
}
