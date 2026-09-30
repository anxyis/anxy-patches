package anxyis.morphe.patches.pure.threed

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
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
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ThreeRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import com.android.tools.smali.dexlib2.immutable.ImmutableField
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

/**
 * 3D rotation import/export (playback half): parser, serializer, registry.
 *
 * NOTE: dexlib2 BuilderInstruction.toString() is useless (object hashes), so
 * ALL anchor discovery here is structural (opcodes + typed references).
 * v1 scope: static attribute values. Keyframed children + copy/parenting are v2.
 */

val rotationParserPatch = bytecodePatch(
    name = "3D rotation import",
    description = "Reads and writes X/Y rotation in project files (static values).",
) {
    compatibleWith(ALIGHT_5270)
    execute {
        addPureAttrFloat()
        addPureWriteRotationAttrs()
        addParserSidecar()
        hookParserTail()
        hookSerialize()
        hookGetKeyableProperties()
        hookHasKeyframes()
        hookAsKeyable()
        hookValueAtTime()
    }
}

// ---------------------------------------------------------------------------
// structured dex helpers
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

private fun isInvoke(ins: Instruction) = ins.opcode.name.startsWith("INVOKE", ignoreCase = true)

/** Registers an instruction may read (conservative: includes destinations). */
private fun usedRegs(ins: Instruction): Set<Int> {
    val s = mutableSetOf<Int>()
    if (ins is OneRegisterInstruction) s.add(ins.registerA)
    if (ins is TwoRegisterInstruction) {
        s.add(ins.registerA); s.add(ins.registerB)
    }
    if (ins is ThreeRegisterInstruction) {
        s.add(ins.registerA); s.add(ins.registerB); s.add(ins.registerC)
    }
    if (ins is FiveRegisterInstruction) {
        s.addAll(listOf(ins.registerC, ins.registerD, ins.registerE, ins.registerF, ins.registerG).take(ins.registerCount))
    }
    if (ins is RegisterRangeInstruction) {
        for (i in 0 until ins.registerCount) s.add(ins.startRegister + i)
    }
    return s
}

// ---------------------------------------------------------------------------
// fresh-method helpers
// ---------------------------------------------------------------------------

private fun BytecodePatchContext.addNewStaticMethod(
    name: String,
    params: List<String>,
    ret: String,
    regs: Int,
    body: String,
) {
    val holder = requireClass(TKT)
    if (holder.methods.any { it.name == name }) {
        throw PatchException("Pure3D: $name exists (double-patched?)")
    }
    val m = ImmutableMethod(
        TKT, name, paramsOf(*params.toTypedArray()), ret,
        AccessFlags.PUBLIC.value or AccessFlags.STATIC.value, null, null,
        ImmutableMethodImplementation(regs, emptyList(), null, null),
    ).toMutable()
    holder.methods.add(m)
    m.addInstructions(0, body.trimIndent())
}

/** pureAttrFloat(String?)F: null/parse-fail -> 0. */
private fun BytecodePatchContext.addPureAttrFloat() {
    addNewStaticMethod(
        "pureAttrFloat", listOf("Ljava/lang/String;"), "F", 3,
        """
        move-object v0, p0
        if-nez v0, :pure3d_af_parse
        const/4 v0, 0
        return v0
        :pure3d_af_parse
        invoke-static {v0}, Lkotlin/text/StringsKt;->toFloatOrNull(Ljava/lang/String;)Ljava/lang/Float;
        move-result-object v0
        if-nez v0, :pure3d_af_unbox
        const/4 v0, 0
        return v0
        :pure3d_af_unbox
        invoke-virtual {v0}, Ljava/lang/Float;->floatValue()F
        move-result v0
        return v0
        """,
    )
}

/** pureWriteRotationAttrs(XmlSerializer, String ns, KeyableTransform). */
private fun BytecodePatchContext.addPureWriteRotationAttrs() {
    addNewStaticMethod(
        "pureWriteRotationAttrs",
        listOf("Lorg/xmlpull/v1/XmlSerializer;", "Ljava/lang/String;", KTF), "V", 6,
        """
        move-object v0, p0
        move-object v1, p1
        move-object v2, p2
        invoke-virtual {v2}, $KTF->getRotationX()F
        move-result v3
        const/4 v4, 0
        cmpl-float v5, v3, v4
        if-eqz v5, :pure3d_wx
        const-string v5, "rotationX"
        invoke-static {v3}, Ljava/lang/Float;->toString(F)Ljava/lang/String;
        move-result-object v3
        invoke-interface {v0, v1, v5, v3}, Lorg/xmlpull/v1/XmlSerializer;->attribute(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Lorg/xmlpull/v1/XmlSerializer;
        :pure3d_wx
        invoke-virtual {v2}, $KTF->getRotationY()F
        move-result v3
        cmpl-float v5, v3, v4
        if-eqz v5, :pure3d_wy
        const-string v5, "rotationY"
        invoke-static {v3}, Ljava/lang/Float;->toString(F)Ljava/lang/String;
        move-result-object v3
        invoke-interface {v0, v1, v5, v3}, Lorg/xmlpull/v1/XmlSerializer;->attribute(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Lorg/xmlpull/v1/XmlSerializer;
        :pure3d_wy
        return-void
        """,
    )
}

private fun BytecodePatchContext.addParserSidecar() {
    val holder = requireClass(TKT)
    if (holder.fields.any { it.name == "pureParser" }) {
        throw PatchException("Pure3D: pureParser exists (double-patched?)")
    }
    val f = ImmutableField(
        TKT, "pureParser", "Lorg/xmlpull/v1/XmlPullParser;",
        AccessFlags.PRIVATE.value or AccessFlags.STATIC.value, null, emptySet(), null,
    ).fieldToMutable()
    holder.fields.add(f)
}

// ---------------------------------------------------------------------------
// insertion utilities
// ---------------------------------------------------------------------------

private fun insnsOf(m: MutableMethod) =
    m.implementation?.instructions?.toList()
        ?: throw PatchException("Pure3D: ${m.name} has no implementation")

private fun BytecodePatchContext.methodOf(type: String, name: String): MutableMethod =
    requireClass(type).methods.singleOrNull { it.name == name }
        ?: throw PatchException("Pure3D: $type->$name not found exactly once")

/** Low regs untouched at/after fromIdx (structural scan). Capped v0..v15:
 * plain 35c invokes cannot address higher regs. */
private fun findDeadRegs(m: MutableMethod, fromIdx: Int, count: Int, reserved: Set<Int> = emptySet()): List<Int> {
    val insns = insnsOf(m)
    val used = mutableSetOf<Int>()
    for (i in fromIdx until insns.size) {
        used.addAll(usedRegs(insns[i]))
    }
    return (0..15).filter { it !in used && it !in reserved }.take(count)
        .also { if (it.size < count) throw PatchException("Pure3D: no $count dead regs in ${m.name}@$fromIdx") }
}

/**
 * Copy possibly-high regs into dead low temps (plain 35c invokes max out at
 * v15). Returns prologue smali + mapped reg list. Types: true = object.
 */
private fun lowCopies(
    m: MutableMethod,
    fromIdx: Int,
    regs: List<Pair<Int, Boolean>>,
): Pair<String, List<Int>> {
    val needHigh = regs.withIndex().filter { it.value.first > 15 }
    if (needHigh.isEmpty()) return "" to regs.map { it.first }
    val reserved = regs.map { it.first }.toSet()
    val temps = findDeadRegs(m, fromIdx, needHigh.size, reserved)
    val sb = StringBuilder()
    val mapped = regs.map { (r, isObj) ->
        val hi = needHigh.indexOfFirst { it.value.first == r }
        if (hi < 0) {
            r
        } else {
            val t = temps[hi]
            sb.append(if (isObj) "move-object/from16 v$t, v$r\n" else "move/from16 v$t, v$r\n")
            t
        }
    }
    return sb.toString() to mapped
}

// ---------------------------------------------------------------------------
// hooks
// ---------------------------------------------------------------------------

/** Stash parser at entry; apply rotation attrs + clear at tail. */
private fun BytecodePatchContext.hookParserTail() {
    val m = methodOf(TKT, "keyableTransformFromXml")
    var insns = insnsOf(m)
    var entryIdx = -1
    var parserEntry = -1
    for (i in insns.indices) {
        val s = insns[i]
        if (s.opcode != Opcode.CONST_STRING) continue
        val str = ((s as? ReferenceInstruction)?.reference as? StringReference)?.string ?: continue
        if (str != "parser") continue
        val nxt = insns.getOrNull(i + 1) ?: continue
        val ref = methodRefOf(nxt) ?: continue
        if (ref.name != "checkNotNullParameter") continue
        parserEntry = regsOf(nxt)[0]
        entryIdx = i + 2
        break
    }
    if (entryIdx < 0 || parserEntry < 0) throw PatchException("Pure3D: parser entry not found")
    m.addInstructions(entryIdx, "sput-object v$parserEntry, $TKT->pureParser:Lorg/xmlpull/v1/XmlPullParser;")
    insns = insnsOf(m)
    val tailIdx = insns.indexOfLast { it.opcode == Opcode.RETURN_OBJECT }
    if (tailIdx < 0) throw PatchException("Pure3D: no return-object in keyableTransformFromXml")
    val retReg = (insns[tailIdx] as? OneRegisterInstruction)?.registerA
        ?: throw PatchException("Pure3D: return reg not found")
    val dead = findDeadRegs(m, tailIdx, 3, setOf(retReg))
    val (vA, vB, vC) = dead
    val (prolog, mappedKt) = lowCopies(m, tailIdx, listOf(retReg to true))
    // NOTE: lowCopies temps may alias vA/vB/vC; body re-inits those first, so order is safe.
    val vKt = mappedKt[0]
    m.addInstructions(
        tailIdx,
        prolog + """
        sget-object v$vA, $TKT->pureParser:Lorg/xmlpull/v1/XmlPullParser;
        const/4 v$vB, 0
        const-string v$vC, "rotationX"
        invoke-interface {v$vA, v$vB, v$vC}, Lorg/xmlpull/v1/XmlPullParser;->getAttributeValue(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
        move-result-object v$vC
        invoke-static {v$vC}, $TKT->pureAttrFloat(Ljava/lang/String;)F
        move-result v$vC
        invoke-virtual {v$vKt, v$vC}, $KTF->setRotationX(F)V
        invoke-static {v$vC}, Lcom/alightcreative/app/motion/scene/KeyableKt;->keyable(F)Lcom/alightcreative/app/motion/scene/KeyableFloat;
        move-result-object v$vC
        invoke-virtual {v$vKt, v$vC}, $KTF->setRotationXKeyable($KEYABLE)V
        const-string v$vC, "rotationY"
        invoke-interface {v$vA, v$vB, v$vC}, Lorg/xmlpull/v1/XmlPullParser;->getAttributeValue(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
        move-result-object v$vC
        invoke-static {v$vC}, $TKT->pureAttrFloat(Ljava/lang/String;)F
        move-result v$vC
        invoke-virtual {v$vKt, v$vC}, $KTF->setRotationY(F)V
        invoke-static {v$vC}, Lcom/alightcreative/app/motion/scene/KeyableKt;->keyable(F)Lcom/alightcreative/app/motion/scene/KeyableFloat;
        move-result-object v$vC
        invoke-virtual {v$vKt, v$vC}, $KTF->setRotationYKeyable($KEYABLE)V
        const/4 v$vB, 0
        sput-object v$vB, $TKT->pureParser:Lorg/xmlpull/v1/XmlPullParser;
        """.trimIndent(),
    )
}

/** serialize: helper call before endTag (regs from startTag call). */
private fun BytecodePatchContext.hookSerialize() {
    val m = methodOf(TKT, "serialize")
    val insns = insnsOf(m)
    var vS = -1
    var vN = -1
    var vK = -1
    for (s in insns) {
        val ref = methodRefOf(s)
        if (ref != null && ref.name == "startTag" && ref.definingClass == "Lorg/xmlpull/v1/XmlSerializer;") {
            val r = regsOf(s)
            if (r.size >= 3) {
                vS = r[0]; vN = r[1]
            }
        }
        if (ref != null && ref.name == "getRotation" && ref.definingClass == KTF) {
            vK = regsOf(s)[0]
        }
    }
    if (vS < 0 || vN < 0 || vK < 0) throw PatchException("Pure3D: serialize regs not found ($vS,$vN,$vK)")
    val endIdx = insns.indexOfFirst { methodRefOf(it)?.name == "endTag" }
    if (endIdx < 0) throw PatchException("Pure3D: no endTag in serialize")
    val (prolog, mm) = lowCopies(m, endIdx, listOf(vS to true, vN to true, vK to true))
    m.addInstructions(endIdx, prolog + "invoke-static {v${mm[0]}, v${mm[1]}, v${mm[2]}}, $TKT->pureWriteRotationAttrs(Lorg/xmlpull/v1/XmlSerializer;Ljava/lang/String;$KTF)V")
}

/**
 * getKeyableProperties: array 6 -> 8, append rotation puts.
 * Verified shape: const 6/new-array; puts idx0..5; vK=KT; v1/v2 dead after.
 */
private fun BytecodePatchContext.hookGetKeyableProperties() {
    val m = methodOf(TKT, "getKeyableProperties")
    var insns = insnsOf(m)
    var sizeIdx = -1
    var arrReg = -1
    for (i in insns.indices) {
        val s = insns[i]
        if (s.opcode != Opcode.NEW_ARRAY) continue
        val type = ((s as? ReferenceInstruction)?.reference as? TypeReference)?.type ?: continue
        if (!type.contains("Keyable")) continue
        val two = s as? TwoRegisterInstruction ?: continue
        arrReg = two.registerB
        val prev = insns.getOrNull(i - 1) as? NarrowLiteralInstruction ?: continue
        if (prev.narrowLiteral == 6 && (prev as? OneRegisterInstruction)?.registerA == arrReg) {
            sizeIdx = i - 1
        }
        break
    }
    if (sizeIdx < 0) throw PatchException("Pure3D: array size const not found")
    var vK = -1
    for (s in insns) {
        val ref = methodRefOf(s)
        if (ref != null && ref.name == "getLocation" && ref.definingClass == KTF) {
            vK = regsOf(s)[0]
            break
        }
    }
    if (vK < 0) throw PatchException("Pure3D: KT reg not found")
    m.removeInstructions(sizeIdx, 1)
    m.addInstructions(sizeIdx, "const/16 v$arrReg, 8")
    insns = insnsOf(m)
    val lastAput = insns.indexOfLast { it.opcode == Opcode.APUT_OBJECT }
    if (lastAput < 0) throw PatchException("Pure3D: no aput found")
    val (prolog, mm) = lowCopies(m, lastAput + 1, listOf(vK to true))
    val vKK = mm[0]
    m.addInstructions(
        lastAput + 1,
        prolog + """
        const/4 v1, 6
        invoke-virtual {v$vKK}, $KTF->getRotationXKeyable()$KEYABLE
        move-result-object v2
        aput-object v2, v$arrReg, v1
        const/4 v1, 7
        invoke-virtual {v$vKK}, $KTF->getRotationYKeyable()$KEYABLE
        move-result-object v2
        aput-object v2, v$arrReg, v1
        """.trimIndent(),
    )
}

/**
 * hasKeyframes: early-true after scale check (vK live, v0 dead).
 * Verified shape: getScale check + if-nez.
 */
private fun BytecodePatchContext.hookHasKeyframes() {
    val m = methodOf(TKT, "hasKeyframes")
    val insns = insnsOf(m)
    var anchor = -1
    var vK = -1
    for (i in insns.indices) {
        val ref = methodRefOf(insns[i])
        if (ref != null && ref.name == "getScale" && ref.definingClass == KTF) {
            vK = regsOf(insns[i])[0]
            for (j in i + 1 until minOf(i + 8, insns.size)) {
                if (insns[j].opcode == Opcode.IF_NEZ) {
                    anchor = j + 1
                    break
                }
            }
            break
        }
    }
    if (anchor < 0 || vK < 0) throw PatchException("Pure3D: hasKeyframes anchor not found")
    val (prolog, mm) = lowCopies(m, anchor, listOf(vK to true))
    val vKK = mm[0]
    m.addInstructions(
        anchor,
        prolog + """
        invoke-virtual {v$vKK}, $KTF->getRotationXKeyable()$KEYABLE
        move-result-object v0
        invoke-interface {v0}, $KEYABLE->getKeyed()Z
        move-result v0
        if-nez v0, :pure3d_hk_next
        invoke-virtual {v$vKK}, $KTF->getRotationYKeyable()$KEYABLE
        move-result-object v0
        invoke-interface {v0}, $KEYABLE->getKeyed()Z
        move-result v0
        if-nez v0, :pure3d_hk_next
        const/4 v0, 1
        return v0
        :pure3d_hk_next
        """.trimIndent(),
    )
}

/**
 * asKeyable: float holders after last transform use, setters pre-return.
 * Verified shape: getSize()F last use; new-instance KTF; return KTF.
 */
private fun BytecodePatchContext.hookAsKeyable() {
    val m = methodOf(TKT, "asKeyable")
    var insns = insnsOf(m)
    var sizeIdx = -1
    var vT = -1
    for (i in insns.indices) {
        val ref = methodRefOf(insns[i])
        if (ref != null && ref.name == "getSize" && ref.definingClass == TF) {
            vT = regsOf(insns[i])[0]
            sizeIdx = i
            break
        }
    }
    if (sizeIdx < 0 || vT < 0) throw PatchException("Pure3D: asKeyable getSize anchor not found")
    // Insert AFTER getSize's move-result (sizeIdx+2): holders must survive to
    // return. Verify no writes to chosen holders in between (fail fast).
    insns = insnsOf(m)
    val afterMove = sizeIdx + 2
    val holders = findDeadRegs(m, afterMove, 2, setOf(vT))
    val retIdx0 = insns.indexOfLast { it.opcode == Opcode.RETURN_OBJECT }
    for (h in holders) {
        for (i in afterMove until retIdx0) {
            val o = insns[i].toString()
            if (Regex("""(const|move-result|new-instance|iget|sget|aget)[^\n]*?\bv$h\b""").containsMatchIn(o)) {
                throw PatchException("Pure3D: holder v$h clobbered in asKeyable")
            }
        }
    }
    val (vH0, vH1) = holders
    val (prolog, mm) = lowCopies(m, afterMove, listOf(vT to true))
    val vTT = mm[0]
    m.addInstructions(
        afterMove,
        prolog + """
        invoke-virtual {v$vTT}, $TF->getRotationX()F
        move-result v$vH0
        invoke-virtual {v$vTT}, $TF->getRotationY()F
        move-result v$vH1
        """.trimIndent(),
    )
    insns = insnsOf(m)
    val retIdx = insns.indexOfLast { it.opcode == Opcode.RETURN_OBJECT }
    if (retIdx < 0) throw PatchException("Pure3D: no return in asKeyable")
    val retReg = (insns[retIdx] as? OneRegisterInstruction)?.registerA
        ?: throw PatchException("Pure3D: asKeyable return reg not found")
    m.addInstructions(
        retIdx,
        """
        invoke-virtual {v$retReg, v$vH0}, $KTF->setRotationX(F)V
        invoke-virtual {v$retReg, v$vH1}, $KTF->setRotationY(F)V
        """.trimIndent(),
    )
}

/**
 * valueAtTime (both overloads): Keyable evaluation + setters post-ctor.
 */
private fun BytecodePatchContext.hookValueAtTime() {
    val targets = requireClass(KTF).methods.filter { it.name == "valueAtTime" }
    if (targets.isEmpty()) throw PatchException("Pure3D: no valueAtTime in KeyableTransform")
    for (m in targets) {
        val insns = insnsOf(m)
        var vTime = -1
        var vK = -1
        var vNew = -1
        var ctorIdx = -1
        for (i in insns.indices) {
            val ref = methodRefOf(insns[i])
            if (ref != null && ref.name == "valueAtTime" && ref.definingClass.contains("KeyableKt")) {
                val r = regsOf(insns[i])
                if (r.size >= 2) vTime = r[1]
            }
            if (insns[i].opcode == Opcode.IGET_OBJECT) {
                val fref = (insns[i] as? ReferenceInstruction)?.reference as? FieldReference
                if (fref != null && fref.name == "rotation" && fref.definingClass == KTF) {
                    vK = (insns[i] as? TwoRegisterInstruction)?.registerB ?: -1
                }
            }
            if (insns[i].opcode == Opcode.NEW_INSTANCE) {
                val t = ((insns[i] as? ReferenceInstruction)?.reference as? TypeReference)?.type ?: ""
                if (t.contains("scene/Transform;") && !t.contains("Keyable")) {
                    vNew = (insns[i] as? OneRegisterInstruction)?.registerA ?: -1
                }
            }
            if (vNew >= 0 && ctorIdx < 0) {
                if (ref != null && ref.name == "<init>" && ref.definingClass == TF) ctorIdx = i
            }
        }
        if (vTime < 0 || vK < 0 || vNew < 0 || ctorIdx < 0) {
            throw PatchException("Pure3D: valueAtTime regs not found (t=$vTime,k=$vK,n=$vNew,c=$ctorIdx)")
        }
        val dead = findDeadRegs(m, ctorIdx + 1, 1, setOf(vNew, vK, vTime))
        val vS = dead[0]
        val (prolog, mm) = lowCopies(m, ctorIdx + 1, listOf(vNew to true, vK to true, vTime to false))
        val (vNN, vKK, vTT) = mm
        m.addInstructions(
            ctorIdx + 1,
            prolog + """
            iget-object v$vS, v$vKK, $KTF->rotationXKeyable:$KEYABLE
            invoke-static {v$vS, v$vTT}, Lcom/alightcreative/app/motion/scene/KeyableKt;->valueAtTime(Lcom/alightcreative/app/motion/scene/Keyable;F)Ljava/lang/Object;
            move-result-object v$vS
            check-cast v$vS, Ljava/lang/Number;
            invoke-virtual {v$vS}, Ljava/lang/Number;->floatValue()F
            move-result v$vS
            invoke-virtual {v$vNN, v$vS}, $TF->setRotationX(F)V
            iget-object v$vS, v$vKK, $KTF->rotationYKeyable:$KEYABLE
            invoke-static {v$vS, v$vTT}, Lcom/alightcreative/app/motion/scene/KeyableKt;->valueAtTime(Lcom/alightcreative/app/motion/scene/Keyable;F)Ljava/lang/Object;
            move-result-object v$vS
            check-cast v$vS, Ljava/lang/Number;
            invoke-virtual {v$vS}, Ljava/lang/Number;->floatValue()F
            move-result v$vS
            invoke-virtual {v$vNN, v$vS}, $TF->setRotationY(F)V
            """.trimIndent(),
        )
    }
}
