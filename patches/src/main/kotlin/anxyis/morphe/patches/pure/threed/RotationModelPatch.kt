package anxyis.morphe.patches.pure.threed

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableField.Companion.toMutable as fieldToMutable
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import anxyis.morphe.patches.pure.shared.ALIGHT_5270
import anxyis.morphe.patches.pure.shared.requireClass
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.ImmutableField
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

/**
 * 3D rotation engine (playback half): X/Y layer rotation model + matrix math.
 *
 * Clean-room port of the *idea* behind Gold Motion's 3D rotation (their code
 * is untouched): stock 5.0.270 Transform/KeyableTransform carry only a scalar
 * Z rotation, so Gold-style <rotationX>/<rotationY> project data is dropped
 * on import. This patch adds the missing model + render math on OUR base:
 *
 *  - Transform: rotationX/rotationY float fields + accessors
 *  - KeyableTransform: rotationX/rotationY float + Keyable fields + accessors
 *  - TransformKt.pureFlatten3D: perspective-flatten (focal 2400) math helper
 *  - pureApplyFlatten3D / pureConcatFlatten3D: zero-guarded matrix/canvas hooks
 *  - 4 render-site hooks (getOptiMatrix, matrix$2, matrixReverseRotation$2,
 *    transform) reusing live registers only (no register growth, no labels)
 *
 * Parser/serializer/registry live in RotationParserPatch. Copy/parenting
 * inheritance is intentionally v2 (see file header there).
 */

const val TF = "Lcom/alightcreative/app/motion/scene/Transform;"
const val KTF = "Lcom/alightcreative/app/motion/scene/KeyableTransform;"
const val TKT = "Lcom/alightcreative/app/motion/scene/TransformKt;"
const val KEYABLE = "Lcom/alightcreative/app/motion/scene/Keyable;"

val rotationModelPatch = bytecodePatch(
    name = "3D rotation engine",
    description = "Adds X/Y layer rotation to the render engine (model + matrix math).",
) {
    compatibleWith(ALIGHT_5270)
    execute {
        // ---- 1. model fields + accessors ----
        addFloatFieldWithAccessors(TF, "rotationX")
        addFloatFieldWithAccessors(TF, "rotationY")
        addFloatFieldWithAccessors(KTF, "rotationX")
        addFloatFieldWithAccessors(KTF, "rotationY")
        addKeyableFieldWithAccessors(KTF, "rotationXKeyable")
        addKeyableFieldWithAccessors(KTF, "rotationYKeyable")

        // ---- 2. math helpers on TransformKt ----
        addPureFlatten3D()
        addPureApplyFlatten3D()

        // ---- 3. render-site hooks (live-register reuse, straight-line) ----
        hookMatrixSite(TF, "getOptiMatrix")
        hookMatrixLambda("Lcom/alightcreative/app/motion/scene/Transform\$matrix\$2;")
        hookMatrixLambda("Lcom/alightcreative/app/motion/scene/Transform\$matrixReverseRotation\$2;")
    }
}

// ---------------------------------------------------------------------------
// small dexlib2 helpers
// ---------------------------------------------------------------------------

private fun paramsOf(vararg types: String) = types.map { ImmutableMethodParameter(it, null, null) }

private fun regsOf(ins: Instruction): List<Int> {
    val five = ins as? FiveRegisterInstruction
        ?: throw PatchException("Pure3D: expected 5-reg invoke, got ${ins.opcode}")
    return listOf(five.registerC, five.registerD, five.registerE, five.registerF, five.registerG)
        .take(five.registerCount)
}

private fun methodRefOf(ins: Instruction): MethodReference? =
    (ins as? ReferenceInstruction)?.reference as? MethodReference

// ---------------------------------------------------------------------------
// fields + accessors
// ---------------------------------------------------------------------------

private fun BytecodePatchContext.addField(type: String, name: String, fieldType: String) {
    val holder = requireClass(type)
    if (holder.fields.any { it.name == name }) {
        throw PatchException("Pure3D: $type->$name already exists (double-patched?)")
    }
    val f = ImmutableField(type, name, fieldType, AccessFlags.PRIVATE.value, null, emptySet(), null).fieldToMutable()
    holder.fields.add(f)
}

private fun BytecodePatchContext.addGetter(type: String, field: String, fieldType: String, retInsn: String) {
    val holder = requireClass(type)
    val m = ImmutableMethod(
        type, "get" + field.replaceFirstChar { it.uppercase() }, emptyList(), fieldType,
        AccessFlags.PUBLIC.value, null, null,
        ImmutableMethodImplementation(2, emptyList(), null, null),
    ).toMutable()
    holder.methods.add(m)
    m.addInstructions(0, "iget v0, p0, $type->$field:$fieldType\n$retInsn v0")
}

private fun BytecodePatchContext.addSetter(type: String, field: String, fieldType: String) {
    val holder = requireClass(type)
    val m = ImmutableMethod(
        type, "set" + field.replaceFirstChar { it.uppercase() }, paramsOf(fieldType), "V",
        AccessFlags.PUBLIC.value, null, null,
        ImmutableMethodImplementation(2, emptyList(), null, null),
    ).toMutable()
    holder.methods.add(m)
    m.addInstructions(0, "iput p1, p0, $type->$field:$fieldType\nreturn-void")
}

private fun BytecodePatchContext.addFloatFieldWithAccessors(type: String, field: String) {
    addField(type, field, "F")
    addGetter(type, field, "F", "return")
    addSetter(type, field, "F")
}

private fun BytecodePatchContext.addKeyableFieldWithAccessors(type: String, field: String) {
    addField(type, field, KEYABLE)
    addGetter(type, field, KEYABLE, "return-object")
    addSetter(type, field, KEYABLE)
}

// ---------------------------------------------------------------------------
// math helpers (fresh methods: branches welcome, no anchor risk)
// ---------------------------------------------------------------------------

/** Perspective-flatten rotationX/Y (degrees) into out matrix. Focal 2400 (0x4516). */
private fun BytecodePatchContext.addPureFlatten3D() {
    val holder = requireClass(TKT)
    if (holder.methods.any { it.name == "pureFlatten3D" }) {
        throw PatchException("Pure3D: pureFlatten3D exists (double-patched?)")
    }
    val m = ImmutableMethod(
        TKT, "pureFlatten3D",
        paramsOf("Landroid/graphics/Matrix;", "F", "F"), "V",
        AccessFlags.PUBLIC.value or AccessFlags.STATIC.value, null, null,
        ImmutableMethodImplementation(12, emptyList(), null, null),
    ).toMutable()
    holder.methods.add(m)
    m.addInstructions(
        0,
        """
        move-object v0, p0
        move v1, p1
        move v2, p2
        const/high16 v11, 0x45160000
        float-to-double v7, v1
        invoke-static {v7, v8}, Ljava/lang/Math;->toRadians(D)D
        move-result-wide v7
        invoke-static {v7, v8}, Ljava/lang/Math;->cos(D)D
        move-result-wide v7
        double-to-float v3, v7
        float-to-double v7, v1
        invoke-static {v7, v8}, Ljava/lang/Math;->toRadians(D)D
        move-result-wide v7
        invoke-static {v7, v8}, Ljava/lang/Math;->sin(D)D
        move-result-wide v7
        double-to-float v4, v7
        float-to-double v7, v2
        invoke-static {v7, v8}, Ljava/lang/Math;->toRadians(D)D
        move-result-wide v7
        invoke-static {v7, v8}, Ljava/lang/Math;->cos(D)D
        move-result-wide v7
        double-to-float v5, v7
        float-to-double v7, v2
        invoke-static {v7, v8}, Ljava/lang/Math;->toRadians(D)D
        move-result-wide v7
        invoke-static {v7, v8}, Ljava/lang/Math;->sin(D)D
        move-result-wide v7
        double-to-float v6, v7
        const/16 v9, 9
        new-array v7, v9, [F
        const/4 v9, 0
        aput v5, v7, v9
        mul-float v8, v4, v6
        const/4 v9, 1
        aput v8, v7, v9
        const/4 v9, 2
        const/4 v8, 0
        aput v8, v7, v9
        const/4 v9, 3
        const/4 v8, 0
        aput v8, v7, v9
        const/4 v9, 4
        aput v3, v7, v9
        const/4 v9, 5
        const/4 v8, 0
        aput v8, v7, v9
        neg-float v8, v6
        div-float v8, v8, v11
        const/4 v9, 6
        aput v8, v7, v9
        mul-float v8, v4, v5
        div-float v8, v8, v11
        const/4 v9, 7
        aput v8, v7, v9
        const/16 v9, 8
        const/high16 v8, 0x3f800000
        aput v8, v7, v9
        invoke-virtual {v0, v7}, Landroid/graphics/Matrix;->setValues([F)V
        return-void
        """.trimIndent(),
    )
}

/** Zero-guarded preConcat hook: pureApplyFlatten3D(Matrix, Transform). */
private fun BytecodePatchContext.addPureApplyFlatten3D() {
    val holder = requireClass(TKT)
    val m = ImmutableMethod(
        TKT, "pureApplyFlatten3D",
        paramsOf("Landroid/graphics/Matrix;", TF), "V",
        AccessFlags.PUBLIC.value or AccessFlags.STATIC.value, null, null,
        ImmutableMethodImplementation(6, emptyList(), null, null),
    ).toMutable()
    holder.methods.add(m)
    m.addInstructions(
        0,
        """
        move-object v0, p0
        move-object v1, p1
        invoke-virtual {v1}, $TF->getRotationX()F
        move-result v2
        const/4 v3, 0
        cmpl-float v4, v2, v3
        if-nez v4, :pure3d_skip
        invoke-virtual {v1}, $TF->getRotationY()F
        move-result v2
        cmpl-float v4, v2, v3
        if-nez v4, :pure3d_skip
        new-instance v2, Landroid/graphics/Matrix;
        invoke-direct {v2}, Landroid/graphics/Matrix;-><init>()V
        invoke-virtual {v1}, $TF->getRotationX()F
        move-result v3
        invoke-virtual {v1}, $TF->getRotationY()F
        move-result v4
        invoke-static {v2, v3, v4}, $TKT->pureFlatten3D(Landroid/graphics/Matrix;FF)V
        invoke-virtual {v0, v2}, Landroid/graphics/Matrix;->preConcat(Landroid/graphics/Matrix;)Z
        :pure3d_skip
        return-void
        """.trimIndent(),
    )
}

// ---------------------------------------------------------------------------
// render-site hooks: single straight-line invoke reusing live regs
// ---------------------------------------------------------------------------

/** Find preRotate invoke index in a MutableMethod. */
private fun findPreRotate(target: MutableMethod): Int {
    val insns = target.implementation?.instructions?.toList()
        ?: throw PatchException("Pure3D: ${target.name} has no implementation")
    return insns.indexOfFirst { ins ->
        ins.opcode.name.startsWith("INVOKE", ignoreCase = true) &&
            methodRefOf(ins)?.name == "preRotate"
    }
}

/** Matrix reg = preRotate's object reg; transform reg = getRotation() caller or rotation iget just before it. */
private fun matrixHookRegs(target: MutableMethod, preIdx: Int): Pair<Int, Int> {
    val insns = target.implementation?.instructions?.toList()
        ?: throw PatchException("Pure3D: ${target.name} has no implementation")
    val vM = regsOf(insns[preIdx])[0]
    for (i in preIdx downTo maxOf(0, preIdx - 12)) {
        val o = insns[i]
        if (o.opcode.name.startsWith("INVOKE", ignoreCase = true)) {
            val ref = methodRefOf(o) ?: continue
            if (ref.name == "getRotation" && ref.definingClass == TF) {
                return vM to regsOf(o)[0]
            }
        }
        if (o.opcode == Opcode.IGET || o.opcode == Opcode.IGET_OBJECT) {
            val ref = (o as? ReferenceInstruction)?.reference as? FieldReference
            if (ref?.name == "rotation" && ref.definingClass == TF) {
                val r = (o as? TwoRegisterInstruction)?.registerB ?: continue
                return vM to r
            }
        }
    }
    throw PatchException("Pure3D: no getRotation()/rotation-iget before preRotate in ${target.name}")
}

private fun BytecodePatchContext.hookMatrixSite(type: String, method: String) {
    val target = requireClass(type).methods.singleOrNull { it.name == method }
        ?: throw PatchException("Pure3D: $type->$method not found exactly once")
    val preIdx = findPreRotate(target)
    if (preIdx < 0) throw PatchException("Pure3D: no preRotate in $type->$method")
    val (vM, vT) = matrixHookRegs(target, preIdx)
    target.addInstructions(preIdx + 1, "invoke-static {v$vM, v$vT}, $TKT->pureApplyFlatten3D(Landroid/graphics/Matrix;$TF)V")
}

private fun BytecodePatchContext.hookMatrixLambda(classType: String) {
    val cands = requireClass(classType).methods.filter { it.name == "invoke" }
    val target = cands.singleOrNull {
        it.implementation?.instructions?.any { ins ->
            methodRefOf(ins)?.name == "preRotate"
        } == true
    } ?: throw PatchException("Pure3D: $classType->invoke with preRotate not found (cands=${cands.size})")
    val preIdx = findPreRotate(target)
    if (preIdx < 0) throw PatchException("Pure3D: no preRotate in $classType->invoke")
    val (vM, vT) = matrixHookRegs(target, preIdx)
    target.addInstructions(preIdx + 1, "invoke-static {v$vM, v$vT}, $TKT->pureApplyFlatten3D(Landroid/graphics/Matrix;$TF)V")
}
