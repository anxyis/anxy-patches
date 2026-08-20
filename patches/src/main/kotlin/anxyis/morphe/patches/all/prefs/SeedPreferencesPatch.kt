package anxyis.morphe.patches.all.prefs

import anxyis.morphe.patches.alightmotion.Constants
import app.morphe.patcher.patch.rawResourcePatch

val seedPreferencesPatch = rawResourcePatch(
    name = "Seed Default Preferences",
    description = "Seeds initial suppression preferences in shared_prefs/.",
    default = true
) {
    compatibleWith(Constants.COMPATIBILITY_AMZ, Constants.COMPATIBILITY_AM_PRO)

    execute {
        fun writeSeed(path: String, content: String) {
            val file = get(path, true)
            file.parentFile?.mkdirs()
            file.writeText(content)
        }

        writeSeed(
            "shared_prefs/dialog.xml",
            """<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <int name="view" value="0"/>
    <boolean name="first" value="true"/>
</map>
"""
        )

        writeSeed(
            "shared_prefs/wdprefs1.xml",
            """<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <boolean name="is_shown" value="true"/>
</map>
"""
        )

        writeSeed(
            "shared_prefs/AlbinModsDialogPrefs.xml",
            """<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <boolean name="dont_show_again" value="true"/>
    <boolean name="dialogLock" value="true"/>
    <int name="show_interval" value="999999999"/>
    <long name="last_show_time" value="9999999999999"/>
    <boolean name="show_close_button" value="false"/>
</map>
"""
        )
    }
}
