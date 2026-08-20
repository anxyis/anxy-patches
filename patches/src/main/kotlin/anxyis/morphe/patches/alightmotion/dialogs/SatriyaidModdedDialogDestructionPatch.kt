package anxyis.morphe.patches.alightmotion.dialogs

import anxyis.morphe.patches.alightmotion.Constants
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.instruction.ImmutableInstruction10x
import com.android.tools.smali.dexlib2.immutable.instruction.ImmutableInstruction11n

val satriyaidModdedDialogDestructionPatch = bytecodePatch(
    name = "Modded By Satriyaid Dialog Destruction",
    description = "Completely eradicates and severs all ModdedBySatriyaid and TGSatriyaidChannel popup reflection classes at the root.",
    default = true
) {
    compatibleWith(Constants.COMPATIBILITY_AM_PRO, Constants.COMPATIBILITY_AMZ)

    execute {
        classDefForEach { classDef ->
            val type = classDef.type

            // 1. Destroy all ModdedBySatriyaid reflection methods
            if (type.startsWith("Lcom/alightcreative/app/motion/activities/main/ModdedBySatriyaid/")) {
                val mutableClass = mutableClassDefBy(classDef)
                val methodList = mutableClass.methods.toList()
                for (m in methodList) {
                    mutableClass.methods.remove(m)
                    val instructions = when (m.returnType) {
                        "V" -> listOf(ImmutableInstruction10x(Opcode.RETURN_VOID))
                        "Z", "I", "B", "S", "C" -> listOf(
                            ImmutableInstruction11n(Opcode.CONST_4, 0, 0),
                            ImmutableInstruction10x(Opcode.RETURN)
                        )
                        else -> listOf(
                            ImmutableInstruction11n(Opcode.CONST_4, 0, 0),
                            ImmutableInstruction10x(Opcode.RETURN_OBJECT)
                        )
                    }
                    val newMethod = ImmutableMethod(
                        m.definingClass,
                        m.name,
                        m.parameters,
                        m.returnType,
                        m.accessFlags and 0x0100.inv(),
                        m.annotations,
                        m.hiddenApiRestrictions,
                        ImmutableMethodImplementation(1, instructions, null, null)
                    ).toMutable()
                    mutableClass.methods.add(newMethod)
                }
            }

            // 2. Destroy TGSatriyaidChannel native loader
            if (type == "Lx0/TGSatriyaidChannel;") {
                val mutableClass = mutableClassDefBy(classDef)
                val methodList = mutableClass.methods.toList()
                for (m in methodList) {
                    mutableClass.methods.remove(m)
                    val newMethod = ImmutableMethod(
                        m.definingClass,
                        m.name,
                        m.parameters,
                        m.returnType,
                        m.accessFlags and 0x0100.inv(),
                        m.annotations,
                        m.hiddenApiRestrictions,
                        ImmutableMethodImplementation(0, listOf(ImmutableInstruction10x(Opcode.RETURN_VOID)), null, null)
                    ).toMutable()
                    mutableClass.methods.add(newMethod)
                }
            }
        }
    }
}
