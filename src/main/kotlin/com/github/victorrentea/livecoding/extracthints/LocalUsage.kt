package com.github.victorrentea.livecoding.extracthints

import com.intellij.psi.PsiVariable

data class LocalUsage(val lineNumber: Int, val variable: PsiVariable, val access: LocalUsageType) {
    override fun toString() = "$lineNumber:" +
            (if (access == LocalUsageType.READ) "R" else "W") +
            "(" + variable.name + ")"
}