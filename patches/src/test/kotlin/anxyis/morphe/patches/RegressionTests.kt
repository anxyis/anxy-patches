package anxyis.morphe.patches

import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.Opcodes
import org.junit.jupiter.api.Test
import java.io.File
import java.util.zip.ZipFile

class RegressionTests {

    @Test
    fun testAlightMotionApplicationAMZ() {
        val apkFile = File("C:/Users/Admin/Desktop/AM-NO-POPUP/orignal-apks/AMZ_5.0.273_Satriyaid_Original.apk")
        val zip = ZipFile(apkFile)
        val dexEntries = zip.entries().toList().filter { it.name.endsWith(".dex") }

        for (entry in dexEntries) {
            val bytes = zip.getInputStream(entry).readBytes()
            val dex = DexBackedDexFile.fromInputStream(Opcodes.getDefault(), bytes.inputStream())
            for (classDef in dex.classes) {
                if (classDef.type == "Lcom/alightcreative/app/motion/AlightMotionApplication;") {
                    println("Found AlightMotionApplication in AMZ: ${classDef.type}")
                    for (method in classDef.methods) {
                        if (method.name == "onCreate") {
                            println("  Method: ${method.name} params=${method.parameterTypes} hasImpl=${method.implementation != null}")
                        }
                    }
                }
            }
        }
    }
}
