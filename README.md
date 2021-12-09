# Live-Coding Toolkit
<!-- Plugin description -->
This plugin helps people write code faster for presentations, demos or discussions accompanied by code.

Currently, various ways to refactor to Lombok are implemented:
- Offer to replace a constructor setting final fields with @RequiredArgsConstructor (lombok)
- When final fields are not initialized (compilation error), offer to annotate the class with @RequiredArgsConstructor
- If a "log" is used anywhere in code, it offers to annotate the class with @Slf4j instead
- Action to silently auto-import Collectors.toList, Assertions.assertThat[assertJ], Mockito.mock/when/verify as static imports on open file / reformat
- Detects places in production code where a method called with @VisibleForTesting is used

More about me : https://www.victorrentea.ro

Next features:
- static import System.currentTimeMillis(), org.mockito.ArgumentMatchers#anyInt   TimeUnit.MILLISECONDS
  Duration.ofMillis Duration.ofSeconds
  // make sure static import doesn't collide with existing static imports.
- suggest alt-enter on field in error only for >= 2 args
- quickfix undefined "log" token to define Slf4j logger (without Lombok)
- move all anonymous classes to separate files as public classes 
- settings page to allow users to set more static methods to import
- Menu entry in GIt to copy to clipboard the current remote URL and Branch cname + timestamp sau commit ID
<!-- Plugin description end -->

