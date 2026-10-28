# Testing

## Unit Testing

Unit tests live in the module whose code they test (`<module>/src/test/`), in the same package
as that code. See [Modules](../engineering/modules.md).

Key test utilities:

- `core:testing` (`testImplementation(project(":core:testing"))`):
  - `MainCoroutineRule.kt`: custom coroutine rule
  - `FakeFileGenerator.kt`: fake entities and models for repository, upload and ViewModel tests
  - `LiveDataTestUtil.kt`: `getOrAwaitValueTest` for LiveData
- `core:navigation` test fixtures (`testImplementation(testFixtures(project(":core:navigation")))`):
  `fakeNavigator` and `FakeClock`
- Feature modules get both from the `treetracker.android.feature` plugin.
- Mocking/streams: MockK and Turbine

Roborazzi screenshot tests stay in `app/src/test/.../screenshot`, since they render screens from
every feature module.

Run a single test class:

```bash
./gradlew :feature:capture:testDebugUnitTest --tests "org.greenstand.android.TreeTracker.capture.TreeCaptureViewModelTest"
```

## Related QA Docs

- [Agent App Functions](agent-app-functions.md): debug-only adb hooks that create accounts and trees
- [App Distribution Testing](app-distribution-testing.md)
- [Releases](../release/releases.md)
