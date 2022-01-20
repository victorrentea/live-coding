# Live-Coding IntelliJ IDEA Plugin
<!-- Plugin description -->
This plugin aims to help people write code faster for presentations, demos or discussions accompanied by code. Like I do every single week.

More about me : https://www.victorrentea.ro

### Inspections and Code Speed
- Add `@RequiredArgsConstructor` (lombok) either when `private final` fields are not initialized or to replace the constructor (especially in a Spring-managed bean).
- QuickFix to define a `log` Slf4j logger, on any undefined `log` token, either by adding a `@Slf4j` (lombok) or declaring a standard Sfl4j `Logger log` field on the class
- Action to silently auto-import usual static methods and constants. The action _Auto-import statics_ is bound by default
  to `Ctrl-Shift-O`, but the shortcut is editable. The list of static tokens is editable via settings, but many commonly used ones are predefined, such as: 
  Collectors.toList, Assertions.assertThat, Mockito.mock/when/verify, Collectors.toList/toSet, Duration.*
- Detects dangerous code generated by Lombok in a JPA `@Entity` and suggests fixes: For example replace `@Data` with `@Getter` `@Setter` and careful `@ToString`
- Inspection to detect overriding of @Before/@BeforeEach, without calling super() - usually a bug

### Refactoring Aid
- **Declare new variable here** inspection and fix for local variables reassigned in the same method to unrelated new values
- **Suggest extractable code sections** based on Cognitive Complexity, if (1) they have a single return value, (2) no inner "return", (3) doesn't strip the host function of ALL of its complexity


### UX Features
- **Copy Git Coordinates** menu entry under Git> to quickly copy the remote git URL + branch to clipboard
- **Screen Effects**: 
  - Shake Screen (`F7`)
  - Broken Glass (`F8`)
- **Background Feelings**: change the background image to:
  - Horror (`Ctrl-Alt-7`)
  - Child play (`Ctrl-Alt-8`)
  - Geek (`Ctrl-Alt-9`)
  - Clear (`Ctrl-Alt-0`)
- 

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

## Next features:
- Alt-ENTER on any part of the ctor (including "public") -> @RAC
- Increase the font size for complexity rendering and change orange to a more visible color.
- use @ToString.Exclude instead of @Exclude when fixing @Data on @Entity
- make autoimporter import List/Set/Map also if not imported
- Replace slf4j Logger log = with @Slf4j inspection
- "Webcam Hero" effect/notification to encourage more participants to open their webcams
- Cleanup and split of cognitive complexity vs extract visual hints
- Inspection to detect usages of jupiter Assertions or junit Assert and suggest replacing them with Assertions (assert4j) as a far better alternative
- Inspection to detect call(x, x.method()) and suggest inlining the 2nd param
- Suggest avoiding "default ->" in switch expressions on enums (to allow the compiler to check all branches)
- Detect consecutive IF on exclusive == , suggest adding an "else"
- Detect local calls to methods annotated with proxying annotations, like Spring: @Transactional, @Cacheable, @PreAuthorized, ... 
- **Big Dreams: "analyze parameter mutation" inspection** to report what fields of parameters change at call site (analysis upon request)
## Notes
- To see debug when running plugin locally, in the 'guest' IJ go to Help>Diagnostic Tools>Debug Log Settings and enter `#com.github.victorrentea.livecoding` in there
    Assertions.assertThatThrownBy