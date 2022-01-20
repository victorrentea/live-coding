package com.github.victorrentea.livecoding.complexity

import com.github.victorrentea.livecoding.getLineNumber
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.PsiErrorElementUtil
import org.jetbrains.uast.util.classSetOf
import org.jetbrains.uast.util.isInstanceOf

class CognitiveComplexityVisitor {
    companion object {
        // B1: There is an increment for each of the following:
        //● if, else if, else, ternary operator
        //● switch
        //● for, foreach
        //● while, do while
        //● catch

        //● goto LABEL, break LABEL, continue LABEL, break NUMBER, continue NUMBER - ignored
        //● sequences of binary logical operators
        //● each method in a recursion cycle
        val incrementElementTypes = classSetOf(
            PsiIfStatement::class.java,
            PsiConditionalExpression::class.java,
            PsiSwitchStatement::class.java,
            PsiForStatement::class.java,
            PsiForeachStatement::class.java,
            PsiWhileStatement::class.java,
            PsiDoWhileStatement::class.java,
            PsiCatchSection::class.java,
        )

        // B2. Nesting level: The following structures increment the nesting level:
        //● if, else if, else, ternary operator
        //● switch
        //● for, foreach
        //● while, do while
        //● catch
        //● nested methods and method-like structures such as lambdas
        val nestingElementsTypes = classSetOf(
            PsiIfStatement::class.java,
            PsiConditionalExpression::class.java,
            PsiSwitchStatement::class.java,
            PsiForStatement::class.java,
            PsiForeachStatement::class.java,
            PsiWhileStatement::class.java,
            PsiDoWhileStatement::class.java,
            PsiCatchSection::class.java,
            PsiLambdaExpression::class.java
        )

        //B3. Nesting increments:
        // The following structures receive a nesting increment commensurate with their nested depth
        // inside B2 structures:
        //● if, ternary operator
        //● switch
        //● for, foreach
        //● while, do while
        //● catch
        val incrementWhenNestedType = classSetOf(
            PsiIfStatement::class.java,
            PsiConditionalExpression::class.java,
            PsiSwitchStatement::class.java,
            PsiForStatement::class.java,
            PsiForeachStatement::class.java,
            PsiWhileStatement::class.java,
            PsiDoWhileStatement::class.java,
            PsiCatchSection::class.java,
        )
    }

    val complexityMap = mutableMapOf<PsiElement, CognitiveComplexityInContext>()

    fun visitElement(element: PsiElement, nestingLevel: Int = 0): CognitiveComplexity {
        if (PsiErrorElementUtil.hasErrors(element.project, element.containingFile.virtualFile)) return CognitiveComplexity.ZERO
        // fundamental increment (B1)
        val ownCost = when (element) {
            is PsiMethodCallExpression -> if (isRecursiveCall(element)) 1 else 0
            is PsiBinaryExpression -> if (element.parent is PsiBinaryExpression) 0 else complexityOfBooleanExpression(element)
            else -> if (element.isInstanceOf(incrementElementTypes)) 1 else 0
        }

        // increment nesting (B3)
        val addNestingCost = element.isInstanceOf(incrementWhenNestedType)
        val nestingCost = if (addNestingCost) nestingLevel else 0

//        println("Visit $element")

        if (ownCost + nestingCost > 0) {
//            log.debug("Add $ownCost + $nestingCost(nesting) for $element at line " + element.getLineNumber())
        }

        val ownComplexity = CognitiveComplexity(nestingCost, if (addNestingCost) 1 else 0, ownCost)

        // nested increment (B2)
        val increasesNesting = element.isInstanceOf(nestingElementsTypes)

        val newNestingLevel = nestingLevel + if (increasesNesting) 1 else 0

        val childrenCosts = element.children.map { visitElement(it, newNestingLevel) }


        val retainedCost = childrenCosts.fold(ownComplexity) { acc, cc -> acc + cc}

        val contextOverheadCost = retainedCost.nestingCount * nestingLevel

        complexityMap[element] = CognitiveComplexityInContext(retainedCost.total(), retainedCost.total() - contextOverheadCost)
        return retainedCost
    }

    private fun isRecursiveCall(element: PsiMethodCallExpression) =
        element.methodExpression.resolve() == PsiTreeUtil.getParentOfType(element, PsiMethod::class.java)

    private fun complexityOfBooleanExpression(element: PsiBinaryExpression): Int {
        val booleanOperators = PsiTreeUtil.findChildrenOfType(element, PsiJavaToken::class.java)
            .map { it.tokenType }
            .filter {
                when (it) {
                    JavaTokenType.ANDAND, JavaTokenType.OROR -> true
                    else -> false
                }
            }
        if (booleanOperators.isEmpty()) return 0
        val firstOperator = JavaTokenType.ASTERISK // impossible to match below
        return booleanOperators
            .fold(0 to firstOperator) { (acc, lastVal), elem -> (acc + if (elem != lastVal) 1 else 0) to elem }
            .first

    }
}