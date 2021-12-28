package com.github.victorrentea.livecoding

import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil


fun PsiModifierList.hasAnyAnnotation(vararg annotationFqns: String) =
    annotationFqns.any { hasAnnotation(it) }

fun PsiReferenceExpression.isWrite() =
    (parent as? PsiAssignmentExpression)?.lExpression == this
        || (parent as? PsiLocalVariable)?.hasInitializer() == true

fun PsiReferenceExpression.isRead(): Boolean {
    val parent = this.parent
    if (parent is PsiLocalVariable) return false
    if (parent is PsiAssignmentExpression && parent.lExpression == this)
        return parent.operationSign.tokenType != JavaTokenType.EQ
    return true
}

