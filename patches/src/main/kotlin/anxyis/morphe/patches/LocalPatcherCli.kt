package anxyis.morphe.patches

import anxyis.morphe.patches.alightmotion.alightMotionProNoPopupPatch
import app.morphe.patcher.Patcher
import app.morphe.patcher.PatcherConfig
import app.morphe.patcher.patch.Patch
import kotlinx.coroutines.runBlocking
import java.io.File

fun main(args: Array<String>) = runBlocking {
    val inputApkPath = if (args.isNotEmpty()) args[0] else "C:/Users/Admin/Downloads/Alight_Motion_(5.0.273.1028420).apk"
    val outputApkPath = if (args.size > 1) args[1] else "C:/Users/Admin/Desktop/AM-NO-POPUP/output/Alight_Motion_5.0.273_MorphePatched.apk"

    val inputFile = File(inputApkPath)
    val outputFile = File(outputApkPath)
    val tempDir = File("build/tmp/morphe-patcher-run").apply {
        deleteRecursively()
        mkdirs()
    }

    println("=== Running Morphe Patcher CLI Locally ===")
    println("Input APK:  ${inputFile.absolutePath} (${inputFile.length()} bytes)")
    println("Output APK: ${outputFile.absolutePath}")

    val patchesToApply = mutableSetOf<Patch<*>>()
    fun collectDependencies(patch: Patch<*>) {
        if (patchesToApply.add(patch)) {
            patch.dependencies.forEach { collectDependencies(it) }
        }
    }
    collectDependencies(alightMotionProNoPopupPatch)

    println("Applying ${patchesToApply.size} patches:")
    patchesToApply.forEach { println(" - ${it.name}") }

    val config = PatcherConfig(
        apkFile = inputFile,
        patchedFiles = tempDir
    )

    val patcher = Patcher(config, patchesToApply)
    
    var lastProgress = -1
    patcher().collect { progress ->
        if (progress.percentage != lastProgress) {
            lastProgress = progress.percentage
            println("Progress: ${progress.percentage}% - ${progress.state}")
        }
    }

    val result = patcher.save()
    outputFile.parentFile.mkdirs()
    outputFile.outputStream().use { out ->
        result.copyTo(out)
    }

    println("SUCCESS! Patched APK written to ${outputFile.absolutePath} (${outputFile.length()} bytes)")
}
