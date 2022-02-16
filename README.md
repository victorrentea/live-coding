# Live-Coding IntelliJ IDEA Plugin
<!-- Plugin description -->
This plugin assists you when doing live-coding presentations with visual effects, inspections and utilities to speed up coding. 
In addition, it adds a number of inspections that automate various changes/migrations that I recommend doing during my workshops.    

I am using it every week in the workshops and talks I deliver. More about me on https://www.victorrentea.ro

### Inspections and Coding Speed
- Inspection to add `@RequiredArgsConstructor` (Lombok) instead of a boilerplate constructor
- When a 'log' is undefined, quick fix to add `@Slf4j` (Lombok) or write the code to define that field. Also, replace the field definition by `@Slf4j`.
- 'Quick Import' action to silently auto-import common constants (eg Duration.SECONDS), static methods (eg Mockito.mock) or common types (eg java.util.List). Bound by default to `Ctrl-Alt-Shift-O` shortcut. 
- Inspection to detect `@Data` on an `@Entity` and suggests replacing with safer code.
- Inspection to migrate usage of JUnit4 or JUnit5 assertions to AssertJ Assertions. Example: assertThat(actual).isEqualTo(expected);
- Inspection to optimize usage of AssertJ assertion api. Example: assertThat(list).hasSize(1)
- Inspection to detect overriding of @Before/@BeforeEach, without calling super() - usually a bug
- Inspection to detect `if` with _anemic_ or missing else that could be flipped to reduce function complexity ('Introduce Guard' refactoring)

### UX Features
- **Copy Git Coordinates** menu entry under Git> to quickly copy the remote git URL + branch to clipboard
- **Screen Effects**: 
  - Shake Screen (`F7`)
  - Broken Glass (`F8`)
  - Siren (`Shift-F8`)
- **Background Feelings**: change the background image to:
  - Call to Open Webcam (`Ctrl-Alt-6`)
  - Horror (`Ctrl-Alt-7`)
  - Child play (`Ctrl-Alt-8`)
  - Geek (`Ctrl-Alt-9`)
  - Clear (`Ctrl-Alt-0`)
- **Chapter Title** toolbar button to set the current section title
### Refactoring Aid
- **Declare new variable here** Inspection and Fix for local variables reassigned in the same method to unrelated new values
- **Display cognitive complexity of methods** Inspection, rendering it after the function name
- **Suggest sections that can be extracted** Inspection based on Cognitive Complexity, if:
  1. they have a single return value,
  2. no inner "return",
  3. doesn't strip the host function of ALL of its complexity ()


<!-- Plugin description end -->

## Declare new variable here
Purpose: many times in legacy code a variable is declared, assigned and used with a certain meaning, but later on it is reassigned to a different value. 
IntelliJ already highlights reassigned variables by underlining them to hint it's a bad idea to reassign local variables. 
Immutable local variables are a default in many other modern languages like Kotlin, Scala, Closure.
This inspection attempts to promote using variables for a single purpose, 
by proposing a fix to declare a new variable (with another name) whenever that's possible. 
In other words, enabling easier application of the **Split Variable** Refactoring from the classic refactoring moves.

Some cases are trivial, like for example `LinearRedefinition.java`, others are very tricky like `LaterUsageAfterReturningBlockNestedKO` while for other syntax examples
I honestly don't know yet how and IF I want to implement this refactoring.

#### Call for help
Please help me to identify more code examples for this refactoring. 
First check out the examples in the [src/test/live-coding-playground/src/main/java/declarenewlocal](https://github.com/victorrentea/live-coding/tree/main/src/test/live-coding-playground/src/main/java/declarenewlocal)
The convention is that whenever you see a `//` on a line, the fix is suggested there.

**Question is: did I miss any interesting case?**

Please help.
Thank you!

PS: you could also install the "Live-coding Toolkit" plugin if you want to test the feature on your code.

**Note**: After it stabilizes, I plan to contribute this quickfix to IntelliJ IDEA Community to make it available for all Java developers out there. 

## Next features/bugfixes:
- TODO broadcast an event so if many windows are open, the chapter name remains in sync + persist it to DB
- Detect (ideally UN-SET) "rearrange code" checkbox in commit dialog.
- Separate cognitive complexity rendering from visual extract aids inspections.
- IDEA: put _ instead of SPACE when writing the names of tests @Test public void service_does_not_depend_on_infrastructure
- Inspection to detect call(x, x.method()) and suggest inlining the 2nd param
- Suggest avoiding "default ->" in switch expressions on enums (to allow the compiler to check all branches)
- Detect consecutive IF on exclusive == , suggest adding an "else"
- Detect local calls to methods annotated with proxying annotations, like Spring: @Transactional, @Cacheable, @PreAuthorized, ... 
- LOW: Migrate all inspections to take into account configured severity
- **Big Dreams: "analyze parameter mutation" inspection** to report what fields of parameters change at call site (analysis upon request)
## Notes
- To see debug when running plugin locally, in the 'guest' IJ go to Help>Diagnostic Tools>Debug Log Settings and enter `#com.github.victorrentea.livecoding` in there
    Assertions.assertThatThrownBy


java.lang.RuntimeException: Document is locked by write PSI operations. Use PsiDocumentManager.doPostponedOperationsAndUnblockDocument() to commit PSI changes to the document.
Unprocessed elements: DECLARATION_STATEMENT(1598,1661), WHITE_SPACE(1594,1598), REFERENCE_EXPRESSION(1707,1723), WHITE_SPACE(1706,1707)
at com.intellij.psi.impl.source.PostprocessReformattingAspect.assertDocumentChangeIsAllowed(PostprocessReformattingAspect.java:297)
at com.intellij.psi.impl.PsiDocumentManagerImpl.beforeDocumentChangeOnUnlockedDocument(PsiDocumentManagerImpl.java:114)
at com.intellij.psi.impl.PsiDocumentManagerBase.beforeDocumentChange(PsiDocumentManagerBase.java:894)
at jdk.internal.reflect.GeneratedMethodAccessor206.invoke(Unknown Source)
at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
at java.base/java.lang.reflect.Method.invoke(Method.java:566)
at com.intellij.util.EventDispatcher.dispatchVoidMethod(EventDispatcher.java:120)
at com.intellij.util.EventDispatcher.lambda$createMulticaster$1(EventDispatcher.java:85)
at com.sun.proxy.$Proxy93.beforeDocumentChange(Unknown Source)
at com.intellij.openapi.editor.impl.DocumentImpl.beforeChangedUpdate(DocumentImpl.java:878)
at com.intellij.openapi.editor.impl.DocumentImpl.updateText(DocumentImpl.java:816)
at com.intellij.openapi.editor.impl.DocumentImpl.deleteString(DocumentImpl.java:578)
at com.intellij.codeInsight.template.TemplateBuilderImpl.initInlineTemplate(TemplateBuilderImpl.java:196)
at com.intellij.codeInsight.template.TemplateBuilderImpl.buildInlineTemplate(TemplateBuilderImpl.java:184)
at com.intellij.refactoring.rename.inplace.InplaceRefactoring.startTemplate(InplaceRefactoring.java:423)
at com.intellij.refactoring.rename.inplace.InplaceRefactoring.lambda$buildTemplateAndStart$2(InplaceRefactoring.java:371)
at com.intellij.openapi.command.WriteCommandAction$BuilderImpl.lambda$doRunWriteCommandAction$1(WriteCommandAction.java:150)
at com.intellij.openapi.application.impl.ApplicationImpl.runWriteAction(ApplicationImpl.java:947)
at com.intellij.openapi.command.WriteCommandAction$BuilderImpl.lambda$doRunWriteCommandAction$2(WriteCommandAction.java:148)
at com.intellij.openapi.command.impl.CoreCommandProcessor.executeCommand(CoreCommandProcessor.java:210)
at com.intellij.openapi.command.impl.CoreCommandProcessor.executeCommand(CoreCommandProcessor.java:184)
at com.intellij.openapi.command.WriteCommandAction$BuilderImpl.doRunWriteCommandAction(WriteCommandAction.java:157)
at com.intellij.openapi.command.WriteCommandAction$BuilderImpl.run(WriteCommandAction.java:124)
at com.intellij.refactoring.rename.inplace.InplaceRefactoring.buildTemplateAndStart(InplaceRefactoring.java:371)
at com.intellij.refactoring.rename.inplace.VariableInplaceRenamer.buildTemplateAndStart(VariableInplaceRenamer.java:116)
at com.intellij.refactoring.rename.inplace.InplaceRefactoring.performInplaceRefactoring(InplaceRefactoring.java:221)
at com.intellij.refactoring.rename.inplace.VariableInplaceRenamer.performInplaceRename(VariableInplaceRenamer.java:91)
at com.intellij.refactoring.rename.inplace.VariableInplaceRenameHandler.doRename(VariableInplaceRenameHandler.java:119)
at com.intellij.refactoring.rename.inplace.VariableInplaceRenameHandler.invoke(VariableInplaceRenameHandler.java:76)
at com.github.victorrentea.livecoding.declarenewlocal.DeclareNewLocalFix.invoke$lambda-2$lambda-1(DeclareNewLocalFix.kt:63)
at java.base/java.util.concurrent.CompletableFuture.uniAcceptNow(CompletableFuture.java:753)
at java.base/java.util.concurrent.CompletableFuture.uniAcceptStage(CompletableFuture.java:731)
at java.base/java.util.concurrent.CompletableFuture.thenAccept(CompletableFuture.java:2108)
at com.github.victorrentea.livecoding.declarenewlocal.DeclareNewLocalFix.invoke$lambda-2(DeclareNewLocalFix.kt:61)
at com.intellij.openapi.command.WriteCommandAction.lambda$runWriteCommandAction$4(WriteCommandAction.java:361)
at com.intellij.openapi.command.WriteCommandAction$BuilderImpl.lambda$doRunWriteCommandAction$1(WriteCommandAction.java:150)
at com.intellij.openapi.application.impl.ApplicationImpl.runWriteAction(ApplicationImpl.java:947)
at com.intellij.openapi.command.WriteCommandAction$BuilderImpl.lambda$doRunWriteCommandAction$2(WriteCommandAction.java:148)
at com.intellij.openapi.command.impl.CoreCommandProcessor.executeCommand(CoreCommandProcessor.java:210)
at com.intellij.openapi.command.impl.CoreCommandProcessor.executeCommand(CoreCommandProcessor.java:184)
at com.intellij.openapi.command.WriteCommandAction$BuilderImpl.doRunWriteCommandAction(WriteCommandAction.java:157)
at com.intellij.openapi.command.WriteCommandAction$BuilderImpl.run(WriteCommandAction.java:124)
at com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction(WriteCommandAction.java:361)
at com.github.victorrentea.livecoding.declarenewlocal.DeclareNewLocalFix.invoke(DeclareNewLocalFix.kt:48)
at com.intellij.codeInspection.LocalQuickFixOnPsiElement.applyFix(LocalQuickFixOnPsiElement.java:100)
at com.intellij.codeInspection.LocalQuickFixOnPsiElement.applyFix(LocalQuickFixOnPsiElement.java:90)
at com.intellij.codeInspection.LocalQuickFixOnPsiElement.applyFix(LocalQuickFixOnPsiElement.java:22)
at com.intellij.codeInspection.ex.QuickFixWrapper.invoke(QuickFixWrapper.java:74)
at com.intellij.codeInsight.intention.impl.ShowIntentionActionsHandler.lambda$invokeIntention$4(ShowIntentionActionsHandler.java:263)
at com.intellij.openapi.application.WriteAction.lambda$run$1(WriteAction.java:86)
at com.intellij.openapi.application.impl.ApplicationImpl.runWriteActionWithClass(ApplicationImpl.java:935)
at com.intellij.openapi.application.impl.ApplicationImpl.runWriteAction(ApplicationImpl.java:961)
at com.intellij.openapi.application.WriteAction.run(WriteAction.java:85)
at com.intellij.codeInsight.intention.impl.ShowIntentionActionsHandler.invokeIntention(ShowIntentionActionsHandler.java:263)
at com.intellij.codeInsight.intention.impl.ShowIntentionActionsHandler.lambda$chooseActionAndInvoke$3(ShowIntentionActionsHandler.java:239)
at com.intellij.openapi.command.impl.CoreCommandProcessor.executeCommand(CoreCommandProcessor.java:219)
at com.intellij.openapi.command.impl.CoreCommandProcessor.executeCommand(CoreCommandProcessor.java:174)
at com.intellij.openapi.command.impl.CoreCommandProcessor.executeCommand(CoreCommandProcessor.java:164)
at com.intellij.openapi.command.impl.CoreCommandProcessor.executeCommand(CoreCommandProcessor.java:150)
at com.intellij.codeInsight.intention.impl.ShowIntentionActionsHandler.chooseActionAndInvoke(ShowIntentionActionsHandler.java:238)
at com.intellij.codeInsight.intention.impl.ShowIntentionActionsHandler.chooseActionAndInvoke(ShowIntentionActionsHandler.java:222)
at com.intellij.codeInsight.daemon.impl.DaemonTooltipAction.execute(DaemonTooltipActionProvider.kt:54)
at com.intellij.codeInsight.daemon.impl.DaemonTooltipWithActionRenderer$addActionsRow$runFixAction$1.invoke(DaemonTooltipWithActionRenderer.kt:148)
at com.intellij.codeInsight.daemon.impl.DaemonTooltipWithActionRenderer$addActionsRow$runFixAction$1.invoke(DaemonTooltipWithActionRenderer.kt:50)
at com.intellij.codeInsight.daemon.impl.DaemonTooltipWithActionRendererKt$createActionLabel$1.hyperlinkActivated(DaemonTooltipWithActionRenderer.kt:372)
at com.intellij.ui.HyperlinkAdapter.hyperlinkUpdate(HyperlinkAdapter.java:11)
at com.intellij.ui.HyperlinkLabel.fireHyperlinkEvent(HyperlinkLabel.java:240)
at com.intellij.ui.HyperlinkLabel.processMouseEvent(HyperlinkLabel.java:162)
at java.desktop/java.awt.Component.processEvent(Component.java:6419)
at java.desktop/java.awt.Container.processEvent(Container.java:2263)
at java.desktop/java.awt.Component.dispatchEventImpl(Component.java:5029)
at java.desktop/java.awt.Container.dispatchEventImpl(Container.java:2321)
at java.desktop/java.awt.Component.dispatchEvent(Component.java:4861)
at java.desktop/java.awt.LightweightDispatcher.retargetMouseEvent(Container.java:4918)
at java.desktop/java.awt.LightweightDispatcher.processMouseEvent(Container.java:4547)
at java.desktop/java.awt.LightweightDispatcher.dispatchEvent(Container.java:4488)
at java.desktop/java.awt.Container.dispatchEventImpl(Container.java:2307)
at java.desktop/java.awt.Window.dispatchEventImpl(Window.java:2790)
at java.desktop/java.awt.Component.dispatchEvent(Component.java:4861)
at java.desktop/java.awt.EventQueue.dispatchEventImpl(EventQueue.java:778)
at java.desktop/java.awt.EventQueue$4.run(EventQueue.java:727)
at java.desktop/java.awt.EventQueue$4.run(EventQueue.java:721)
at java.base/java.security.AccessController.doPrivileged(Native Method)
at java.base/java.security.ProtectionDomain$JavaSecurityAccessImpl.doIntersectionPrivilege(ProtectionDomain.java:85)
at java.base/java.security.ProtectionDomain$JavaSecurityAccessImpl.doIntersectionPrivilege(ProtectionDomain.java:95)
at java.desktop/java.awt.EventQueue$5.run(EventQueue.java:751)
at java.desktop/java.awt.EventQueue$5.run(EventQueue.java:749)
at java.base/java.security.AccessController.doPrivileged(Native Method)
at java.base/java.security.ProtectionDomain$JavaSecurityAccessImpl.doIntersectionPrivilege(ProtectionDomain.java:85)
at java.desktop/java.awt.EventQueue.dispatchEvent(EventQueue.java:748)
at com.intellij.ide.IdeEventQueue.defaultDispatchEvent(IdeEventQueue.java:891)
at com.intellij.ide.IdeEventQueue.dispatchMouseEvent(IdeEventQueue.java:820)
at com.intellij.ide.IdeEventQueue._dispatchEvent(IdeEventQueue.java:757)
at com.intellij.ide.IdeEventQueue.lambda$dispatchEvent$6(IdeEventQueue.java:447)
at com.intellij.openapi.progress.impl.CoreProgressManager.computePrioritized(CoreProgressManager.java:818)
at com.intellij.ide.IdeEventQueue.lambda$dispatchEvent$7(IdeEventQueue.java:446)
at com.intellij.openapi.application.impl.ApplicationImpl.runIntendedWriteActionOnCurrentThread(ApplicationImpl.java:805)
at com.intellij.ide.IdeEventQueue.dispatchEvent(IdeEventQueue.java:498)
at java.desktop/java.awt.EventDispatchThread.pumpOneEventForFilters(EventDispatchThread.java:203)
at java.desktop/java.awt.EventDispatchThread.pumpEventsForFilter(EventDispatchThread.java:124)
at java.desktop/java.awt.EventDispatchThread.pumpEventsForHierarchy(EventDispatchThread.java:113)
at java.desktop/java.awt.EventDispatchThread.pumpEvents(EventDispatchThread.java:109)
at java.desktop/java.awt.EventDispatchThread.pumpEvents(EventDispatchThread.java:101)
at java.desktop/java.awt.EventDispatchThread.run(EventDispatchThread.java:90)