package anxyis.morphe.patches.alightmotion

import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

object Constants {
    const val PACKAGE_NAME = "com.alightcreative.motion"
    const val TARGET_VERSION_AMZ = "5.0.273.1028426"
    const val TARGET_VERSION_PRO = "5.0.273.1028420"
    const val TARGET_VERSION_SHORT = "5.0.273"

    val COMPATIBILITY_AMZ = Compatibility(
        name = "After Motion Z+",
        packageName = PACKAGE_NAME,
        targets = listOf(
            AppTarget(TARGET_VERSION_PRO),
            AppTarget(TARGET_VERSION_AMZ),
            AppTarget(TARGET_VERSION_SHORT)
        )
    )

    val COMPATIBILITY_AM_PRO = Compatibility(
        name = "Alight Motion Pro",
        packageName = PACKAGE_NAME,
        targets = listOf(
            AppTarget(TARGET_VERSION_PRO),
            AppTarget(TARGET_VERSION_AMZ),
            AppTarget(TARGET_VERSION_SHORT)
        )
    )
}
