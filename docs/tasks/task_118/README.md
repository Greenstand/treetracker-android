# Task 118 - Typed text lost or doubled in the end-to-end check (E10, B4)

## Problem

Since Test Buddy v0.6.3 (#1457) the "Android end-to-end" check failed on unrelated PRs in two
flows, with `inputText` steps that passed although the text was wrong:

- E10 "Edit profile name": "Grace" never reached the first name field (step passed, tb logged
  `typed: nothing changed`); the app then refused to save an empty name.
- B4 "Add a note on review": the note read "OOak by the gate".

In the latest 100 runs of the check: B4 failed this way 0 of 22 times on the old tb pin and 2 of
14 on the new one; E10 6 of 11 on the new one (it barely ran on the old one).

## Cause

Not the app, and not a slow runner. It is the state Test Buddy's session starts in.

1. **tb's accessibility service is off for the whole run, about half the time.** On a freshly
   booted emulator tb installs its helper app at the start of its first session and at once
   writes the setting that enables the helper's accessibility service. The system has often not
   finished registering the package and drops the setting. tb does not check. Device state 45 s
   into such a run: `Enabled services:{}`, `Bound services:{}`,
   `enabled_accessibility_services = null`; in a good run the service is listed and bound. The
   device log shows the setting changed twice within 23 ms, 100 ms before the package was added.
   Without the service a Compose app sends no accessibility events: every tap waits its full
   second (this is the "slow runner" with ~13 s sign-ups in the CI history), and tb cannot find
   the focused field. The check runs every flow in one tb process, so the state lasts the whole
   run. This part is the same in the old tb pin.
2. **In that state tb types without checking, and passes the step.** `inputText` falls back to
   a path (`internal/engine/input.go`, `typeFirstKey`) that types the text blind when it sees no
   focused field, types the first key a second time after 1 s without a visible change, and
   returns success whatever the field holds. Keys sent while the keyboard is still attaching to
   a field that has just taken focus are dropped or arrive late, so the text is lost (E10) or
   its first key doubled (B4).

What is inferred rather than shown: that the lost keys are dropped by Android while the keyboard
connects (tb's own comments say so; not observed directly), and why the old tb pin typed
correctly in the same state (its agent's typing code is a quarter of the size; not traced).

## Change

- `e2e/warm-up-tb.sh`, called by `e2e/run.sh` and `e2e/ci/run-flows.sh`: when tb's helper is not
  installed yet, one short tb session (`first-screen`) runs first. The next session finds the
  helper installed, only writes the setting, and it stays. Nothing happens when the helper is
  already there (a developer's emulator, later Flow runner passes).
- Flows assert the typed text in its field right after `inputText`, where a later screen reads
  the text back (E10, B4, the org note flows, the tree note in E4, the wallet name) and for the
  sign-up names. A lost or doubled key now fails at the typing step with the field's content in
  the error. Nothing a flow checked before was removed.
- The pinned tb version is unchanged: the old pin has the same service race (cause 1).
- `upstream-issue.md`: the text of two issues for Test Buddy (not filed).

## Evidence

A harness on a throwaway branch (`claude/e2e-typing-baseline`): 12 copies each of E10 and B4, run
by the Flow runner on CI's emulator.

| Setup | Runs | Runs in the bad state | Flows failed |
|---|---|---|---|
| One tb process per flow (the Flow runner as it is), master | 2 | first flow only, in both | 1 of 48 (B4 "OOak", a first flow) |
| The same with 4 busy loops on the runner's 4 cores (sign-up 15-39 s) | 2 | 0 | 0 of 48 |
| All 24 in one tb process straight after boot (as the check runs), master | 8 | 5 (all 24 flows each) | 70 of 192, all in the 5 bad runs |
| The same, with the warm-up and the assertions | 8 | 0 | 0 of 192 |
| (earlier form of the warm-up, without the assertions) | 6 | 0 | 0 of 144 |

In the 8 runs with the fix the accessibility service was enabled 45 s into every suite, and in 4
of them the warm-up session itself was the one that started without it.

Runs of the real check on this PR are listed in the PR description.

## Not done

- No fix in Test Buddy (see `upstream-issue.md`).
- `e2e/tour/tour.sh` drives tb through `tb open`/`tb do`, not `tb test`; it was not looked at.
