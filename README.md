# Live-Coding Toolkit
<!-- Plugin description -->
This plugin will help people write code faster for presentations, demos or discussions.

Currently, various ways to refactor to Lombok are implemented:
- Detect and offer to replace a constructor for final fields with @RequiredArgsConstructor
- When final fields are not set, offer to annotate the class with @RequiredArgsConstructor
- If a "log" is used anywhere in code, it offers to annotate the class with @Slf4j

More about me : https://www.victorrentea.ro

Next features?
- suggest replace private static final Logger log = LoggerFactory. with  @Slf4j
- move all anonymous classes to separate files as public classes 
- always silently auto-import toList(), assertEquals, assertThat as static imports on open file / reformat
<!-- Plugin description end -->
