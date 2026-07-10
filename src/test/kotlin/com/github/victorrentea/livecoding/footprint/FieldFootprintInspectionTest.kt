package com.github.victorrentea.livecoding.footprint

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase

/**
 * The cheap on-the-fly inspection must fire on a fat parameter used through a few getters and
 * NOT propagated, and stay silent the moment the parameter escapes or reaches a whole-object
 * sink (those cases can't be judged intra-procedurally, so no highlight).
 */
class FieldFootprintInspectionTest : LightJavaCodeInsightFixtureTestCase() {

    override fun setUp() {
        super.setUp()
        FootprintTargets.minInstanceFields = 3
        myFixture.enableInspections(FieldFootprintInspection())
        myFixture.addClass(
            """
            package com.acme;
            public class Fat {
                private int a, b, c, d, e;
                public int getA() { return a; }
                public int getB() { return b; }
                public int getC() { return c; }
                public String toXML() { return null; }
            }
            """.trimIndent(),
        )
    }

    override fun tearDown() {
        FootprintTargets.minInstanceFields = 8
        super.tearDown()
    }

    private fun footprintWarnings(source: String): List<String> {
        myFixture.configureByText(JavaFileType.INSTANCE, source)
        return myFixture.doHighlighting()
            .mapNotNull { it.description }
            .filter { it.contains("thin-DTO candidate") }
    }

    fun testThinLeafParameterIsHighlighted() {
        val warnings = footprintWarnings(
            """
            import com.acme.Fat;
            class C { void m(Fat f) { int x = f.getA(); int y = f.getB(); } }
            """.trimIndent(),
        )
        assertSize(1, warnings)
    }

    fun testParameterPassedOnwardIsNotHighlighted() {
        val warnings = footprintWarnings(
            """
            import com.acme.Fat;
            class C {
                void m(Fat f) { sink(f); }
                void sink(Fat x) { }
            }
            """.trimIndent(),
        )
        assertEmpty(warnings)
    }

    fun testWholeObjectSinkParameterIsNotHighlighted() {
        val warnings = footprintWarnings(
            """
            import com.acme.Fat;
            class C { String m(Fat f) { return f.toXML(); } }
            """.trimIndent(),
        )
        assertEmpty(warnings)
    }
}
