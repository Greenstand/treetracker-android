# Task 118 - No comments in Kotlin files: the rule, its enforcement, and the removal

## Problem

The maintainer wants one rule, "Do not add comments", written where agents and contributors
read it, and every existing comment in Kotlin files removed without changing behaviour.

## Change

- **The rule** is in `CLAUDE.md`/`AGENTS.md` (one file, symlinked) and in the new
  `docs/engineering/code-conventions.md`, linked from both indexes: the reason, what to do
  instead, the exceptions, enforcement and scope. The PR template has a checkbox for it.
  `docs/testing/agent-app-functions.md` and the `agent-app-functions` skill say that the
  AppFunctions KDoc is the exception.
- **Enforcement** is detekt's `ForbiddenComment`, in the `code analysis` job that already runs
  `./gradlew detekt` on every PR. No new job, step or download. See below.
- **The removal** was done by `tools/kotlin-comments.sh`, not by hand and not by regular
  expressions.

## The tool

`tools/KotlinComments.java` uses `KotlinLexer` from `kotlin-compiler-embeddable`, at the Kotlin
version in `gradle/libs.versions.toml`. A comment is whatever the Kotlin lexer says is a comment
token (`EOL_COMMENT`, `BLOCK_COMMENT`, `DOC_COMMENT`, `SHEBANG_COMMENT`), so `//` in a URL or
`/*` in a string literal is never touched. `tools/kotlin-comments.sh` downloads the two jars once
(to `$KOTLIN_COMMENTS_CACHE`, default a temp directory), compiles the tool and runs it over
every tracked `*.kt` and `*.kts` file. It needs a JDK, `curl` and `git`.

| Command | What it does |
| --- | --- |
| `kotlin-comments.sh inventory` | One line per comment: file, line, kind, class, text. Counts on stderr |
| `kotlin-comments.sh strip [--keep-headers]` | Removes comments in place. A comment alone on its lines takes the lines with it; a trailing comment takes the spaces before it |
| `kotlin-comments.sh verify <git-ref>` | Compares each file with the same file at `<git-ref>` and fails on any difference outside comments and whitespace |
| `kotlin-comments.sh check [--keep-headers]` | Lists the comments that `strip` would remove and fails if there are any |

`strip` always keeps KDoc on a declaration annotated `isDescribedByKDoc = true`. With
`--keep-headers` it also keeps a comment that is the first token of the file and mentions
copyright or a licence.

## Inventory (master at `0303c06`)

`inventory-before.tsv` has all 1,349 comments with their text, so nothing that was removed is
lost. There are no `*.kts` files in the repository: the build scripts are Groovy.

| Kind | Count | What happened |
| --- | --- | --- |
| Licence header (block comment, first token of the file) | 510 | Kept. See the open question |
| KDoc read by the AppFunctions compiler | 4 | Kept, as a named exception |
| Other KDoc | 298 | Removed |
| Line comments | 479 | Removed |
| Block comments | 4 | Removed. Three were commented-out code |
| `// region` and `// endregion` | 54 | Removed |

By source set, without headers: main 450, test 357, debug 27, testDebug 5.

### Comments that are not inert

- **Tool directives.** None of `ktlint-disable`, `noinspection`, `detekt`, `language=` or
  `@formatter` exists in a Kotlin file. The 54 region markers only fold code in an IDE; no build
  step reads them, so they went. No directive is kept, and the rule lists none.
- **KDoc that feeds generated output.** `createAccount` and `captureTrees`
  (`@AppFunctionDeclaration(isDescribedByKDoc = true)`) and `AgentAccount` and
  `AgentCapturedTrees` (`@AppFunctionSerializable(isDescribedByKDoc = true)`). Checked by
  running `:app:kspDebugKotlin` with and without these four KDocs: without them, every
  `description` in the generated `$AgentAppFunctions_AppFunctionInventory.kt`,
  `$App_SerializableComponentRegistry.kt` and `agent_app_functions.xml` becomes `""`. With them
  kept, the KSP output of this branch is byte-identical to master's (`diff -r`, 0 lines), so
  `list-app-functions.expected.txt` stays as it is. The library has no annotation parameter for
  a description, so the text stays in KDoc. The other 13 KDocs under `app/src/debug` were
  ordinary and went.
- **Comments a linter counted.** Two functions held one `return` and a comment. ktlint's
  `function-expression-body` rule skips a body with a comment in it, so with the comment gone it
  asked for `= x` in place of `{ return x }`: `RouteRegistry.resolveNoArgRoute` and
  `ValidationUtils.filterNameInput`. That rewrite (ktlintFormat's own) is a separate commit.
  detekt's empty-block rules also accept a block that holds only a comment; none fired here.
- **Licence headers.** Every file starts with an Apache-2.0 notice, "Copyright 2023 Treetracker"
  (268 files) or "Copyright 2026 Treetracker" (241 files), plus the template
  `spotless/copyright.kt`. The repository's `LICENSE` is AGPL-3.0, so the headers and the
  licence disagree today. No CI job checks the headers: Spotless is configured with
  `licenseHeaderFile`, but `spotlessCheck` runs in no workflow. `./codeAnalysis.sh` runs
  `spotlessApply`, which writes the header into files that lack it.
- **TODO and FIXME.** There are none in Kotlin files (lexer inventory and a case-insensitive
  search agree). Commented-out code: three block comments in `ObjectStorageClient.kt` (two) and
  `DashboardScreen.kt`, plus scattered line comments; all are in `inventory-before.tsv`.

## Proof that only comments changed

`verify` lexes the file before and after and compares, for every token that is neither a comment
nor whitespace, its type, its text and whether a line break comes before it. The last part is
stricter than a plain token comparison: Kotlin ends statements at line breaks, and a removed
multi-line comment could otherwise join two lines unnoticed.

At the removal commit, against the commit before it:

```
$ docs/tasks/task_118/tools/kotlin-comments.sh verify HEAD~1
verified 510 files, 400536 significant tokens, 0 failures
$ docs/tasks/task_118/tools/kotlin-comments.sh check --keep-headers
0 comments in 510 files
```

The check does fail when it should: run right after `ktlintFormat`, it reported the two files
above (`DIFFERENT ... first difference at significant token 172: LBRACE vs EQ`). Those two are
the only Kotlin files whose tokens differ from master in the whole branch, besides the four
`@Suppress("ForbiddenComment")` annotations in the two AppFunctions files.

Blank lines: `./gradlew ktlintFormat` ran after `strip` and did only what `ktlintCheck`
requires (doubled blank lines, blank lines at the start of a block).

## Checks

Local, JDK 21, no emulator:

- `./gradlew ktlintCheck detekt`: passes (it also passed on master before the change).
- `./gradlew test` (as CI runs it): passes. debug 2,747 tests; beta, dev, prerelease and release
  1,878 each. The first run had four classes fail on Robolectric not being able to download an
  Android runtime jar; the next run passed.
- `./gradlew :app:assembleDebug`: passes.
- `:app:kspDebugKotlin` output identical to master's.
- `test_app_fn.sh` (125 passed), the `pr-validating-flow` skill tests and the `e2e/ci` tests.

Left to CI: screenshot tests (`verifyRoborazziDebug`) and the Android end-to-end flows.

## Enforcement

`detekt.yml` sets `ForbiddenComment` to one pattern, `\A`, which matches every comment: line,
block and each KDoc section. The message tells the author what to do instead.

- The licence header passes through `allowedPatterns` (a comment that starts with the header's
  first lines).
- A KDoc that feeds generated output passes because its declaration carries
  `@Suppress("ForbiddenComment")`.
- `build.gradle` adds `src/debug/java` and `src/testDebug/java` to detekt's sources. detekt
  skipped them before, which is where the AppFunctions are. No other detekt rule fires there.

Tried: on master's sources this configuration reports 848 findings and no header: 450 in main
and 357 in test, which is exactly the inventory's count there, and 36 in debug and 5 in
testDebug (27 and 5 comments; a KDoc with tags is reported once per section). On this branch it
reports none. One planted comment of each kind, one per source set (line in
main, block in test, KDoc in debug, trailing in testDebug), fails the task with four findings.

What it does not cover: `*.kts` files (the repo has none; detekt does not read build scripts)
and Kotlin outside `app/src/{main,test,debug,testDebug}`. `kotlin-comments.sh check` covers
every tracked Kotlin file and can be run by hand.

## Size, conflicts and how to run it again

245 Kotlin files changed by the removal, 1,670 lines removed and 22 changed (lines that lost a
trailing comment). The whole branch: 257 files.

Every open PR that touches Kotlin will conflict wherever it edits near a removed comment. Do
not resolve those by hand:

- **A PR that merges before this one.** Regenerate this branch on the new master: keep the tool
  and rule commits, drop the two removal commits, then

  ```bash
  docs/tasks/task_118/tools/kotlin-comments.sh strip --keep-headers
  ./gradlew ktlintFormat
  docs/tasks/task_118/tools/kotlin-comments.sh verify origin/master
  ./gradlew ktlintCheck detekt
  ```

  `verify` names any file where ktlintFormat rewrote code, as it did for the two above; look at
  those by eye and commit them separately.
- **A PR that is still open after this one merges.** On the PR's branch, before merging master:

  ```bash
  git checkout origin/master -- docs/tasks/task_118/tools
  docs/tasks/task_118/tools/kotlin-comments.sh strip --keep-headers
  ./gradlew ktlintFormat
  git commit -am "Remove comments (task 118 tool)"
  git merge origin/master
  ```

  Both sides have then made the same deletions, so what is left to resolve is the PR's own
  change. New comments the PR adds are removed by the same run.

Suggested order: merge the small fix PRs that are ready (#1434, #1435, #1443, #1444, #1451)
first, regenerate this branch and merge it soon after, and use the second recipe on the
#1334-#1386 refactor series, one PR at a time from the bottom of the stack. That series moves
files into modules and replaces the root `subprojects` block (where the `source.from` line for
detekt lives) with convention plugins, and converts build files to `*.kts`; the tool follows
moved files by itself, and the detekt source line has to move into the convention plugin.

## Merge of master on 27 Nov

Master gained #1470 (two integration test files and one UI test), whose notes took
`docs/tasks/task_117`, so this task moved to `task_118`. The merge was redone with the tool, as
described above: `strip --keep-headers` removed the 9 comments the new files brought (3 KDoc, 6
region markers), `ktlintFormat`, then

```
$ docs/tasks/task_118/tools/kotlin-comments.sh verify origin/master
verified 513 files, 404918 significant tokens, 4 failures
```

The 4 are the files this branch changes on purpose: the two expression bodies and the two files
with `@Suppress("ForbiddenComment")`. The other 509, including the new ones, are identical to
master outside comments. `ktlintCheck`, `detekt` and the three new test classes pass.

## Open question: licence headers

Not decided here. Two options for the maintainer:

- **Keep** (this branch): the header is exception 2 in the rule.
- **Remove**: one more commit, `kotlin-comments.sh strip` without `--keep-headers`, which also
  has to delete the `licenseHeaderFile` block in `build.gradle` and `spotless/copyright.kt`
  (otherwise `codeAnalysis.sh` writes the headers back), drop `allowedPatterns` in `detekt.yml`
  and exception 2 in the conventions page. It touches all 510 files.

The Apache-2.0 headers against an AGPL-3.0 `LICENSE` is a separate legal question.

## Not verified

- Screenshot tests and end-to-end flows were not run locally (no emulator); they run on the PR.
- Nothing was installed on a device, so `list-app-functions` was not re-captured. The generated
  metadata it is built from is identical to master's.
- Markdown pages keep Kotlin snippets with comments (`docs/engineering/architecture.md`). The
  rule is about source files; the snippets were left alone.

## Should the rule extend past Kotlin?

Not as it stands. YAML flows, workflow files, shell and Python scripts and Gradle files use
comments for things with no other home (a flow's header, why a CI step exists), and those
languages have no names-and-tests alternative of the same strength. If the build moves to
`*.kts`, those files fall under the rule and the tool, but not under detekt.
