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
import kotlin.math.max

val updatesRequiredPatch = bytecodePatch(
    name = "Updates Required Popup Suppression",
    description = "No-ops fq.ab dialog builder in Firebase Analytics wrapper.",
    default = false
) {
    compatibleWith(Constants.COMPATIBILITY_AMZ, Constants.COMPATIBILITY_AM_PRO)

    execute {
        val method = FqAbFingerprint.methodOrNull
            ?: throw PatchException("Fq.ab fingerprint not found")

        val mutableClass = mutableClassDefBy(FqAbFingerprint.classDef)
        val oldMethod = mutableClass.methods.firstOrNull { it.name == "ab" } ?: return@execute
        mutableClass.methods.remove(oldMethod)

        val paramCount = oldMethod.parameters.size + (if ((oldMethod.accessFlags and 0x0008) == 0) 1 else 0)
        val registerCount = max(paramCount, 8)

        val newMethod = ImmutableMethod(
            oldMethod.definingClass,
            oldMethod.name,
            oldMethod.parameters,
            oldMethod.returnType,
            oldMethod.accessFlags,
            oldMethod.annotations,
            oldMethod.hiddenApiRestrictions,
            ImmutableMethodImplementation(
                registerCount,
                listOf(ImmutableInstruction10x(Opcode.RETURN_VOID)),
                null,
                null
            )
        ).toMutable()

        mutableClass.methods.add(newMethod)
    }
}
