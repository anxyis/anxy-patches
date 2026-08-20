package anxyis.morphe.patches.alightmotion.dialogs

import anxyis.morphe.patches.alightmotion.Constants
import anxyis.morphe.patches.alightmotion.ZzwXyzFingerprint
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.instruction.ImmutableInstruction10x

val moddedByDialogPatch = bytecodePatch(
    name = "Modded By Satriyaid Dialog Suppression",
    description = "No-ops zzw.xyz startup dialog entry.",
    default = true
) {
    compatibleWith(Constants.COMPATIBILITY_AMZ)

    execute {
        val method = ZzwXyzFingerprint.methodOrNull ?: return@execute

        val mutableClass = mutableClassDefBy(ZzwXyzFingerprint.classDef)
        val oldMethod = mutableClass.methods.firstOrNull { it.name == "xyz" } ?: return@execute
        mutableClass.methods.remove(oldMethod)

        val newMethod = ImmutableMethod(
            oldMethod.definingClass,
            oldMethod.name,
            oldMethod.parameters,
            oldMethod.returnType,
            oldMethod.accessFlags and AccessFlags.NATIVE.value.inv(),
            oldMethod.annotations,
            oldMethod.hiddenApiRestrictions,
            ImmutableMethodImplementation(
                1,
                listOf(ImmutableInstruction10x(Opcode.RETURN_VOID)),
                null,
                null
            )
        ).toMutable()

        mutableClass.methods.add(newMethod)
    }
}
