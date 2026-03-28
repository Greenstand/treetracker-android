# Task 11: Dependency Cleanup Quick Wins

## Summary
Remove deprecated/dead dependencies and bump outdated libraries that are safe, non-breaking upgrades.

## Changes

### Removed (dead/deprecated)
| Dependency | Reason |
|-----------|--------|
| `androidx.legacy:legacy-support-v4` | Deprecated, unmaintained, zero imports in codebase |
| `kotlin-stdlib-jdk7` | Deprecated since Kotlin 1.8; merged into `kotlin-stdlib` |
| `maven.fabric.io` repo | Fabric shut down in 2020; dead Maven repository |
| `com.android.support.test:runner` | Old support library; replaced with `androidx.test:runner` |
| `com.android.support.test.espresso:espresso-core` | Old support library; consolidated with AndroidX variant |

### Version bumps
| Library | Old | New | Notes |
|---------|-----|-----|-------|
| Gson | 2.8.5 | 2.13.2 | Bug fixes, performance improvements |
| Timber | 4.7.1 | 5.0.1 | Rewritten in Kotlin, binary-compatible |
| JUnit | 4.13.1 | 4.13.2 | Patch release |
| Detekt | 1.18.0-RC2 | 1.23.8 | Ancient RC to latest stable |
| Room Testing | 2.2.6 | 2.6.1 | Aligned with room-runtime version |
| AndroidX Test Runner | 1.0.2 | 1.6.1 | Migrated to AndroidX coordinates |
| AndroidX Test Espresso | 3.0.2/3.4.0 | 3.6.1 | Consolidated duplicate entries |
| AndroidX Test JUnit | 1.1.3 | 1.2.1 | Minor version bump |

## Verification
- `./gradlew clean assembleDebug` - BUILD SUCCESSFUL
- `./gradlew test` - all tests pass
