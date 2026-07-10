# live-coding plugin — Claude notes

Working notes for AI agents. This is a **public** IntelliJ plugin on the JetBrains Marketplace —
keep everything here (and in code/tests/docs) **client-agnostic**: no customer names, class names,
or codebases. Features must be general-purpose (heuristics, not hardcoded names).

## What this is

IntelliJ IDEA plugin "Live-Coding Toolkit": inspections, intentions, refactoring aids, and
live-coding/presentation UX. New code is **Kotlin**; a lot of older code is Java.
- Plugin XML id: `com.github.victorrentea.slf4jplugin` (legacy; must stay this way to keep installed users).
- Marketplace: https://plugins.jetbrains.com/plugin/18087-live-coding-toolkit (numeric id **18087**).
- Package layout: `com.github.victorrentea.livecoding.<feature>` (e.g. `footprint`, `lombok`, `assertj`, `extracthints`).

## Stack / build / run

- IntelliJ Platform Gradle Plugin **2.16.0**, Kotlin **2.1.0**, target **IC 2025.1**, `pluginSinceBuild=243`, JDK **21**. Version + platform live in `gradle.properties`.
- `./gradlew buildPlugin` → `build/distributions/live-coding-<version>.zip` (also runs `verifyPlugin`).
- `./gradlew test` — JUnit on `LightJavaCodeInsightFixtureTestCase`.
- `./gradlew runIde` — sandbox IDE. **Do NOT run `runIde` while another Gradle task (tests / a background agent) runs on this repo** — the second sandbox hands off / exits within seconds. Run them sequentially.
- Extensions (inspections / intentions / actions) are registered in `src/main/resources/META-INF/plugin.xml`.
- **Marketplace description comes from `README.md`** between `<!-- Plugin description -->` … `<!-- Plugin description end -->` (see `build.gradle.kts` `pluginConfiguration.description`), NOT from `plugin.xml`. Edit that README block to change marketplace copy.

## IntelliJ PSI gotchas (each cost real debugging time)

- `WriteCommandAction.runWriteCommandAction(project, name, groupId, runnable, file)` — you MUST pass the target `PsiFile` as the trailing vararg, or PSI writes fail with *"Cannot modify a read-only file"*.
- Never `CodeStyleManager.reformat(docComment)` — a `PsiDocComment` isn't a standalone format root (*"Invalid root block PSI element"*). Use `reformatRange(method, doc.textRange.startOffset, doc.textRange.endOffset)`.
- `PsiMethodReferenceExpression` **is a** `PsiReferenceExpression` — match it FIRST in a `when`, else bound method refs (`x::getFoo`, `x::toString`) fall into the generic call branch and are mishandled.
- Heavy intentions: `startInWriteAction() = false`, run analysis under a cancelable progress bar + read action, then a separate `WriteCommandAction`. Override `generatePreview()` to return `IntentionPreviewInfo.EMPTY`, or the platform runs the (expensive) intention on a copy for the Alt+Enter preview.
- Guard modal progress with `ApplicationManager.getApplication().isUnitTestMode` (run the computable directly in tests).
- JavaBeans property name from a getter: keep a leading all-caps run (`getURL()` → `URL`, `getId()` → `id`), don't blindly lowercase the first char.

## Testing conventions

- `LightJavaCodeInsightFixtureTestCase`, **JUnit3-style `testXxx` method names** (no `@Test`; backtick names are NOT discovered).
- **Neutral fixtures only** (`package com.acme; class Order {…}`) — never copy real/customer classes into tests.
- Intentions: `myFixture.launchAction(myFixture.findSingleIntention("<intention text>"))`, then assert on `myFixture.file.text`.
- Inspections: `myFixture.enableInspections(...)`, `myFixture.doHighlighting()`, filter by a stable substring of the problem description.

## Performance rule for on-the-fly inspections (important)

A `LocalInspectionTool` runs on every keystroke, so it must be **intra-procedural / file-local**.
A transitive (cross-file) analysis depends on *other* files, so the only correct `CachedValue`
dependency is the global `PsiModificationTracker.MODIFICATION_COUNT` — bumped on every keystroke
anywhere, so the cache never survives and you recompute constantly. **Keep transitive analysis out
of on-the-fly inspections**: put it in an on-demand intention or a batch (`!isOnTheFly`) inspection.
If you must reuse a transitive analyzer on-the-fly, cap it so it never resolves cross-file (e.g.
`maxDepth = 0`, bail *before* `resolveMethod`).

## Publishing → Marketplace

Two paths; both need the `PUBLISH_TOKEN`.

**Local (fastest, most reliable):** the token is in `secrets.env` (gitignored):
```
set -a; . ./secrets.env; set +a
./gradlew publishPlugin
```

**CI (the `main` → release flow):**
1. Bump `pluginVersion` in `gradle.properties`; add notes under `## Unreleased` in `CHANGELOG.md`; update the README marketplace block for user-facing features.
2. Merge to **`main`** → `build.yml` (triggers ONLY on push to `main`) builds/tests/verifies and cuts a **draft** GitHub release.
3. **Publish the draft** (`gh release edit v<ver> --draft=false --latest`) → `release.yml` (triggers on release published) runs `./gradlew publishPlugin` with the repo's `PUBLISH_TOKEN` secret → marketplace.
4. Marketplace has a moderation/indexing delay. Poll `https://plugins.jetbrains.com/plugins/list?pluginId=com.github.victorrentea.slf4jplugin` and watch the top `<version>` until it flips.

**CI gotchas learned:**
- `build.yml` fires on **every** push to `main` and cuts a fresh draft — don't push unrelated commits to `main` while a release is in flight.
- The `patchChangelog` + "Create Pull Request" steps in `release.yml` are cosmetic and are `continue-on-error` — they must never block `publishPlugin` (they used to, aborting the whole publish).
- Some past releases were published **manually** (no matching GitHub release), so the CI release path goes stale — verify it end-to-end, or just use the local path above.
- To move `main` forward without disturbing the working checkout (e.g. a running sandbox / agent): FF via an isolated worktree —
  `git worktree add -B main /tmp/x origin/main && git -C /tmp/x merge --ff-only <branch> && git -C /tmp/x push origin main && git worktree remove /tmp/x --force`.
