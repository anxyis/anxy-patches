package anxyis.morphe.patches.alightmotion.dialogs

import anxyis.morphe.patches.alightmotion.Constants
import anxyis.morphe.patches.alightmotion.ZzzbVbdFingerprint
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.instruction.ImmutableInstruction10x
import com.android.tools.smali.dexlib2.immutable.instruction.ImmutableInstruction11n
import com.android.tools.smali.dexlib2.immutable.instruction.ImmutableInstruction11x

val projectWizardPatch = bytecodePatch(
    name = "New Project Wizard Suppression",
    description = "No-ops zzzb.vbd, zzzb.vwp, and forces zzzb.uio to return false.",
    default = false
) {
    compatibleWith(Constants.COMPATIBILITY_AMZ)

    execute {
        val method = ZzzbVbdFingerprint.methodOrNull
            ?: throw PatchException("Zzzb.vbd fingerprint not found")

        val mutableClass = mutableClassDefBy(ZzzbVbdFingerprint.classDef)

        val vbd = mutableClass.methods.firstOrNull { it.name == "vbd" }
        if (vbd != null) {
            mutableClass.methods.remove(vbd)
            val pCount = vbd.parameters.size + (if ((vbd.accessFlags and 0x0008) != 0) 0 else 1)
            val regCount = if (pCount > 8) pCount else 8
            mutableClass.methods.add(
                ImmutableMethod(
                    vbd.definingClass,
                    vbd.name,
                    vbd.parameters,
                    vbd.returnType,
                    vbd.accessFlags,
                    vbd.annotations,
                    vbd.hiddenApiRestrictions,
                    ImmutableMethodImplementation(
                        regCount,
                        listOf(ImmutableInstruction10x(Opcode.RETURN_VOID)),
                        null,
                        null
                    )
                ).toMutable()
            )
        }

        val vwp = mutableClass.methods.firstOrNull { it.name == "vwp" }
        if (vwp != null) {
            mutableClass.methods.remove(vwp)
            val pCount = vwp.parameters.size + (if ((vwp.accessFlags and 0x0008) != 0) 0 else 1)
            val regCount = if (pCount > 8) pCount else 8
            mutableClass.methods.add(
                ImmutableMethod(
                    vwp.definingClass,
                    vwp.name,
                    vwp.parameters,
                    vwp.returnType,
                    vwp.accessFlags,
                    vwp.annotations,
                    vwp.hiddenApiRestrictions,
                    ImmutableMethodImplementation(
                        regCount,
                        listOf(ImmutableInstruction10x(Opcode.RETURN_VOID)),
                        null,
                        null
                    )
                ).toMutable()
            )
        }

        val uio = mutableClass.methods.firstOrNull { it.name == "uio" }
        if (uio != null) {
            mutableClass.methods.remove(uio)
            val pCount = uio.parameters.size + (if ((uio.accessFlags and 0x0008) != 0) 0 else 1)
            val regCount = if (pCount > 8) pCount else 8
            mutableClass.methods.add(
                ImmutableMethod(
                    uio.definingClass,
                    uio.name,
                    uio.parameters,
                    uio.returnType,
                    uio.accessFlags,
                    uio.annotations,
                    uio.hiddenApiRestrictions,
                    ImmutableMethodImplementation(
                        regCount,
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
