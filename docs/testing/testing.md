# Testing

## Unit Testing

Tests are located in `app/src/test/` with structure mirroring main source.

Key test utilities:

- `MainCoroutineRule.kt`: custom coroutine rule
- `FakeFileGenerator.kt`: test file generation for upload flows
- Mocking/streams: MockK and Turbine

Run a single test class:

```bash
./gradlew test --tests "org.greenstand.android.TreeTracker.capture.TreeCaptureViewModelTest"
```

## Related QA Docs

- [Agent App Functions](agent-app-functions.md): debug-only adb hooks that create accounts and trees
- [App Distribution Testing](app-distribution-testing.md)
- [Releases](../release/releases.md)
