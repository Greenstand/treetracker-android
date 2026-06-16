# Task 22 — Split monolithic TreeTrackerDAO into entity-specific DAOs (#1235)

Branch: `refactor/split-tree-tracker-dao` · PR #TBD

## Goal

`TreeTrackerDAO` is a single Room interface covering 10 entity groups and is injected
into ~25 production classes. Splitting it into entity-specific DAOs improves testability,
narrows change impact, and matches the existing `entity/` and `legacy/entity/` layout.

Split `TreeTrackerDAO` (~385 lines) into focused Room DAOs:

1. `TreeDAO` — current `tree` table
2. `UserDAO` — `user`
3. `SessionDAO` — `session`
4. `OrganizationDAO` — `organization`
5. `DeviceConfigDAO` — `device_config`
6. `PlanterDAO` — legacy `planter_info` and `planter_check_in`
7. `LocationDAO` — `location` and legacy `location_data`

Update AppDatabase and RoomModule to expose and wire the new DAOs.

## PR plan
| PR | Scope |
|---|---|
| 1 | Add DAO interfaces + `AppDatabase`/`RoomModule` wiring; keep `TreeTrackerDAO` |
| 2 | Migrate org, device config, user |
| 3 | Migrate session + tree |
| 4 | Migrate legacy planter + tree capture |
| 5 | Migrate location + sync/dashboard |
| 6 | Remove `TreeTrackerDAO`, split tests |

## Changes

### Infrastructure

- Add `database/dao/` (and `database/dao/legacy/` for legacy aggregates).
- Register each DAO on `AppDatabase`.
- Wire each DAO in `RoomModule` (Koin).
- Keep `TreeTrackerDAO` temporarily during migration, then remove it.

### Tests

- Split `TreeTrackerDaoTest` into per-DAO tests.
- Update ~17 test files that mock `TreeTrackerDAO` to mock the relevant DAO(s).

## Out of scope (next stacks)

- Room schema changes (DB stays at version 9).
- Repository layer introduction — this task is DAO + wiring + caller migration only.
- Merging `UserDAO` with `PlanterDAO` — different domain eras (v2 vs legacy).

## Verification

- `./gradlew :app:compileDebugKotlin :app:compileDebugUnitTestKotlin`
- `./gradlew :app:testDebugUnitTest`
- `./codeAnalysis.sh`
- No remaining references to `TreeTrackerDAO` after final PR in the stack.
