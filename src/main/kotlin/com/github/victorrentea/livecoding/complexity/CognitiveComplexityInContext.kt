package com.github.victorrentea.livecoding.complexity

data class CognitiveComplexityInContext(val costInContext:Int, val costIfExtracted:Int) {
    companion object {
        val ZERO = CognitiveComplexityInContext(0, 0)
    }
    operator fun plus(other: CognitiveComplexityInContext) = CognitiveComplexityInContext(
        costInContext + other.costInContext, costIfExtracted + other.costIfExtracted
    )
}