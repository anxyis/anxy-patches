package anxyis.morphe.patches.alightmotion.daemon

import anxyis.morphe.patches.alightmotion.Constants
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference

val popupDismisserPatch = bytecodePatch(
    name = "Popup Dismisser Daemon",
    description = "Injects background runtime dialog dismisser and preference seeder to eliminate all modder popups.",
    default = true
) {
    compatibleWith(Constants.COMPATIBILITY_AM_PRO, Constants.COMPATIBILITY_AMZ)

    extendWith("extensions/classes.dex")

    execute {
        try {
            val appClass = mutableClassDefBy("Lcom/alightcreative/app/motion/AlightMotionApplication;")
            val onCreateMethod = appClass.methods.firstOrNull { it.name == "onCreate" && it.parameterTypes.isEmpty() } ?: return@execute
            val impl = onCreateMethod.implementation ?: return@execute

            val hookRef = ImmutableMethodReference(
                "Lcom/alightcreative/app/motion/persist/PopupDismisser;",
                "onStart",
                emptyList(),
                "V"
            )
            val instruction = BuilderInstruction35c(Opcode.INVOKE_STATIC, 0, 0, 0, 0, 0, 0, hookRef)
            impl.addInstruction(0, instruction)
        } catch (ignored: Exception) {
        }
    }
}
