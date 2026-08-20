package anxyis.morphe.patches.alightmotion.dialogs

import anxyis.morphe.patches.alightmotion.Constants
import anxyis.morphe.patches.alightmotion.ZzzbVbdFingerprint
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.instruction.ImmutableInstruction10x
import com.android.tools.smali.dexlib2.immutable.instruction.ImmutableInstruction11n
import com.android.tools.smali.dexlib2.immutable.instruction.ImmutableInstruction11x

val projectWizardPatch = bytecodePatch(
    name = "New Project Wizard Suppression",
    description = "No-ops zzzb.vbd, zzzb.vwp, and forces zzzb.uio to return false.",
    default = true
) {
    compatibleWith(Constants.COMPATIBILITY_AMZ)

    execute {
        val method = ZzzbVbdFingerprint.methodOrNull ?: return@execute

        val mutableClass = mutableClassDefBy(ZzzbVbdFingerprint.classDef)

        mutableClass.methods.firstOrNull { it.name == "vbd" }?.let { old ->
            mutableClass.methods.remove(old)
            mutableClass.methods.add(
                ImmutableMethod(
                    old.definingClass, old.name, old.parameters, old.returnType,
                    old.accessFlags and AccessFlags.NATIVE.value.inv(),
                    old.annotations, old.hiddenApiRestrictions,
                    ImmutableMethodImplementation(1, listOf(ImmutableInstruction10x(Opcode.RETURN_VOID)), null, null)
                ).toMutable()
            )
        }

        mutableClass.methods.firstOrNull { it.name == "vwp" }?.let { old ->
            mutableClass.methods.remove(old)
            mutableClass.methods.add(
                ImmutableMethod(
                    old.definingClass, old.name, old.parameters, old.returnType,
                    old.accessFlags and AccessFlags.NATIVE.value.inv(),
                    old.annotations, old.hiddenApiRestrictions,
                    ImmutableMethodImplementation(1, listOf(ImmutableInstruction10x(Opcode.RETURN_VOID)), null, null)
                ).toMutable()
            )
        }

        mutableClass.methods.firstOrNull { it.name == "uio" }?.let { old ->
            mutableClass.methods.remove(old)
            mutableClass.methods.add(
                ImmutableMethod(
                    old.definingClass, old.name, old.parameters, old.returnType,
                    old.accessFlags and AccessFlags.NATIVE.value.inv(),
                    old.annotations, old.hiddenApiRestrictions,
                    ImmutableMethodImplementation(
                        1,
                        listOf(
                            ImmutableInstruction11n(Opcode.CONST_4, 0, 0),
                            ImmutableInstruction11x(Opcode.RETURN, 0)
                        ),
                        null,
                        null
                    )
                ).toMutable()
            )
        }
    }
}
