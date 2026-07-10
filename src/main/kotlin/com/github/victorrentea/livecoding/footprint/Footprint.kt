package com.github.victorrentea.livecoding.footprint

/**
 * Verdict for a method's usage of the target ("god object") parameter.
 *
 * - [SOUND]        : every access was resolved; [Footprint.paths] is the complete set of
 *                    getter paths read across the call graph -> safe thin-DTO candidate.
 * - [WHOLE_OBJECT] : the object reaches a sink that reads all fields (serialization,
 *                    a script engine, whole-object marshalling) -> cannot be thinned.
 * - [UNKNOWN]      : analysis hit an opaque boundary (no source, widened to a supertype,
 *                    stored in a field, returned) -> footprint cannot be trusted.
 */
enum class Verdict { SOUND, WHOLE_OBJECT, UNKNOWN }

/**
 * @param paths   dotted getter paths READ, e.g. "patient.patientID", "requestDiagnosisList".
 * @param writes  fields WRITTEN via setters, e.g. "comments" (side effects a thin DTO must keep).
 * @param reasons human-readable explanations for a non-SOUND verdict.
 */
data class Footprint(
    val verdict: Verdict,
    val paths: Set<String>,
    val reasons: Set<String> = emptySet(),
    val writes: Set<String> = emptySet(),
) {
    companion object {
        val EMPTY = Footprint(Verdict.SOUND, emptySet())
        fun sound(paths: Set<String>) = Footprint(Verdict.SOUND, paths)
        fun written(field: String) = Footprint(Verdict.SOUND, emptySet(), emptySet(), setOf(field))
        fun wholeObject(reason: String) = Footprint(Verdict.WHOLE_OBJECT, emptySet(), setOf(reason))
        fun unknown(reason: String) = Footprint(Verdict.UNKNOWN, emptySet(), setOf(reason))
    }
}

/**
 * Least-upper-bound merge. WHOLE_OBJECT dominates UNKNOWN dominates SOUND, because a
 * single whole-object sink anywhere in the graph already forces every field to stay.
 * Reads, writes and reasons are always unioned so the report can still show partial info.
 */
fun merge(a: Footprint, b: Footprint): Footprint {
    val verdict = when {
        a.verdict == Verdict.WHOLE_OBJECT || b.verdict == Verdict.WHOLE_OBJECT -> Verdict.WHOLE_OBJECT
        a.verdict == Verdict.UNKNOWN || b.verdict == Verdict.UNKNOWN -> Verdict.UNKNOWN
        else -> Verdict.SOUND
    }
    return Footprint(verdict, a.paths + b.paths, a.reasons + b.reasons, a.writes + b.writes)
}
