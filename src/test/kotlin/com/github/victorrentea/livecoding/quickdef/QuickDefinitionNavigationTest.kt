package com.github.victorrentea.livecoding.quickdef

import com.github.victorrentea.livecoding.ux.quickdef.FragmentLocator
import com.github.victorrentea.livecoding.ux.quickdef.QuickDefinitionNavigation
import com.intellij.codeInsight.hint.ImplementationViewComponent
import com.intellij.codeInsight.hint.PsiImplementationViewElement
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.EditorKind
import com.intellij.psi.PsiMethod
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import org.jetbrains.concurrency.Promise
import javax.swing.SwingUtilities

class QuickDefinitionNavigationTest : LightJavaCodeInsightFixtureTestCase() {

    private val source = """
        package com.acme;
        class Order {
            void place() {
                validate();
                new Invoice().issue();
            }
            void validate() {
                total();
            }
            int total() { return 1; }
        }
        class Invoice {
            void issue() {}
        }
    """.trimIndent()

    private lateinit var view: ImplementationViewComponent
    private lateinit var popup: Editor
    private lateinit var navigation: QuickDefinitionNavigation

    override fun setUp() {
        super.setUp()
        myFixture.configureByText("Order.java", source)
        val place = method("Order", "place")
        val element = ApplicationManager.getApplication().executeOnPooledThread<PsiImplementationViewElement> {
            ReadAction.compute<PsiImplementationViewElement, RuntimeException> { PsiImplementationViewElement(place) }
        }.get() // the platform refuses to build it on the EDT
        view = ImplementationViewComponent(listOf(element), 0)
        popup = EditorFactory.getInstance().allEditors.single {
            it.editorKind == EditorKind.PREVIEW && SwingUtilities.isDescendingFrom(it.component, view)
        }
        navigation = QuickDefinitionNavigation(popup, project)
    }

    override fun tearDown() {
        try {
            if (::view.isInitialized) view.cleanup() // releases the popup's editor
        } finally {
            super.tearDown()
        }
    }

    fun testPopupShowsACopyOfTheDefinition() {
        assertTrue(popup.document.text.contains("validate();"))
        assertTrue(source.contains(popup.document.text))
    }

    fun testTokenInPopupResolvesInTheSourceFile() {
        assertEquals(method("Order", "validate"), await(navigation.targetsAt(popupOffsetOf("validate();"))).single())
    }

    fun testTokenOfAnotherClassResolves() {
        assertEquals(method("Invoice", "issue"), await(navigation.targetsAt(popupOffsetOf("issue()"))).single())
    }

    fun testDrillInShowsTheDefinitionInTheSamePopupAndBackReturns() {
        assertTrue(await(navigation.drillIn(popupOffsetOf("validate();"))))
        assertEquals(method("Order", "validate"), (view.elements.single() as PsiImplementationViewElement).getPsiElement())

        // ...and again, from inside the new fragment
        assertTrue(await(navigation.drillIn(popupOffsetOf("total();"))))
        assertEquals(method("Order", "total"), (view.elements.single() as PsiImplementationViewElement).getPsiElement())

        assertTrue(navigation.back())
        assertEquals(method("Order", "validate"), (view.elements.single() as PsiImplementationViewElement).getPsiElement())
        assertTrue(navigation.back())
        assertEquals(method("Order", "place"), (view.elements.single() as PsiImplementationViewElement).getPsiElement())
        assertFalse(navigation.back())
    }

    fun testWhitespaceResolvesToNothing() {
        assertEmpty(await(navigation.targetsAt(0)))
        assertFalse(await(navigation.drillIn(0)))
    }

    fun testLocatorPrefersTheHintedOccurrence() {
        val text = "a();\nb();\na();\n"
        assertEquals(10, FragmentLocator.locate(text, "a();", 10))
        assertEquals(10, FragmentLocator.locate(text, "a();", 9))
        assertEquals(0, FragmentLocator.locate(text, "a();", 0))
        assertEquals(-1, FragmentLocator.locate(text, "c();", 0))
    }

    private fun <T> await(promise: Promise<T>?): T = PlatformTestUtil.waitForPromise(promise!!)!!

    private fun popupOffsetOf(snippet: String): Int {
        val at = popup.document.text.indexOf(snippet)
        assertTrue("'$snippet' not in popup: ${popup.document.text}", at >= 0)
        return at + 1
    }

    private fun method(className: String, name: String): PsiMethod =
        myFixture.findClass("com.acme.$className").findMethodsByName(name, false).single()
}
