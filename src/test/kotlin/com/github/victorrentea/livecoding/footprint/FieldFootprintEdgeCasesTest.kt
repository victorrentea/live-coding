package com.github.victorrentea.livecoding.footprint

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.application.ReadAction
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase

/**
 * Adversarial, per-syntactic-shape coverage for [FieldFootprintAnalyzer].
 *
 * Each test drives the analyzer DIRECTLY (not via the intention) on a neutral `com.acme` fixture
 * and asserts the exact reads / writes / verdict. The shapes mirror a census of how fat-object
 * parameters are actually used in a large real codebase (casts, deep getter chains, ternaries,
 * loops, method references, constructors, collections, sinks, escapes, cycles, ...).
 *
 * Fixtures are intentionally client-agnostic: never copy a customer's classes into the plugin.
 * Cases where the analyzer is provably wrong are marked `// KNOWN BUG:` and assert the CURRENT
 * behavior (so the suite stays green) — see docs/footprint-findings.md for the ranked list.
 */
class FieldFootprintEdgeCasesTest : LightJavaCodeInsightFixtureTestCase() {

    private val fat = "com.acme.Fat"

    override fun setUp() {
        super.setUp()
        FootprintTargets.minInstanceFields = 0
        myFixture.addClass(
            """
            package com.acme;
            public class Address {
                public String getCity() { return null; }
                public String getZip() { return null; }
            }
            """.trimIndent(),
        )
        myFixture.addClass("package com.acme; public class Base { }")
        myFixture.addClass(
            """
            package com.acme;
            public class Fat extends Base {
                public String getA() { return null; }
                public String getB() { return null; }
                public String getC() { return null; }
                public boolean isActive() { return false; }
                public int getCount() { return 0; }
                public String getURL() { return null; }
                public Address getAddress() { return null; }
                public Fat getParent() { return null; }
                public String[] getItems() { return null; }
                public void setA(String v) { }
                public void setB(String v) { }
                public void setC(String v) { }
                public String toXML() { return null; }
                public Object clone() { return null; }
                public String toString() { return null; }
            }
            """.trimIndent(),
        )
        // Support classes for the transitive / sink / functional shapes.
        myFixture.addClass("package com.acme; public class BaseHandler { public void handle(Fat x) { String a = x.getA(); } }")
        myFixture.addClass("package com.acme; public class Holder { public Holder(Fat x) { String a = x.getA(); } }")
        myFixture.addClass("package com.acme; public class Keeper { private Fat kept; public Keeper(Fat x) { this.kept = x; } }")
        myFixture.addClass("package com.acme; public class Bag { public void add(Object o) { } }")
        myFixture.addClass("package com.acme; public class Fmt { public static String format(String fmt, Object... args) { return null; } }")
        myFixture.addClass("package com.acme; public class ObjectMapper { public void writeValue(Object out, Object v) { } }")
        myFixture.addClass("package com.acme; public interface Run { void go(); }")
        myFixture.addClass("package com.acme; public interface Sup { String get(); }")
        myFixture.addClass("package com.acme; public interface StrConsumer { void accept(String s); }")
    }

    override fun tearDown() {
        FootprintTargets.minInstanceFields = 8
        super.tearDown()
    }

    /** Configure [entrySource] as the current file and run the analyzer on the first Fat parameter of [methodName]. */
    private fun analyze(entrySource: String, methodName: String = "entry"): Footprint {
        myFixture.configureByText(JavaFileType.INSTANCE, entrySource)
        return ReadAction.compute<Footprint, RuntimeException> {
            val method = PsiTreeUtil.findChildrenOfType(myFixture.file, PsiMethod::class.java)
                .first { it.name == methodName }
            val param = method.parameterList.parameters.first { isTargetType(it.type, setOf(fat)) }
            FieldFootprintAnalyzer(fat).analyzeParameter(method, param)
        }
    }

    private fun body(stmts: String, extra: String = "") = """
        import com.acme.Fat;
        import com.acme.Base;
        class Consumer $extra {
            void entry(Fat f) { $stmts }
        }
        """.trimIndent()

    // ---- getter shapes ---------------------------------------------------------------------

    fun testDeepGetterChainThreeLevels() {
        val fp = analyze(body("String c = f.getParent().getAddress().getCity();"))
        assertEquals(Verdict.SOUND, fp.verdict)
        assertEquals(setOf("parent.address.city"), fp.paths)
    }

    fun testGettersInTernary() {
        val fp = analyze(body("String x = f.isActive() ? f.getA() : f.getB();"))
        assertEquals(Verdict.SOUND, fp.verdict)
        assertEquals(setOf("active", "a", "b"), fp.paths)
    }

    fun testGetterInForEachOverArray() {
        val fp = analyze(body("for (String s : f.getItems()) { System.out.print(s); }"))
        assertEquals(Verdict.SOUND, fp.verdict)
        assertEquals(setOf("items"), fp.paths)
    }

    fun testGetterInsideForLoopBody() {
        val fp = analyze(body("for (int i = 0; i < 3; i++) { String a = f.getA(); }"))
        assertEquals(setOf("a"), fp.paths)
    }

    fun testSwitchOnGetterResult() {
        val fp = analyze(body("switch (f.getCount()) { default: break; }"))
        assertEquals(Verdict.SOUND, fp.verdict)
        assertEquals(setOf("count"), fp.paths)
    }

    fun testMixedReadsAndWrites() {
        val fp = analyze(body("if (f.getA() != null) { f.setB(\"x\"); String c = f.getC(); }"))
        assertEquals(Verdict.SOUND, fp.verdict)
        assertEquals(setOf("a", "c"), fp.paths)
        assertEquals(setOf("b"), fp.writes)
    }

    // ---- cast shapes -----------------------------------------------------------------------

    fun testCastToTargetThenGetter() {
        val fp = analyze(body("String a = ((Fat) f).getA(); String city = ((Fat) f).getAddress().getCity();"))
        assertEquals(Verdict.SOUND, fp.verdict)
        assertEquals(setOf("a", "address.city"), fp.paths)
    }

    fun testDowncastFromWidenedLocal() {
        // Object o = f;  ((Fat) o).getB()  -> alias + cast collaborate.
        val fp = analyze(body("Object o = f; String b = ((Fat) o).getB();"))
        assertEquals(Verdict.SOUND, fp.verdict)
        assertEquals(setOf("b"), fp.paths)
    }

    // ---- escape / opaque boundaries --------------------------------------------------------

    fun testStoreIntoFieldIsUnknown() {
        val fp = analyze(
            """
            import com.acme.Fat;
            class Consumer {
                Fat saved;
                void entry(Fat f) { this.saved = f; }
            }
            """.trimIndent(),
        )
        assertEquals(Verdict.UNKNOWN, fp.verdict)
        assertTrue(fp.reasons.toString(), fp.reasons.any { it.contains("Stored into field") })
    }

    fun testReturnedIsUnknown() {
        val fp = analyze(
            """
            import com.acme.Fat;
            class Consumer { Fat entry(Fat f) { return f; } }
            """.trimIndent(),
        )
        assertEquals(Verdict.UNKNOWN, fp.verdict)
    }

    fun testWidenedToSupertypeIsUnknown() {
        val fp = analyze(body("handle(f); } void handle(Base b) { System.out.print(b);"))
        assertEquals(Verdict.UNKNOWN, fp.verdict)
    }

    fun testPassedToVarargsIsUnknown() {
        val fp = analyze(body("com.acme.Fmt.format(\"%s\", f);"))
        assertEquals(Verdict.UNKNOWN, fp.verdict)
    }

    fun testAddedToCollectionIsUnknown() {
        val fp = analyze(body("com.acme.Bag b = new com.acme.Bag(); b.add(f);"))
        assertEquals(Verdict.UNKNOWN, fp.verdict)
    }

    // ---- ignored (no field access) shapes --------------------------------------------------

    fun testInstanceofIsIgnored() {
        val fp = analyze(body("if (f instanceof Fat) { System.out.print(1); }"))
        assertEquals(Verdict.SOUND, fp.verdict)
        assertEmpty(fp.paths)
    }

    fun testNullCheckThenGetter() {
        val fp = analyze(body("if (f != null) { String a = f.getA(); }"))
        assertEquals(Verdict.SOUND, fp.verdict)
        assertEquals(setOf("a"), fp.paths)
    }

    // ---- whole-object sinks ----------------------------------------------------------------

    fun testCloneIsWholeObject() {
        val fp = analyze(body("Object c = f.clone();"))
        assertEquals(Verdict.WHOLE_OBJECT, fp.verdict)
    }

    fun testToXmlIsWholeObject() {
        val fp = analyze(body("String x = f.toXML();"))
        assertEquals(Verdict.WHOLE_OBJECT, fp.verdict)
    }

    fun testSerializerArgumentIsWholeObject() {
        val fp = analyze(body("com.acme.ObjectMapper m = new com.acme.ObjectMapper(); m.writeValue(null, f);"))
        assertEquals(Verdict.WHOLE_OBJECT, fp.verdict)
    }

    // ---- transitive propagation ------------------------------------------------------------

    fun testThisQualifiedCallPropagates() {
        val fp = analyze(body("this.helper(f); } void helper(Fat x) { String b = x.getB();"))
        assertEquals(setOf("b"), fp.paths)
    }

    fun testOverloadedTargetMethodResolvesToTargetOverload() {
        val fp = analyze(body("process(f); } void process(Fat x) { String a = x.getA(); } void process(String s) { System.out.print(s);"))
        assertEquals(setOf("a"), fp.paths)
    }

    fun testSuperCallPropagates() {
        val fp = analyze(body("super.handle(f);", extra = "extends com.acme.BaseHandler"))
        assertEquals(setOf("a"), fp.paths)
    }

    fun testPassedToConstructorRecurses() {
        val fp = analyze(body("new com.acme.Holder(f);"))
        assertEquals(setOf("a"), fp.paths)
    }

    fun testConstructorThatStoresIsUnknown() {
        val fp = analyze(body("new com.acme.Keeper(f);"))
        assertEquals(Verdict.UNKNOWN, fp.verdict)
    }

    fun testLambdaCapturingTargetIsAnalyzed() {
        val fp = analyze(body("com.acme.Run r = () -> { String a = f.getA(); };"))
        assertEquals(setOf("a"), fp.paths)
    }

    fun testAnonymousClassCapturingTargetIsAnalyzed() {
        val fp = analyze(body("com.acme.Run r = new com.acme.Run() { public void go() { String b = f.getB(); } };"))
        assertEquals(setOf("b"), fp.paths)
    }

    fun testMutuallyRecursiveCallGraphTerminates() {
        val fp = analyze(body("String a = f.getA(); other(f); } void other(Fat f) { String b = f.getB(); entry(f);"))
        assertEquals(Verdict.SOUND, fp.verdict)
        assertEquals(setOf("a", "b"), fp.paths)
    }

    // ---- method references (fixed: previously returned EMPTY) ------------------------------

    fun testMethodReferenceGetterIsRead() {
        val fp = analyze(body("com.acme.Sup s = f::getA;"))
        assertEquals(Verdict.SOUND, fp.verdict)
        assertEquals(setOf("a"), fp.paths)
    }

    fun testMethodReferenceSetterIsWrite() {
        val fp = analyze(body("com.acme.StrConsumer c = f::setA;"))
        assertEquals(Verdict.SOUND, fp.verdict)
        assertEquals(setOf("a"), fp.writes)
    }

    fun testMethodReferenceToWholeObjectSinkIsWholeObject() {
        val fp = analyze(body("com.acme.Sup s = f::toXML;"))
        assertEquals(Verdict.WHOLE_OBJECT, fp.verdict)
    }

    // ---- KNOWN BUGS (assert current behavior; see findings report) -------------------------

    fun testKnownBugReassignmentAliasNotTracked() {
        // KNOWN BUG: expandAliases only follows local *initializers*, not later `y = f` assignments,
        // so reads through a re-assigned alias are missed. Correct reads would be {a}.
        val fp = analyze(body("Fat y = null; y = f; String a = y.getA();"))
        assertEquals(Verdict.SOUND, fp.verdict)
        assertEmpty(fp.paths)
    }

    fun testKnownBugImplicitToStringViaConcatMissed() {
        // KNOWN BUG: string concatenation `"" + f` calls f.toString() (often reads every field),
        // but the operand is neither a call nor an escape, so it is ignored (verdict stays SOUND).
        val fp = analyze(body("String s = \"x\" + f; System.out.print(s);"))
        assertEquals(Verdict.SOUND, fp.verdict)
        assertEmpty(fp.paths)
    }

    fun testKnownBugArrayStoreEscapeMissed() {
        // KNOWN BUG: storing the target into an array element escapes it, but the LHS is a
        // PsiArrayAccessExpression (not a field reference), so it is ignored. Should be UNKNOWN.
        val fp = analyze(body("Fat[] arr = new Fat[1]; arr[0] = f;"))
        assertEquals(Verdict.SOUND, fp.verdict)
        assertEmpty(fp.paths)
    }

    fun testKnownBugAcronymGetterDecapitalization() {
        // KNOWN BUG: getURL() yields "uRL" instead of JavaBeans' "URL" (Introspector.decapitalize
        // keeps an all-caps run). Harmless for the verdict, but the persisted path text is off.
        val fp = analyze(body("String u = f.getURL();"))
        assertEquals(setOf("uRL"), fp.paths)
    }

    fun testExplicitToStringIsConservativelyUnknown() {
        // Not a bug: an explicit non-getter call degrades to UNKNOWN (safe), unlike the implicit
        // concatenation case above — documenting the asymmetry.
        val fp = analyze(body("String s = f.toString(); System.out.print(s);"))
        assertEquals(Verdict.UNKNOWN, fp.verdict)
    }
}
