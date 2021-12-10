<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# live-coding Changelog
    
## [Unreleased]
### Added
- Initial scaffold created from [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template)
## 0.0.2
- Place @RAC to inner-most class in both suggestion to fix and to replace existing constructor
- Place @Slf4j to inner-most class
- Stop offering @RAC for fields with initializers
- Extend suggestion range for fix uninitialized final fields
## 0.0.5
- Added Auto Import Statics action that replaces a list of commonly used static methods with import static
- Added @VisibleForTesting abuse detector
- Bugfixes
## 0.0.6
- Suggest replace with @RAC only as WARNING from >= 2 args, only fix if 1 arg
- Fixed a bug: add @RAC crashed if line was not completed with ;
- Removed VisibleForTesting check as I found a standard inspection already doing that, 'Java | General | Test-only class or method call in production code'
- Added quickfix inside "" in log.debug("")  -> for easier ALT ENTER
- Added more functions to import statically (groupingBy,toMap, Predicate.not)
## 0.0.7
- Support auto-adding static imports to fields , not only methods
- Add more static imports :  "java.lang.System#currentTimeMillis",  "org.mockito.ArgumentMatchers#anyInt",   "java.util.concurrent.TimeUnit#MILLISECONDS",  "java.time.Duration#ofSeconds",
  "java.time.Duration#ofMillis"
- Quickfix to define a Slf4J logger on the class (besides Lombok @Slf4j) on unresolved "log" token