# Preferences and PrefKeys

`Preferences` is a wrapper around Android `SharedPreferences` for centralized key naming and lifecycle handling.

## `PrefKey`

- Use typed keys instead of raw strings.
- Keys can be composed to create path-like namespaces.

## `UserPrefKey`

- Behaves like `PrefKey`, but automatically scopes by current user.
- Useful for per-user settings on shared devices.

Example:

```kotlin
val myKey = UserPrefKey("myKey")
```

## Session-Scoped Keys

Session-prefixed keys are deleted on logout/session end.

```kotlin
val key = PrefKeys.SESSION + PrefKey("MyKey")
val userKey = PrefKeys.SESSION + UserPrefKey("MyUserKey")
val genericUserKey = PrefKeys.ROOT + UserPrefKey("GenericKey")
val genericSystemKey = PrefKeys.SYSTEM_SETTINGS + PrefKey("GenericKey")
```

Behavior summary:

- `PrefKeys.SESSION + ...` -> deleted on logout
- `PrefKeys.ROOT + ...` or `PrefKeys.SYSTEM_SETTINGS + ...` -> persisted
