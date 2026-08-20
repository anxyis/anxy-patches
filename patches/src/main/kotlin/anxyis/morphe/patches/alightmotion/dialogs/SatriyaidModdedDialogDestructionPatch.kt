package anxyis.morphe.patches.alightmotion.dialogs

import anxyis.morphe.patches.alightmotion.Constants
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction10x
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11n

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
                for (mutableMethod in mutableClass.methods) {
                    val impl = mutableMethod.implementation ?: continue
                    impl.instructions.clear()
                    when (mutableMethod.returnType) {
                        "V" -> impl.addInstruction(BuilderInstruction10x(Opcode.RETURN_VOID))
                        "Z", "I", "B", "S", "C" -> {
                            impl.addInstruction(BuilderInstruction11n(Opcode.CONST_4, 0, 0))
                            impl.addInstruction(BuilderInstruction10x(Opcode.RETURN))
                        }
                        else -> {
                            impl.addInstruction(BuilderInstruction11n(Opcode.CONST_4, 0, 0))
                            impl.addInstruction(BuilderInstruction10x(Opcode.RETURN_OBJECT))
                        }
                    }
                }
            }

            // 2. Destroy TGSatriyaidChannel native loader
            if (type == "Lx0/TGSatriyaidChannel;") {
                val mutableClass = mutableClassDefBy(classDef)
                for (mutableMethod in mutableClass.methods) {
                    val impl = mutableMethod.implementation ?: continue
                    impl.instructions.clear()
                    impl.addInstruction(BuilderInstruction10x(Opcode.RETURN_VOID))
                }
            }
        }
    }
}
