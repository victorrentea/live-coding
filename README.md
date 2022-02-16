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
## Call for help
I plan to improve the code style to bring it closer to the IntelliJ codebase, and then contribute parts of this plugin to the community opensource IntelliJ project.

**If you have prior experience with IntelliJ platform development, and you are willing to pair program with me several rounds to help me improve the code, please contact me!** 

## Next features/bugfixes:
- Chapter title to be shared with all IJ instances - button to be auto updated
- Chapter title to be displayed on top only when no IJ is visible
- Click on Chapter stay on top to open edit screen. 
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

