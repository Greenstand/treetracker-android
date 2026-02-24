# Common Commands

## Building

```bash
# Build dev variant (recommended for development)
./gradlew assembleDev

# Build other variants
./gradlew assembleDebug
./gradlew assembleBeta
./gradlew assemblePrerelease
./gradlew assembleRelease
```

## Testing

```bash
# Run all unit tests
./gradlew test

# Run tests for specific variant
./gradlew testDevUnitTest
./gradlew testDebugUnitTest

# Run instrumented tests on connected device
./gradlew connectedDebugAndroidTest
```

## Code Quality

```bash
# Run all checks (includes ktlint, spotless, detekt)
./gradlew check

# Run ktlint only
./gradlew ktlintCheck

# Format code with ktlint
./gradlew ktlintFormat

# Run detekt static analysis
./gradlew detekt

# Apply spotless formatting
./gradlew spotlessApply
```

## Deployment (Fastlane)

```bash
# Setup fastlane
bundle install --path vendor/bundle
fastlane install_plugins
firebase login
```
