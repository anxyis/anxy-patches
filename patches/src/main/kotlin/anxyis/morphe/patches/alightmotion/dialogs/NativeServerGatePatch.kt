package anxyis.morphe.patches.alightmotion.dialogs

import anxyis.morphe.patches.alightmotion.Constants
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.rawResourcePatch

val nativeServerGatePatch = rawResourcePatch(
    name = "Native Server 1 Startup Gate Bypass",
    description = "NOPs native gate cbz branch in libsatriyaid.so at 0x585c0.",
    default = false
) {
    compatibleWith(Constants.COMPATIBILITY_AMZ)

    execute {
        val targetPath = "lib/arm64-v8a/libsatriyaid.so"
        val libFile = get(targetPath)
        if (!libFile.exists()) {
            return@execute
        }

        val data = libFile.readBytes()
        val signature = byteArrayOf(
            0xa8.toByte(), 0x83.toByte(), 0x5c.toByte(), 0xb8.toByte(),
            0xc8.toByte(), 0x12.toByte(), 0x00.toByte(), 0x34.toByte()
        )
        val nop = byteArrayOf(0x1f.toByte(), 0x20.toByte(), 0x03.toByte(), 0xd5.toByte())

        var matchIndex = -1
        var matchCount = 0
        outer@ for (i in 0..(data.size - signature.size)) {
            for (j in signature.indices) {
                if (data[i + j] != signature[j]) continue@outer
            }
            matchCount++
            matchIndex = i
        }

        if (matchCount == 0) {
            throw PatchException("Signature not found in $targetPath - failing closed to prevent corrupt patch")
        }
        if (matchCount > 1) {
            throw PatchException("Ambiguous signature match ($matchCount occurrences) in $targetPath")
        }

        val patchedData = data.clone()
        System.arraycopy(nop, 0, patchedData, matchIndex + 4, 4)
        libFile.writeBytes(patchedData)
    }
}
