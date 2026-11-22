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

echo "== E3: 120 rounds; each round pulls a 1.2 MB file four ways"
adb -s "$SERIAL" shell 'dd if=/dev/urandom of=/sdcard/dbg.bin bs=1024 count=1200 2>/dev/null; cp /sdcard/dbg.bin /data/local/tmp/dbg.bin; ls -la /sdcard/dbg.bin /data/local/tmp/dbg.bin'
SUM=$(adb -s "$SERIAL" shell md5sum /data/local/tmp/dbg.bin | cut -d' ' -f1)
declare -A OK FAIL
T0=$(date +%s)
for i in $(seq 1 120); do
  for way in pull-sdcard pull-data cat-sdcard cat-data; do
    rm -f $OUT/dbg.bin
    case $way in
      pull-sdcard) adb -s "$SERIAL" pull /sdcard/dbg.bin $OUT/dbg.bin >$OUT/pull.txt 2>&1 ;;
      pull-data) adb -s "$SERIAL" pull /data/local/tmp/dbg.bin $OUT/dbg.bin >$OUT/pull.txt 2>&1 ;;
      cat-sdcard) adb -s "$SERIAL" exec-out cat /sdcard/dbg.bin >$OUT/dbg.bin 2>$OUT/pull.txt ;;
      cat-data) adb -s "$SERIAL" exec-out cat /data/local/tmp/dbg.bin >$OUT/dbg.bin 2>$OUT/pull.txt ;;
    esac
    CODE=$?
    if [[ $CODE == 0 && $(md5sum < $OUT/dbg.bin | cut -d' ' -f1) == "$SUM" ]]; then
      OK[$way]=$(( ${OK[$way]:-0} + 1 ))
    else
      FAIL[$way]=$(( ${FAIL[$way]:-0} + 1 ))
      echo "  round $i $way FAILED at +$(( $(date +%s) - T0 ))s: exit $CODE, got $(stat -c %s $OUT/dbg.bin 2>/dev/null || echo 0) bytes: $(tr '\r' '\n' < $OUT/pull.txt | tail -1)"
      adb -s "$SERIAL" wait-for-device
    fi
  done
done
for way in pull-sdcard pull-data cat-sdcard cat-data; do echo "  $way: ${OK[$way]:-0} ok, ${FAIL[$way]:-0} failed"; done
grep -c 'connection terminated' "$ADBLOG" | sed 's/^/  host adb log: connections terminated so far: /'

echo "== E1: tb test --record --annotate, as run-flows.sh does; 12 times"
LOST=0
for i in 1 2 3 4 5 6 7 8 9 10 11 12; do
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
echo "E1: lost $LOST of 12"
echo "== adb server log (last 60 lines)"; tail -60 "$ADBLOG" 2>&1
pgrep -a adb
