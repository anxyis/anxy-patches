package anxyis.morphe.patches.alightmotion.dialogs

import anxyis.morphe.patches.alightmotion.Constants
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.instruction.ImmutableInstruction10x

object SystemExitFingerprint : Fingerprint(
    custom = { method, classDef ->
        classDef.type == "Lcom/google/firebase/analytics/FirebaseEncoder\$9;" &&
                method.name == "n" &&
                method.parameterTypes.size == 1 &&
                method.parameterTypes[0] == "I"
    }
)

object KillProcessFingerprint : Fingerprint(
    custom = { method, classDef ->
        classDef.type == "Lcom/google/firebase/analytics/connector/internal/core/d;" &&
                method.name == "bb" &&
                method.parameterTypes.size == 1 &&
                method.parameterTypes[0] == "I"
    }
)

val antiExitPatch = bytecodePatch(
    name = "Anti-Exit Prevention Patch",
    description = "Neutralizes System.exit and Process.killProcess triggers in fake Firebase wrapper.",
    default = false
) {
    compatibleWith(Constants.COMPATIBILITY_AMZ, Constants.COMPATIBILITY_AM_PRO)

    execute {
        val exitMethod = SystemExitFingerprint.methodOrNull
        if (exitMethod != null) {
            val mutableClass = mutableClassDefBy(SystemExitFingerprint.classDef)
            val oldMethod = mutableClass.methods.firstOrNull { it.name == "n" }
            if (oldMethod != null) {
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
                        8,
                        listOf(ImmutableInstruction10x(Opcode.RETURN_VOID)),
                        null,
                        null
                    )
                ).toMutable()
                mutableClass.methods.add(newMethod)
            }
        }

        val killMethod = KillProcessFingerprint.methodOrNull
        if (killMethod != null) {
            val mutableClass = mutableClassDefBy(KillProcessFingerprint.classDef)
            val oldMethod = mutableClass.methods.firstOrNull { it.name == "bb" }
            if (oldMethod != null) {
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
                        8,
                        listOf(ImmutableInstruction10x(Opcode.RETURN_VOID)),
                        null,
                        null
                    )
                ).toMutable()
                mutableClass.methods.add(newMethod)
            }
        }
    }
}
