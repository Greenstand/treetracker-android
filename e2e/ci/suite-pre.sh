# TEMP: candidate mitigation M1. A short tb session before the suite, so the suite is not tb's
# first session after boot.
"$TB" test e2e/flows/first-screen.yaml -p android --device "$SERIAL" \
  --debug-output "$OUT/warmup" </dev/null > "$OUT/warmup-console.txt" 2>&1 || true
