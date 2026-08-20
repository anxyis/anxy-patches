package anxyis.morphe.patches.all.dialogs

import anxyis.morphe.patches.alightmotion.Constants
import anxyis.morphe.patches.alightmotion.FqAbFingerprint
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.instruction.ImmutableInstruction10x

val updatesRequiredPatch = bytecodePatch(
    name = "Updates Required Popup Suppression",
    description = "No-ops fq.ab dialog builder in Firebase Analytics wrapper.",
    default = true
) {
    compatibleWith(Constants.COMPATIBILITY_AMZ, Constants.COMPATIBILITY_AM_PRO)

    execute {
        val method = FqAbFingerprint.methodOrNull
            ?: throw PatchException("Fq.ab fingerprint not found")

        val mutableClass = mutableClassDefBy(FqAbFingerprint.classDef)
        val oldMethod = mutableClass.methods.firstOrNull { it.name == "ab" } ?: return@execute
        mutableClass.methods.remove(oldMethod)

        val newMethod = ImmutableMethod(
            oldMethod.definingClass,
            oldMethod.name,
            oldMethod.parameters,
            oldMethod.returnType,
            oldMethod.accessFlags,
            oldMethod.annotations,
            oldMethod.hiddenApiRestrictions,
            ImmutableMethodImplementation(
                0,
                listOf(ImmutableInstruction10x(Opcode.RETURN_VOID)),
                null,
                null
            )
        ).toMutable()

        mutableClass.methods.add(newMethod)
    }
}
