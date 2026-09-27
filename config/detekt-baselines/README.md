# Detekt baselines

Per-module baseline XML files live here. Each file grandfathers existing findings for that
Gradle module so `ignoreFailures = false` only fails the build on **new** violations.

Regenerate after intentional bulk cleanups or rule changes:

```bash
./gradlew detektBaselineAll
```

Do not hand-edit unless you know the Detekt baseline ID format.
