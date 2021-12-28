package com.github.victorrentea.livecoding

import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
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

        // B2. Nesting level
        //The following structures increment the nesting level:
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

        //B3. Nesting increments
        //The following structures receive a nesting increment commensurate with their nested depth
        //inside B2 structures:
        //● if, ternary operator
        //● switch
        //● for, foreach
        //● while, do while
        //● catch
        val nestingIncrements = classSetOf(
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

    var nestingLevel: Int = 0
    var complexity: Int = 0

    fun visitElement(element: PsiElement) {
        // increments B1
        val increment = when (element) {
            is PsiMethodCallExpression -> if (isRecursiveCall(element)) 1 else 0
            is PsiBinaryExpression -> {
                if (element.parent is PsiBinaryExpression) 0
                else complexityOfBooleanExpression(element)
            }
            else -> if (element.isInstanceOf(incrementElementTypes)) 1 else 0
        }

        // nested increment B3
        val nestingIncrement = if (element.isInstanceOf(nestingIncrements)) nestingLevel else 0

//        println("Visit $element")
        if (increment + nestingIncrement > 0)
            println("Add $increment + $nestingIncrement(nesting) for $element at line " + element.getLineNumber())

        complexity += increment + nestingIncrement

        // nesting B2
        val nesting = element.isInstanceOf(nestingElementsTypes)

        if (nesting) nestingLevel++

        element.children.forEach { visitElement(it) }

        if (nesting) nestingLevel--
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