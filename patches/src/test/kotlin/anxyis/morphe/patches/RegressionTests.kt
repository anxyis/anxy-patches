package anxyis.morphe.patches

import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference
import com.android.tools.smali.dexlib2.Opcodes
import org.junit.jupiter.api.Test
import java.io.File
import java.util.zip.ZipFile

class RegressionTests {

    @Test
    fun testDirectInstructionInjection() {
        val apkFile = File("C:/Users/Admin/Desktop/AM-NO-POPUP/orignal-apks/AM_Pro_5.0.273_BangAlbin_Original.apk")
        val zip = ZipFile(apkFile)
        val dexEntries = zip.entries().toList().filter { it.name.endsWith(".dex") }

        val hookRef = ImmutableMethodReference(
            "Lcom/alightcreative/app/motion/persist/PopupDismisser;",
            "onStart",
            emptyList(),
            "V"
        )
        val instruction = BuilderInstruction35c(Opcode.INVOKE_STATIC, 0, 0, 0, 0, 0, 0, hookRef)
        println("Created instruction: $instruction")
    }
}
