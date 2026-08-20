package anxyis.morphe.patches.alightmotion

import app.morphe.patcher.Fingerprint

object FqAbFingerprint : Fingerprint(
    custom = { method, classDef ->
        classDef.interfaces.any { it.endsWith("/ge;") || it == "Lcom/google/firebase/analytics/ge;" } &&
                method.name == "ab" &&
                method.parameterTypes.size == 3 &&
                method.returnType == "V"
    }
)

object ZzzbVbdFingerprint : Fingerprint(
    custom = { method, classDef ->
        classDef.type == "Lcom/strxid/zzza/zzzb;" &&
                method.name == "vbd"
    }
)

object ZzwXyzFingerprint : Fingerprint(
    custom = { method, classDef ->
        classDef.type == "Lcom/strxid/zza/zzw;" &&
                method.name == "xyz"
    }
)
