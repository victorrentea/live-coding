# Live-Coding Toolkit
<!-- Plugin description -->
This plugin helps people write code faster for presentations, demos or discussions accompanied by code.

Currently, various ways to refactor to Lombok are implemented:
- Offer to replace a constructor setting final fields with @RequiredArgsConstructor (lombok)
- When final fields are not initialized (compilation error), offer to annotate the class with @RequiredArgsConstructor
- If a "log" is used anywhere in code, it offers to annotate the class with @Slf4j instead
- Action to silently auto-import Collectors.toList, Assertions.assertThat[assertJ], Mockito.mock/when/verify + many more as static imports on Ctrl-Shift-O ("Auto-import statics" action)

More about me : https://www.victorrentea.ro

Next features:
- settings page to allow users to set more static methods to import
- Split Variable
  if (employees.size() != 0) {
    averageConsultantSalary /= employees.size();
  }
  for () v = v + 1;  NO
  v = 73;
  try {v += 44;}
<!-- Plugin description end -->

