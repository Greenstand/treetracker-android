#!/usr/bin/env bash
set -euo pipefail

here="$(cd "$(dirname "$0")" && pwd)"
root="$(git -C "$here" rev-parse --show-toplevel)"
kotlin="$(sed -n 's/^kotlin = "\(.*\)"$/\1/p' "$root/gradle/libs.versions.toml")"
cache="${KOTLIN_COMMENTS_CACHE:-${TMPDIR:-/tmp}/kotlin-comments-$kotlin}"
maven="https://repo1.maven.org/maven2/org/jetbrains/kotlin"
mkdir -p "$cache/classes"

for artifact in kotlin-compiler-embeddable kotlin-stdlib; do
  jar="$cache/$artifact-$kotlin.jar"
  if [ ! -s "$jar" ]; then
    curl -sSfL --retry 5 --retry-delay 20 --retry-all-errors -o "$jar.part" "$maven/$artifact/$kotlin/$artifact-$kotlin.jar"
    mv "$jar.part" "$jar"
  fi
done
classpath="$cache/kotlin-compiler-embeddable-$kotlin.jar:$cache/kotlin-stdlib-$kotlin.jar"

if [ ! -f "$cache/classes/KotlinComments.class" ] || [ "$here/KotlinComments.java" -nt "$cache/classes/KotlinComments.class" ]; then
  javac -cp "$classpath" -d "$cache/classes" "$here/KotlinComments.java"
fi
tool() { java -cp "$classpath:$cache/classes" KotlinComments "$@"; }

cd "$root"
mapfile -t files < <(git ls-files '*.kt' '*.kts')
command="${1:-}"
shift || true

case "$command" in
  inventory | check | strip)
    tool "$command" "$@" "${files[@]}"
    ;;
  verify)
    ref="${1:?usage: kotlin-comments.sh verify <git-ref>}"
    before="$(mktemp -d)"
    trap 'rm -rf "$before"' EXIT
    mapfile -t both < <(git ls-tree -r --name-only "$ref" -- "${files[@]}")
    git archive "$ref" -- "${both[@]}" | tar -x -C "$before"
    tool verify "$before" "$root" "${both[@]}"
    ;;
  *)
    echo "usage: kotlin-comments.sh inventory | strip [--keep-headers] | check [--keep-headers] | verify <git-ref>" >&2
    exit 2
    ;;
esac
