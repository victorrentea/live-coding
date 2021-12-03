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