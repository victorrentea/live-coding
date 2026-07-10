package com.github.victorrentea.livecoding.footprint

import com.intellij.openapi.application.ReadAction
import com.intellij.psi.PsiClass
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase

/**
 * Micro-benchmark for [FieldFootprintAnalyzer] on synthetic, transitive call graphs.
 *
 * Answers: "can the transitive analysis run as an on-the-fly inspection, and how expensive is a
 * deep, cold call graph?" It generates fat targets with N getters and a chain of M methods that
 * each read all N getters then propagate the object onward, plus a wide (branching) and a cyclic
 * variant. It prints a warm-median table in ms; it deliberately does NOT assert on timing
 * (the printed table IS the deliverable — see docs/footprint-findings.md).
 *
 * The analyzer's default maxDepth is 15; here we raise it to `depth+2` so the FULL chain is
 * traversed and the numbers reflect the true cost of a graph of that depth.
 */
class FieldFootprintBenchmarkTest : LightJavaCodeInsightFixtureTestCase() {

    private val depths = listOf(1, 5, 10, 20, 40)
    private val getterCounts = listOf(5, 20, 50)
    private val warmups = 2
    private val measured = 5

    fun testBenchmarkPrintsTable() {
        val results = LinkedHashMap<Pair<Int, Int>, Double>()
        for (n in getterCounts) {
            for (depth in depths) {
                results[depth to n] = timeChain(depth, n)
            }
        }

        val sb = StringBuilder("\n===== FieldFootprintAnalyzer benchmark (warm median, ms) =====\n")
        sb.append("chain depth (M methods) x getters-per-method (N)\n")
        sb.append(String.format("%-8s", "depth\\N"))
        for (n in getterCounts) sb.append(String.format("%10s", n))
        sb.append('\n')
        for (depth in depths) {
            sb.append(String.format("%-8d", depth))
            for (n in getterCounts) sb.append(String.format("%10.3f", results[depth to n]))
            sb.append('\n')
        }
        sb.append("\nwide fan-out (1 entry -> K siblings, each reads N getters):\n")
        sb.append(String.format("  K=20, N=20 : %.3f ms%n", timeWide(20, 20)))
        sb.append(String.format("  K=50, N=20 : %.3f ms%n", timeWide(50, 20)))
        sb.append("cyclic (a<->b, each reads N getters):\n")
        sb.append(String.format("  N=20 : %.3f ms%n", timeCyclic(20)))
        sb.append(String.format("  N=50 : %.3f ms%n", timeCyclic(50)))
        sb.append("==============================================================\n")
        println(sb)
    }

    // ---- generators ------------------------------------------------------------------------

    private fun genTarget(name: String, n: Int): String {
        val sb = StringBuilder("package com.acme; public class $name {")
        for (i in 0 until n) sb.append(" public String getF$i() { return null; }")
        return sb.append(" }").toString()
    }

    private fun genChain(name: String, targetFqn: String, depth: Int, n: Int): String {
        val sb = StringBuilder("package com.acme; public class $name {")
        for (i in 0..depth) {
            sb.append(" void m$i($targetFqn t) {")
            for (g in 0 until n) sb.append(" String v${i}_$g = t.getF$g();")
            if (i < depth) sb.append(" m${i + 1}(t);")
            sb.append(" }")
        }
        return sb.append(" }").toString()
    }

    private fun genWide(name: String, targetFqn: String, k: Int, n: Int): String {
        val sb = StringBuilder("package com.acme; public class $name {")
        sb.append(" void w($targetFqn t) {")
        for (j in 0 until k) sb.append(" s$j(t);")
        sb.append(" }")
        for (j in 0 until k) {
            sb.append(" void s$j($targetFqn t) {")
            for (g in 0 until n) sb.append(" String v${j}_$g = t.getF$g();")
            sb.append(" }")
        }
        return sb.append(" }").toString()
    }

    private fun genCyclic(name: String, targetFqn: String, n: Int): String {
        val sb = StringBuilder("package com.acme; public class $name {")
        sb.append(" void a($targetFqn t) {")
        for (g in 0 until n) sb.append(" String va_$g = t.getF$g();")
        sb.append(" b(t); }")
        sb.append(" void b($targetFqn t) {")
        for (g in 0 until n) sb.append(" String vb_$g = t.getF$g();")
        sb.append(" a(t); }")
        return sb.append(" }").toString()
    }

    // ---- timing ----------------------------------------------------------------------------

    private fun timeChain(depth: Int, n: Int): Double {
        val tName = "TChain_d${depth}_n$n"
        val cName = "Chain_d${depth}_n$n"
        myFixture.addClass(genTarget(tName, n))
        val chain = myFixture.addClass(genChain(cName, "com.acme.$tName", depth, n))
        return medianMs(chain, "m0", "com.acme.$tName", depth + 2)
    }

    private fun timeWide(k: Int, n: Int): Double {
        val tName = "TWide_k${k}_n$n"
        val cName = "Wide_k${k}_n$n"
        myFixture.addClass(genTarget(tName, n))
        val cls = myFixture.addClass(genWide(cName, "com.acme.$tName", k, n))
        return medianMs(cls, "w", "com.acme.$tName", 15)
    }

    private fun timeCyclic(n: Int): Double {
        val tName = "TCyc_n$n"
        val cName = "Cyc_n$n"
        myFixture.addClass(genTarget(tName, n))
        val cls = myFixture.addClass(genCyclic(cName, "com.acme.$tName", n))
        return medianMs(cls, "a", "com.acme.$tName", 15)
    }

    private fun medianMs(owner: PsiClass, entryName: String, fqn: String, maxDepth: Int): Double {
        val samples = ArrayList<Long>(measured)
        repeat(warmups + measured) { iteration ->
            val nanos = ReadAction.compute<Long, RuntimeException> {
                val entry = owner.findMethodsByName(entryName, false).first()
                val param = entry.parameterList.parameters.first()
                val start = System.nanoTime()
                FieldFootprintAnalyzer(fqn, maxDepth).analyzeParameter(entry, param)
                System.nanoTime() - start
            }
            if (iteration >= warmups) samples.add(nanos)
        }
        samples.sort()
        return samples[samples.size / 2] / 1_000_000.0
    }
}
