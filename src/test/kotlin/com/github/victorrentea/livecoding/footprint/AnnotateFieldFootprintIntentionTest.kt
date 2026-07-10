package com.github.victorrentea.livecoding.footprint

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase

/**
 * Drives the real [AnnotateFieldFootprintIntention] end-to-end on inline sources, so getter
 * collection, chained-path building, whole-object sink detection, setter (write) tracking, and
 * transitive propagation are all exercised headlessly. Fixtures use neutral classes to confirm
 * the analysis is general-purpose, not tied to any specific project.
 */
class AnnotateFieldFootprintIntentionTest : LightJavaCodeInsightFixtureTestCase() {

    private val intentionText = "Annotate field footprint in Javadoc"

    override fun setUp() {
        super.setUp()
        // Treat every fixture class as a fat object regardless of field count.
        FootprintTargets.minInstanceFields = 0
        myFixture.addClass(
            """
            package com.acme;
            public class Address { public String getCity() { return null; } }
            """.trimIndent(),
        )
        myFixture.addClass(
            """
            package com.acme;
            public class Order {
                public String getId() { return null; }
                public String getTotal() { return null; }
                public Address getAddress() { return null; }
                public void setStatus(String s) { }
                public String toXML() { return null; }
            }
            """.trimIndent(),
        )
    }

    override fun tearDown() {
        FootprintTargets.minInstanceFields = 8
        super.tearDown()
    }

    private fun annotate(source: String): String {
        myFixture.configureByText(JavaFileType.INSTANCE, source)
        myFixture.launchAction(myFixture.findSingleIntention(intentionText))
        return myFixture.file.text
    }

    fun testDirectAndChainedGettersBecomeSortedDottedPaths() {
        val result = annotate(
            """
            import com.acme.Order;
            class Consumer {
                void handle(Order o) {
                    <caret>String id = o.getId();
                    String c = o.getAddress().getCity();
                }
            }
            """.trimIndent(),
        )
        assertTrue(result, result.contains("@param o reads {address.city, id}"))
    }

    fun testWholeObjectSinkIsAnnotatedAll() {
        val result = annotate(
            """
            import com.acme.Order;
            class Consumer {
                String dump(Order o) {
                    <caret>return o.toXML();
                }
            }
            """.trimIndent(),
        )
        assertTrue(result, result.contains("@param o reads {ALL}"))
    }

    fun testFootprintPropagatesTransitivelyThroughCallee() {
        val result = annotate(
            """
            import com.acme.Order;
            class Consumer {
                void entry(Order o) {
                    <caret>helper(o);
                }
                private void helper(Order x) {
                    String id = x.getId();
                }
            }
            """.trimIndent(),
        )
        assertTrue(result, result.contains("@param o reads {id}"))
    }

    fun testSetterCallsRecordedAsWrites() {
        val result = annotate(
            """
            import com.acme.Order;
            class Consumer {
                void handle(Order o) {
                    <caret>if (o.getId() != null) {
                        o.setStatus("x");
                        String t = o.getTotal();
                    }
                }
            }
            """.trimIndent(),
        )
        assertTrue(result, result.contains("@param o reads {id, total} writes {status}"))
    }
}
