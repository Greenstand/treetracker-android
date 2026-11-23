# Code Conventions

## Do not add comments

Kotlin files (`*.kt`, `*.kts`) contain no comments: no line comments, no block comments, no KDoc,
no commented-out code, no `TODO` or `FIXME` notes, no `// region` markers.

**Why.** Nothing checks a comment. The compiler, the tests and the linters all pass when a
comment is wrong, so comments drift away from the code they describe and then mislead the next
reader, human or agent.

**What to do instead.**

| You want to say | Put it here |
| --- | --- |
| What the code does | Names: rename the function, variable or type, or extract one with a name that says it |
| What the code must keep doing, and its edge cases | A test whose name states the behaviour |
| Why the change was made, what was tried, what was ruled out | The PR description and the task notes in `docs/tasks/task_{id}/` |
| How a part of the system works across files | A page under `docs/`, linked from the [index](../README.md) |
| Something left to do | A GitHub issue |
| Code that is no longer used | Delete it. Git keeps it |
| A linter or compiler warning to silence | An annotation: `@Suppress("...")`, `@SuppressLint("...")`, `@Language("...")` |

### Exceptions

There are two, and CI knows both.

1. **KDoc that a compiler turns into shipped output.** The AppFunctions compiler copies the KDoc
   of a declaration annotated with `isDescribedByKDoc = true` into the function and parameter
   descriptions that agents read (see [Agent App Functions](../testing/agent-app-functions.md)).
   Removing that KDoc empties the descriptions. Such a declaration keeps its KDoc and carries
   `@Suppress("ForbiddenComment")` next to the annotation. Today that is `createAccount` and
   `captureTrees` in `AgentAppFunctions.kt`, and `AgentAccount` and `AgentCapturedTrees` in
   `AgentResults.kt`. The KDoc there describes the function to its caller; it does not explain
   the implementation.
2. **The licence header** at the top of each file. It is a legal notice, not an explanation.
   `./gradlew spotlessApply` writes it from `spotless/copyright.kt`.

Tool directives written as comments (`// ktlint-disable`, `//noinspection`, `// language=`) are
not an exception: none is needed for a green build, and each has an annotation form.

### Enforcement

`./gradlew detekt` fails on any comment in `app/src/main`, `app/src/test`, `app/src/debug` and
`app/src/testDebug`. It runs in the `code analysis` job of every pull request. The rule is
detekt's `ForbiddenComment`, set in `detekt.yml` to match every comment; its `allowedPatterns`
lets the licence header through, and `@Suppress("ForbiddenComment")` on a declaration lets that
declaration's KDoc through.

Do not add `@Suppress("ForbiddenComment")` to keep an ordinary comment. It is only for exception
1.

### Scope

The rule covers Kotlin. YAML flows, XML, shell and Python scripts, Gradle files and Markdown keep
their comments, and some need them: an end-to-end flow starts with a header comment that says
what it guards.

[Task 117](../tasks/task_117/README.md) has the tool that removed the existing comments, the
proof that nothing else changed, and how to run it again.
