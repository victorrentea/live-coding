# live-coding plugin — Claude notes

## Publishing

JetBrains Marketplace `PUBLISH_TOKEN` is stored in `secrets.env` (gitignored).
To publish, source it and run gradle:

```
set -a; . ./secrets.env; set +a
./gradlew publishPlugin
```

Marketplace listing: https://plugins.jetbrains.com/plugin/18087-live-coding-toolkit
Plugin XML id: `com.github.victorrentea.slf4jplugin` (legacy; must stay this way to keep installed users).
