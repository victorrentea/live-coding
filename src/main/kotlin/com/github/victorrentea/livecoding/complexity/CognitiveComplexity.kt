package com.github.victorrentea.livecoding.complexity

data class CognitiveComplexity(val nestingCost:Int, val nestingCount:Int, val ownCost:Int) {
    companion object {
        val ZERO = CognitiveComplexity(0, 0, 0)
    }
    operator fun plus(other: CognitiveComplexity) =
        CognitiveComplexity(nestingCost + other.nestingCost, nestingCount + other.nestingCount, ownCost + other.ownCost)
    fun total() = nestingCost + ownCost
}