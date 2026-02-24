# Testing

Tests are located in `app/src/test/` with structure mirroring main source. Key test utilities:

- `MainCoroutineRule.kt`: Custom rule for testing coroutines
- `FakeFileGenerator.kt`: Generate test files for upload testing
- Unit tests use MockK for mocking and Turbine for Flow testing

Run tests for a single class:

```bash
./gradlew test --tests "org.greenstand.android.TreeTracker.capture.TreeCaptureViewModelTest"
```
