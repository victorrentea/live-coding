<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# live-coding Changelog

## [Unreleased]
### Added
- Initial scaffold created from [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template)
## 0.0.2
- Place @RAC to inner-most class in both suggestion to fix and to replace existing constructor
- Place @Slf4j to inner-most class
- Stop offering @RAC for fields with initializers
- Extend suggestion range for fix uninitiallized final fields
