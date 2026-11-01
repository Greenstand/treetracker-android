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

## Compose UI Tests

Robolectric Compose UI tests live in `app/src/test/.../uitest/<feature>/` and extend
`uitest/ComposeUiTest.kt`. The base class renders content inside the app theme with a real
`Navigator` over `backStack`, a `SnackbarController`, and a `string(R.string.x)` helper.

- Test the stateless composable (`Foo(state, callbacks)`) for each state it can render and each
  callback.
- Where the root `FooScreen` holds logic or navigation, pass it a ViewModel built with MockK'd
  dependencies, call `setContent(inspectionMode = false)`, and assert on `backStack`.
- They run only in `testDebugUnitTest`, because the activity they launch comes from the
  debug-only `ui-test-manifest` dependency:

```bash
./gradlew testDebugUnitTest --tests "org.greenstand.android.TreeTracker.uitest.*"
```

- A text field inside a dialog never idles under Robolectric when the screen is wider than
  320dp ([robolectric#8460](https://github.com/robolectric/robolectric/issues/8460)). Test
  classes that show one use `@Config(qualifiers = TEXT_FIELD_DIALOG_QUALIFIERS)`.

Unit tests run with Android resources (`includeAndroidResources = true`).
`app/src/test/resources/robolectric.properties` sets a plain `Application`, so
`TreeTrackerApplication` and Koin never start in tests, and pins every test to SDK 35 so they
share one Robolectric sandbox. Test JVMs get a 3 GB heap and restart every 100 test classes
(`app/build.gradle`).

## Screenshot Tests

Roborazzi screenshot tests live in `app/src/test/.../screenshot/` and only run with
`./gradlew verifyRoborazziDebug` (or `recordRoborazziDebug` to update the golden images in
`app/src/test/snapshots/`).

## Related QA Docs

- [Agent App Functions](agent-app-functions.md): debug-only adb hooks that create accounts and trees
- [App Distribution Testing](app-distribution-testing.md)
- [Releases](../release/releases.md)
