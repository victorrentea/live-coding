# Field-Footprint Analyzer — Findings

Scope: `com.github.victorrentea.livecoding.footprint`. Produced by adding a performance
benchmark, an adversarial edge-case census + test suite, and one small analyzer fix.
All footprint tests are green (`./gradlew test --tests "*footprint*"`: 39 tests, 0 failures).

---

## 1. Performance & inspection feasibility

### Benchmark (warm median, ms) — `FieldFootprintBenchmarkTest`

Synthetic call graphs: a fat target `T` with **N** getters, and a chain of **M** methods where
each `m_i(T t)` reads all N getters then calls `m_{i+1}(t)` (object propagates to depth M).
`maxDepth` was raised to `M+2` so the *full* chain is traversed (the analyzer's default cap is 15).

```
chain depth (M methods) x getters-per-method (N)
depth\N          5        20        50
1            0.530     0.902     2.164
5            1.259     2.815     6.740
10           2.280     5.347    11.544
20           3.660    10.308    21.818
40           6.265    18.357    44.421

wide fan-out (1 entry -> K siblings, each reads N getters):
  K=20, N=20 :  8.448 ms
  K=50, N=20 : 21.091 ms
cyclic (a<->b, each reads N getters):
  N=20 : 0.805 ms      (cycle detection terminates immediately — cheap)
  N=50 : 2.057 ms
```

Cost scales ~linearly with **total getter references traversed** (`≈ M·N`). Cyclic graphs are
cheap (the visited-set short-circuits the second visit). A realistic mid-size case (depth 10,
N 20) is ~5 ms **per parameter**; a deep/wide cold graph is 20–45 ms **per parameter**.

### Verdict: NOT viable as an on-the-fly (transitive) inspection

Two independent reasons, both grounded in the numbers above:

1. **Per-pass cost.** On-the-fly inspections re-run on essentially every keystroke, over every
   fat-object parameter in the visible file. Census below shows real service classes carry
   *many* such parameters (1,057 fat-typed params across the sample repo). At 5–45 ms each,
   a single file's highlight pass can reach hundreds of ms — enough to make typing lag.

2. **The cache-invalidation killer (decisive).** A transitive footprint depends on *other
   files* (the callees). A `CachedValue` is only correct if its dependency actually changes
   whenever any input changes — so the only sound dependency for a cross-file result is the
   global `PsiModificationTracker.MODIFICATION_COUNT`. That counter is bumped on **every
   keystroke anywhere in the project**. Therefore a cached transitive footprint is invalidated
   constantly and **recomputed on every highlight pass** — caching buys nothing. You cannot
   scope the dependency to just the current file, because an edit in a callee file *must*
   invalidate the result, and the analyzer has no cheap way to know which files are callees.

   By contrast, an **intra-procedural** fact ("which fields are read *directly* in this method
   body") is file-local: its `CachedValue` can depend on just the containing file/method, it is
   never invalidated by edits elsewhere, and it is sub-millisecond. That is cheap enough for
   on-the-fly.

### Recommendation

- Keep the full transitive `FieldFootprintAnalyzer` where it already is: the **on-demand
  intention** (behind Alt+Enter + a cancelable progress bar) and, additionally, a **batch
  inspection** (`isOnTheFly == false`) that sweeps the whole project off the typing path.
- Add a cheap **on-the-fly inspection** (`WEAK_WARNING`, yellow) that flags a fat-object
  parameter as a *thin-DTO candidate* when **either**:
  - **(a) intra-procedural + leaf**: the set of fields used *directly in this body* is small
    **and** the method is a **leaf for that parameter** — it never propagates the param onward
    (no call passes it as an argument, no store into a field, no return). When the param does
    not escape, the intra-procedural footprint *is* the complete footprint, and it is
    file-local and cheap; **or**
  - **(b) read the persisted annotation**: parse the count already written by the intention in
    `@param name reads {a, b}` and highlight when it is small and not `ALL`/`?`. The persisted
    Javadoc annotation acts as the cache a live inspection can watch for free.
- **"Small subset" threshold**: candidate when verdict is `SOUND` **and**
  `reads ≤ 3 fields` **OR** `reads ≤ 20% of the class's declared instance fields`
  (whichever the team prefers; the percentage form scales with class size). Never highlight on
  `WHOLE_OBJECT` or `UNKNOWN`.

---

## 2. Edge-case census (read-only sweep of `modulabgold-pr`)

Fat classes confirmed: `RequestData` (~349 members, referenced in 716 files),
`ResultData` (~164, 196 files), `PatientData` (~79, 184 files). Approx occurrence counts of each
syntactic shape a fat-object parameter appears in (grep over `*.java`):

| Shape | ~count | Analyzer handling |
|---|---:|---|
| 2-level getter chains `a.getB().getC()` | 18,436 | correct → dotted path |
| deep getter chains (≥3 levels) | 1,527 | correct → dotted path |
| setters `.setX(...)` (all types, coarse) | 71,484 | correct → `writes {}` |
| ternary containing a getter | 1,433 | correct |
| method params of fat type | 1,057 (+410 later position) | entry point |
| casts `(RequestData/ResultData/PatientData)` | 648 | correct (cast branch + cast loop) |
| for-each over a getter result | 424 | correct |
| add to a collection `.add(...Data)` | 241 | UNKNOWN (safe — escapes) |
| `instanceof` | 224 | correct → ignored (no field read) |
| constructor `new X(...Data)` | 140 | correct → recurses into ctor |
| **method reference `data::m`** | **105** | **was BROKEN → now fixed** |
| null-check `data != null` | 54 | correct → ignored |
| store into field `this.x = ...Data` | 12 | correct → UNKNOWN (escapes) |
| switch on a getter | 5 | correct |
| put into a map | 3 | UNKNOWN (safe) |
| super call passing the param | 2 | correct → recurses |

Public-field access (`data.someField` without a getter) was **not observed** — these DTOs are
fully encapsulated — so the analyzer's blind spot there (see bug #5) does not bite this codebase.

---

## 3. Analyzer bugs found (prioritized)

Severity is measured by **soundness impact**: the dangerous failure is a false `SOUND`/thin
verdict, which tells the user "safe to slim down" when the object is actually read whole or
escapes.

### FIXED — #0 · Bound method references were entirely ignored — **HIGH**

Repro (any of):
```java
Sup<String> s = order::getId;      // read of `id`      -> was: nothing
Consumer<String> c = order::setStatus; // write of `status` -> was: nothing
Supplier<String> x = order::toXML; // WHOLE-OBJECT sink  -> was: SOUND (dangerous!)
```
`classify()` fell through to the generic `PsiReferenceExpression` branch (a
`PsiMethodReferenceExpression` *is* a `PsiReferenceExpression`), whose `parent.parent as?
PsiMethodCallExpression` is null for a method ref, so it returned `EMPTY`. A bound method-ref to
a getter under-reported reads; to a setter dropped a write; to `toXML`/`clone` **silently missed
a whole-object sink** and reported `SOUND`. The census shows 105 method-ref uses on these types
(bound forms like `requestData::setComments` are real).

**Fix** (internal only, public API + `Footprint` shape unchanged): a
`PsiMethodReferenceExpression` branch placed *before* the generic one, delegating to a new
`methodRefOnTarget(name)` that applies the same whole-object / getter / setter rules (a method
reference can't be chained, so a getter yields exactly one field). Unbound `Type::method` refs
(e.g. `ResultData::getData` over a stream) are correctly still ignored — their qualifier resolves
to a `PsiClass`, not the tracked variable. Tests: `testMethodReference*`.

### KNOWN BUGS (documented; current behavior asserted so the suite stays green)

**#1 · Reassignment aliasing not followed — MEDIUM**
```java
Fat y; y = f; String a = y.getA();   // read of `a` is MISSED (verdict stays SOUND)
```
`expandAliases()` only follows a local's *initializer*, not a later `y = f` assignment. Effect:
under-reports reads → false-thin. Test: `testKnownBugReassignmentAliasNotTracked`. Fix would
require dataflow over assignments (non-trivial); left as-is.

**#2 · Array-element store escape missed — MEDIUM**
```java
Fat[] arr = new Fat[1]; arr[0] = f;  // escape is MISSED (stays SOUND; should be UNKNOWN)
```
The assignment branch only treats a field LHS as an escape; an array-access LHS
(`PsiArrayAccessExpression`) falls through to `EMPTY`. Effect: misses an escape → false-thin.
Test: `testKnownBugArrayStoreEscapeMissed`. (A safe fix: also degrade to `UNKNOWN` when the
target is the RHS of an assignment whose LHS is an array access.)

**#3 · Implicit `toString()` via string concatenation missed — MEDIUM/LOW**
```java
String s = "x" + f;   // calls f.toString() (often reads every field) — MISSED, stays SOUND
```
Note the asymmetry: an **explicit** `f.toString()` degrades to `UNKNOWN` (safe — it's a
non-getter call), but the implicit call through `+` is invisible to the walk. Tests:
`testKnownBugImplicitToStringViaConcatMissed`, `testExplicitToStringIsConservativelyUnknown`.

**#4 · Acronym getter decapitalization — LOW (cosmetic)**
```java
f.getURL();   // path recorded as "uRL"  (JavaBeans Introspector.decapitalize -> "URL")
```
The verdict is unaffected, but the persisted `@param reads {uRL}` text is wrong for all-caps
runs. Test: `testKnownBugAcronymGetterDecapitalization`. (Fix: don't decapitalize when the first
two chars are both upper-case.)

**#5 · Public-field read/write ignored — LOW (not triggered here)**
`f.publicField` (read or write) is ignored — `classify` only recognizes method calls. Harmless
for the sample codebase (all DTOs are encapsulated), so no test was added; noted for completeness.

### Conservative-but-correct behaviors (NOT bugs — never a false SOUND)

Widening to a supertype / `Object` / varargs, `collection.add(f)`, `map.put(k, f)`, and any
non-getter business call (`f.recalculate()`, `f.hasChildren()`, explicit `f.toString()`) all
degrade to `UNKNOWN`. That is the safe direction. Verified by
`testWidenedToSupertypeIsUnknown`, `testPassedToVarargsIsUnknown`, `testAddedToCollectionIsUnknown`,
`testExplicitToStringIsConservativelyUnknown`.

---

## 4. Tests added

- **`FieldFootprintBenchmarkTest`** (1 test) — generates chain / wide / cyclic synthetic graphs,
  prints the warm-median table above. No timing assertions (flaky); the table is the deliverable.
- **`FieldFootprintEdgeCasesTest`** (34 tests) — one focused test per syntactic shape, driving the
  analyzer directly and asserting reads/writes/verdict on neutral `com.acme` fixtures:
  - getters: deep 3-level chain, ternary, for-each over array, for-loop body, switch, mixed read+write.
  - casts: cast-then-getter, downcast from a widened local (`Object o = f; ((Fat)o).getB()`).
  - escapes/opaque: store into field, returned, widened to supertype, varargs, collection `add`.
  - ignored shapes: `instanceof`, null-check then getter.
  - whole-object sinks: `clone()`, `toXML()`, serializer argument (`ObjectMapper.writeValue(out, f)`).
  - transitive: this-qualified call, overload resolution, super call, constructor recursion,
    constructor-that-stores, lambda capture, anonymous-class capture, mutual recursion (cycle).
  - method references (the fix): getter → read, setter → write, `toXML` → whole-object.
  - KNOWN BUGs #1–#4 + the explicit-`toString` contrast.

All fixtures are client-agnostic (`com.acme`); no customer classes were copied into the plugin.

---

## 5. Other recommendations / human decisions

- **Decide on KNOWN BUG #2** (array-store escape): a two-line fix in `classify` would remove a
  real false-thin path; low risk. Recommend fixing.
- **Decide on KNOWN BUG #4** (acronym decapitalization): trivial fix, but it changes the text of
  already-persisted `@param reads {...}` annotations — coordinate so existing annotations stay
  consistent.
- **`has*()` / `should*()` boolean accessors** are treated as opaque non-getters (→ `UNKNOWN`).
  That is safe but conservative; if these are common property accessors in the domain, consider
  adding them to the getter recognizer (they were rare in the census, so not urgent).
- **`getX(int)` indexed getters** are treated as property getters by name; acceptable, but note
  they may read more than a single field.
- The `Sinks` method/type name sets are heuristic. A Settings page (already flagged as "the
  natural next step" in the source) would let teams tune `WHOLE_OBJECT_METHODS`,
  `SINK_QUALIFIER_TYPES`, and `minInstanceFields` without a code change.
