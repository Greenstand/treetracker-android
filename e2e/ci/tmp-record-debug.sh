#!/usr/bin/env bash
# TEMPORARY (debug PR, not for merging): why does tb's --record lose the video on CI?
set -uo pipefail
TB=${TB:-tb}
SERIAL=$(adb get-serialno)
OUT=build/dbg; rm -rf $OUT; mkdir -p $OUT
ADBLOG=${TMPDIR:-/tmp}/adb.$(id -u).log
echo "== environment"
which -a adb; adb version; echo "ANDROID_HOME=${ANDROID_HOME:-} ANDROID_SDK_ROOT=${ANDROID_SDK_ROOT:-}"
ls -la "${ANDROID_HOME:-/none}/platform-tools/adb" 2>&1
pgrep -a adb; ls -la "$ADBLOG" 2>&1; adb devices -l
adb -s "$SERIAL" shell 'screenrecord --help 2>&1 | grep -i -A1 time-limit; getprop ro.build.fingerprint; df -h /sdcard | tail -1'
mark() { wc -l < "$ADBLOG" 2>/dev/null || echo 0; }
since() { tail -n +"$(( $1 + 1 ))" "$ADBLOG" 2>/dev/null | head -30 | sed 's/^/    adb.log: /'; }
drops() { adb -s "$SERIAL" logcat -d 2>/dev/null | grep -a -E 'adbd +: .*(terminated|offline|spawning)|ScreenRecord: success' | tail -6 | sed 's/^/    dev: /'; }
adb -s "$SERIAL" shell cmd connectivity airplane-mode enable
adb -s "$SERIAL" shell svc wifi disable
cat > e2e/flows/.dbg.yaml <<'Y'
appId: org.greenstand.android.TreeTracker.debug
name: dbg
---
- launchApp: {clearState: true}
- extendedWaitUntil:
    visible: {id: LanguageOption_ENGLISH}
    timeout: 30000
- tapOn: {id: LanguageOption_ENGLISH}
- tapOn: {id: ArrowForwardButton}
- tapOn: {id: PrivacyPolicyCloseButton}
- tapOn: {id: PhoneCredentialButton}
- tapOn: {id: PhoneField}
- inputText: "5551230001"
- hideKeyboard
- takeScreenshot: end
Y

echo "== E3: plain adb pull of a 1.2 MB file, 10 times each from /sdcard and /data/local/tmp"
adb -s "$SERIAL" shell 'dd if=/dev/urandom of=/sdcard/dbg.bin bs=1024 count=1200 2>/dev/null; cp /sdcard/dbg.bin /data/local/tmp/dbg.bin'
for where in /sdcard /data/local/tmp; do
  ok=0
  for i in 1 2 3 4 5 6 7 8 9 10; do
    if adb -s "$SERIAL" pull $where/dbg.bin $OUT/dbg.bin >$OUT/pull.txt 2>&1; then ok=$((ok+1)); else echo "  $where pull $i FAILED: $(tr '\r' '\n' < $OUT/pull.txt | tail -2)"; sleep 2; fi
  done
  echo "  $where: $ok/10 pulled"
done

echo "== E2: plain screenrecord (no tb), stop with SIGINT, pull at once; 6 times"
for i in 1 2 3 4 5 6; do
  adb -s "$SERIAL" logcat -c; M=$(mark)
  adb -s "$SERIAL" shell 'rm -f /sdcard/dbg-rec.mp4'
  adb -s "$SERIAL" shell 'screenrecord --verbose --time-limit 0 --bit-rate 4000000 /sdcard/dbg-rec.mp4' >$OUT/rec.txt 2>&1 &
  REC=$!
  sleep 3
  adb -s "$SERIAL" shell am start -n org.greenstand.android.TreeTracker.debug/org.greenstand.android.TreeTracker.activities.TreeTrackerActivity >/dev/null 2>&1
  adb -s "$SERIAL" shell 'for n in 1 2 3 4 5 6 7 8; do input swipe 100 400 100 200 200; input keyevent 4; done' >/dev/null 2>&1
  sleep 10
  adb -s "$SERIAL" shell "pkill -INT -f '^[^ ]*screenrecord .*dbg-re[c]'"
  wait $REC
  if adb -s "$SERIAL" pull /sdcard/dbg-rec.mp4 $OUT/rec-$i.mp4 >$OUT/pull.txt 2>&1; then
    echo "  E2 run $i: pulled $(stat -c %s $OUT/rec-$i.mp4) bytes"
  else
    echo "  E2 run $i: PULL FAILED: $(tr '\r' '\n' < $OUT/pull.txt | tail -2)"; drops; since "$M"
    sleep 2
    adb -s "$SERIAL" pull /sdcard/dbg-rec.mp4 $OUT/rec-$i.mp4 >$OUT/pull.txt 2>&1 && echo "    second pull worked: $(stat -c %s $OUT/rec-$i.mp4) bytes" || echo "    second pull failed too"
  fi
done

echo "== E1: tb test --record --annotate, as run-flows.sh does; 8 times"
LOST=0
for i in 1 2 3 4 5 6 7 8; do
  adb -s "$SERIAL" logcat -c; M=$(mark)
  "$TB" test e2e/flows/.dbg.yaml -p android --device "$SERIAL" --debug-output $OUT/tb-$i --flatten-debug-output --record --annotate </dev/null >$OUT/console-$i.txt 2>&1
  CODE=$?
  if grep -q -- '--record: the recording' $OUT/console-$i.txt; then
    LOST=$((LOST+1))
    echo "  E1 run $i (exit $CODE): LOST: $(grep -- '--record:' $OUT/console-$i.txt)"; drops; since "$M"
  else
    echo "  E1 run $i (exit $CODE): $(find $OUT/tb-$i -name 'recording*.mp4' -printf '%f %s bytes ')"
    adb -s "$SERIAL" logcat -d | grep -a -E 'MPEG4Writer: final' | tail -1 | sed 's/^/    dev: /'
  fi
done
echo "E1: lost $LOST of 8"
echo "== adb server log (last 60 lines)"; tail -60 "$ADBLOG" 2>&1
pgrep -a adb
