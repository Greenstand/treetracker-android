# Task 38 - Build with JDK 25

## Goal

Build the project, locally and in CI, with JDK 25. Gradle 8.13 couldn't run on JDK 25
("Unsupported class file major version 69"), and Android Studio now bundles JDK 25.

## Changes

- **Gradle 8.13 -> 9.5.1.** Gradle runs on JDK 25 from 9.1. 9.5 is the newest version that AGP 8.x
  supports: Gradle 9.6 removed an internal API that AGP 8.x uses.
- **AGP 8.11.0 -> 8.13.2**, the last 8.x release. AGP 9 is a bigger migration (built-in Kotlin,
  DSL changes) and is left for its own task.
- **detekt 1.23.8 -> 2.0.0-alpha.6** (plugin ID `dev.detekt`). detekt 1.23's bundled Kotlin
  compiler crashes on JDK 25 (it can't parse the Java version string `25.0.3`), and only detekt 2
  fixes this. `detekt.yml` was migrated so the same checks run with the same limits:
  - Renamed rules: `CommentOverPrivate*` -> `DocumentationOverPrivate*`, `EmptyKtFile` ->
    `EmptyKotlinFile`, `FunctionMax/MinLength` -> `FunctionNameMax/MinLength`, `MayBeConst` ->
    `MayBeConstant`, `RedundantVisibilityModifierRule` -> `RedundantVisibilityModifier`,
    `SpacingBetweenPackageAndImports` -> `SpacingAfterPackageAndImports`,
    `UntilInsteadOfRangeTo` -> `RangeUntilInsteadOfRangeTo`, `UnusedImports` -> `UnusedImport`,
    `UnusedPrivateMember` -> `UnusedPrivateFunction`.
  - `UnnecessaryAbstractClass` split into `AbstractClassCanBeConcreteClass` and
    `AbstractClassCanBeInterface`, both still off.
  - `threshold` keys became `allowed*` keys. Where detekt 2 changed from "report at N" to "allow up
    to N" (its default dropped by one), the value dropped by one too, so the limit is unchanged:
    `ComplexCondition`, `CyclomaticComplexMethod`, `LongParameterList` and
    `StringLiteralDuplication`.
  - `excludes` are YAML lists instead of comma-separated strings.
  - Removed `build.maxIssues: 0` (detekt 2 fails on any issue by default) and
    `PreferToOverPairSyntax` (dropped from detekt 2 core).
- **MockK 1.13.12 -> 1.14.11.** On JDK 25, 1.13.12 couldn't mock `java.io.File` or
  `Runtime`, which failed 8 tests in `TreeCapturerTest` and `CheckForInternetUseCaseTest`.
- **CI:** every `setup-java` step uses JDK 25 (Temurin) and `actions/setup-java@v4`.
- `docs/getting-started/prerequisites.md` now says JDK 25 is required.

Not changed: the app still compiles to Java 11 bytecode (`jvmTarget`/`sourceCompatibility`). That
is what Android runs, and is separate from the JDK that runs the build.

## Verification

On JDK 25 (Android Studio's bundled `25.0.3`):

- `assembleDebug` and `assembleBeta` succeed.
- `ktlintCheck` passes. `detekt` passes with 0 findings across all 308 Kotlin files, as on 1.23.8.
- `test`: all 1,856 tests pass across the debug, dev, beta, prerelease and release variants.
- `verifyRoborazziDebug` passes, so screenshots are unchanged.
- The debug build installs and launches to the dashboard on an Android 17 emulator, with no app crashes.
