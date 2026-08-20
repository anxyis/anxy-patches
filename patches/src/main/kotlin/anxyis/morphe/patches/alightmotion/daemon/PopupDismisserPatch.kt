package anxyis.morphe.patches.alightmotion.daemon

import anxyis.morphe.patches.alightmotion.Constants
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.instruction.ImmutableInstruction35c
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference

object AlightMotionAppFingerprint : Fingerprint(
    custom = { method, classDef ->
        classDef.type == "Lcom/alightcreative/app/motion/AlightMotionApplication;" &&
                method.name == "onCreate" &&
                method.parameterTypes.isEmpty()
    }
)

val popupDismisserPatch = bytecodePatch(
    name = "Popup Dismisser Daemon",
    description = "Injects background runtime dialog dismisser and preference seeder to eliminate all modder popups.",
    default = true
) {
    compatibleWith(Constants.COMPATIBILITY_AM_PRO, Constants.COMPATIBILITY_AMZ)

    extendWith("extensions/classes.dex")

    execute {
        val appMethod = AlightMotionAppFingerprint.methodOrNull
        if (appMethod != null) {
            val mutableClass = mutableClassDefBy(AlightMotionAppFingerprint.classDef)
            val oldMethod = mutableClass.methods.firstOrNull { it.name == "onCreate" }
            if (oldMethod != null) {
                val oldImpl = oldMethod.implementation
                if (oldImpl != null) {
                    val insList = mutableListOf<Instruction>()
                    val hookInst = ImmutableInstruction35c(
                        Opcode.INVOKE_STATIC,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        ImmutableMethodReference(
                            "Lcom/alightcreative/app/motion/persist/PopupDismisser;",
                            "onStart",
                            listOf(),
                            "V"
                        )
                    )
                    insList.add(hookInst)
                    for (inst in oldImpl.instructions) {
                        insList.add(inst)
                    }
                    val regCount = oldImpl.registerCount
                    mutableClass.methods.remove(oldMethod)
                    mutableClass.methods.add(
                        ImmutableMethod(
                            oldMethod.definingClass,
                            oldMethod.name,
                            oldMethod.parameters,
                            oldMethod.returnType,
                            oldMethod.accessFlags,
                            oldMethod.annotations,
                            oldMethod.hiddenApiRestrictions,
                            ImmutableMethodImplementation(
                                regCount,
                                insList,
                                oldImpl.tryBlocks,
                                null
                            )
                        ).toMutable()
                    )
                }
            }
        }
    }
}
