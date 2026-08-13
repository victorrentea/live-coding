package com.github.victorrentea.livecoding.tokens

import com.knuddels.jtokkit.api.EncodingType

/**
 * The LLM families we can report a token count for.
 *
 * OpenAI publishes its BPE vocabularies (bundled offline via JTokkit), so those counts are exact.
 * Anthropic and Google do not publish theirs, so those families reuse the closest public vocabulary
 * and apply a calibration factor - the count is shown prefixed with `~` to say so.
 */
enum class TokenizerFamily(
    val displayName: String,
    val shortLabel: String,
    val encodingType: EncodingType,
    val calibration: Double = 1.0,
    val note: String,
) {
    GPT("GPT-5 / GPT-4o", "GPT", EncodingType.O200K_BASE, note = "Exact - OpenAI o200k_base vocabulary"),
    GPT_LEGACY("GPT-4 / GPT-3.5", "GPT-4", EncodingType.CL100K_BASE, note = "Exact - OpenAI cl100k_base vocabulary"),
    CLAUDE("Claude", "Claude", EncodingType.CL100K_BASE, 1.15, "Estimate - Anthropic's vocabulary is not public (cl100k + 15%)"),
    GEMINI("Gemini", "Gemini", EncodingType.CL100K_BASE, 1.05, "Estimate - Google's vocabulary is not public (cl100k + 5%)"),
    ;

    /** True when the count is calibrated rather than computed with the model's own vocabulary. */
    val approximate: Boolean get() = calibration != 1.0
}
