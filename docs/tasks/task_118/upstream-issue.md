# Issue text for Elforama/testbuddy (not filed)

Two issues. The first is the cause of the CI failures; the second is why they showed up as
passed steps.

## 1. Android: the helper's accessibility service is silently left off when the helper is installed in that session, seconds after boot

**Version:** v0.6.3 (447f360), agent 0.1.67. API 35 `google_apis` x86_64 emulator on GitHub
Actions (`reactivecircus/android-emulator-runner`, booted from a cached snapshot, `tb test`
started within a minute of boot, load average 13-18). The mechanism is the same code at ab598c2.

**What happens.** `startAgent` (`internal/driver/android/android.go`, `serviceSetup`) runs, in one
shell command: `pm install -r -t <helper>`, then at once
`settings put secure enabled_accessibility_services dev.testbuddy.agent/.NoopService` and
`settings put secure accessibility_enabled 1`. On a device that is still busy,
AccessibilityManagerService has not yet registered the new package when the setting is written,
and it removes the unknown component from the setting. Nothing checks afterwards, and nothing
writes it again for that tb process (`adopt` only writes it on a new connection).

From a run where it happened (device logcat, then state 45 s into the session):

```
05:04:04.413 BackupManagerService: restoreAtInstall pkg=dev.testbuddy.agent
05:04:04.545 Settings changed for uri: content://settings/secure/enabled_accessibility_services
05:04:04.568 Settings changed for uri: content://settings/secure/enabled_accessibility_services
05:04:04.575 Settings changed for uri: content://settings/secure/accessibility_enabled
05:04:04.646 PackageUpdatedTask: Package updated: mOp=ADD packages=[dev.testbuddy.agent]
...
dumpsys accessibility:  Bound services:{}   Enabled services:{}
settings get secure enabled_accessibility_services  ->  null
ps: tb-agent running, no dev.testbuddy.agent process
```

In a run where it did not happen, the same dump shows
`Enabled services:{{dev.testbuddy.agent/dev.testbuddy.agent.NoopService}}` and the service bound.

**Effect.** The whole `tb test` process runs without the service: a Compose app sends no
accessibility events, so every tap logs `nothing changed (sent in N ms, waited 1000 ms)`, each
flow logs `the screen changed without the device reporting it`, and every `inputText` on a field
that has just taken focus logs `the focused field is unavailable through the device focus API:
using screen verification` (issue 2). Nothing says the service is off.

**How often.** With 24 flows in one `tb test` run straight after boot: 5 of 8 runs were in this
state for the whole run (every one of the 24 flows), 3 of 8 were not at all. With one `tb test`
process per flow, only the first flow after boot was ever affected (later sessions find the helper
installed, skip `pm install`, and the setting stays).

**Asked for.** After enabling the service, wait until it is bound (for example until
`NoopService.onServiceConnected` has run, which the agent can report in `hello`/`info`, or until
`dumpsys accessibility` lists it), writing the setting again if it was dropped; and if it still is
not bound, say so once in the log and in `--json` ("accessibility service not running: waits and
typing checks are degraded") instead of running silently without it.

**Workaround in use.** One short `tb test` session before the real run when
`pm path dev.testbuddy.agent` is empty (treetracker-android `e2e/warm-up-tb.sh`).

## 2. `inputText` passes when nothing reached the field, and can type the first key twice (host fallback)

`internal/engine/input.go`, `typeText` -> `typeFirstKey`, reached when the agent answers
`verifyUnavailable` (no editable node from `findFocus(FOCUS_INPUT)`):

- When the screen read finds no focused editable field, `typeFirstKey` returns the whole text
  ("no field to watch: type as before") and `typeText` types it with no check. The result of
  `Drv.TypeText` is only traced (`typed: nothing changed (...)`); the step passes.
- When it does find one, the first key is typed, and typed **again** after 1 s without a visible
  change, on elapsed time alone. Issue #299 removed exactly this from the agent's path ("typed
  again only when it provably did not land"); the host fallback still does it.
- If the field never changes: `return rest, nil // still unchanged`, and the step passes.

Seen, all in sessions in the state of issue 1 (so the field's change was reported late or never):

```
19.901s ▶ Input text Grace
19.901s   typing 5 characters
19.905s   the focused field is unavailable through the device focus API: using screen verification
20.605s   typed: nothing changed (sent in 6 ms, waited 300 ms)
20.605s ✓ Input text Grace  703 ms            <- the field stayed empty
```

```
37.425s   typing 15 characters
37.430s   the focused field is unavailable through the device focus API: using screen verification
38.800s   the first key did not reach the field (it was still connecting to the keyboard): typing it again
39.537s   typed: nothing changed (sent in 63 ms, waited 300 ms)
39.538s ✓ Input text Oak by the gate  2301 ms  <- the field holds "OOak by the gate"
```

The agent's own path, in the same sessions, fails the step properly ("the field did not show the
first key ("H") within 3000 ms ... so the key was not typed again").

Also: in CI mode `typeChecked` waits up to `FOCUS_WAIT_MS` for focus only when **no** node has
input focus. In the logs above the fallback starts 4-5 ms after typing begins, so a node that is
not editable held input focus right after the tap and there was no wait. (Read from the timing
and the code, not confirmed on a device.)

**Asked for.** In the fallback: no second first key without proof it was lost; and when a focused
editable field was seen before typing and its text is unchanged after it, fail the step as the
agent path does. When no focused field can be seen at all, at least a warning on the step
(`log:` line and `--json`) that the text was typed unverified.
