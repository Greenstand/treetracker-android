#!/usr/bin/env python3
"""Run beta's opt-in camera test on an emulator; restore display settings afterward."""
import argparse
import json
from pathlib import Path
import re
import shutil
import subprocess
import time


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--serial', required=True, help='Disposable emulator serial')
    parser.add_argument('--adb', default=shutil.which('adb') or str(Path.home() / 'Library/Android/sdk/platform-tools/adb'))
    parser.add_argument('--output', type=Path, default=Path('app/build/reports/beta-camera-device.txt'))
    args = parser.parse_args()
    if not args.serial.startswith('emulator-'):
        parser.error('Use a disposable emulator, not a personal device')
    prefix = [args.adb, '-s', args.serial]
    package = 'org.greenstand.android.TreeTracker.test'

    def call(*command, check=True):
        return subprocess.run(prefix + list(command), check=check, capture_output=True, text=True, timeout=20)

    def read(*command):
        return call(*command).stdout.strip()

    def override(kind):
        match = re.search(r'Override ' + kind + r':\s*(\S+)', read('shell', 'wm', kind.lower()))
        return match.group(1) if match else 'reset'

    original = {'size': override('size'), 'density': override('density')}
    for name in ['accelerometer_rotation', 'user_rotation']:
        original[name] = read('shell', 'settings', 'get', 'system', name)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.with_suffix('.display.json').write_text(json.dumps(original, indent=2))
    process = None
    try:
        call('install', '-r', 'app/build/outputs/apk/androidTest/beta/app-beta-androidTest.apk')
        call('shell', 'pm', 'grant', package, 'android.permission.CAMERA')
        call('shell', 'wm', 'size', '1920x1200')
        call('shell', 'wm', 'density', '160')
        call('shell', 'settings', 'put', 'system', 'accelerometer_rotation', '0')
        call('shell', 'settings', 'put', 'system', 'user_rotation', '0')
        for rotation in ['0', '2']:
            for name in ['ready', 'rotated']:
                call('shell', 'run-as', package, 'rm', '-f', f'files/camera-{name}-{rotation}')
        with args.output.open('w') as log:
            process = subprocess.Popen(prefix + [
                'shell', 'am', 'instrument', '-w', '-r', '-e', 'class',
                'org.greenstand.android.TreeTracker.BetaCameraRotationTest',
                '-e', 'betaCameraRotation', 'true',
                package + '.test/androidx.test.runner.AndroidJUnitRunner',
            ], stdout=log, stderr=subprocess.STDOUT)
            for rotation in ['0', '2']:
                deadline = time.monotonic() + 50
                while process.poll() is None:
                    if call('shell', 'run-as', package, 'test', '-f', f'files/camera-ready-{rotation}', check=False).returncode == 0:
                        call('shell', 'settings', 'put', 'system', 'user_rotation', rotation)
                        call('shell', 'run-as', package, 'touch', f'files/camera-rotated-{rotation}')
                        break
                    if time.monotonic() >= deadline:
                        raise TimeoutError(f'Camera never became ready for rotation {rotation}')
                    time.sleep(0.2)
            process.wait(timeout=45)
    finally:
        if process is not None and process.poll() is None:
            process.terminate()
            process.wait(timeout=10)
        call('shell', 'wm', 'size', original['size'])
        call('shell', 'wm', 'density', original['density'])
        for name in ['accelerometer_rotation', 'user_rotation']:
            if original[name] == 'null':
                call('shell', 'settings', 'delete', 'system', name)
            else:
                call('shell', 'settings', 'put', 'system', name, original[name])
    result = args.output.read_text()
    print(f'Display settings restored. Device results: {args.output}')
    if 'OK (1 test)' not in result:
        raise SystemExit('Native camera device test failed; inspect the report')
    print('Native camera device test passed.')


if __name__ == '__main__':
    main()
