#!/usr/bin/env python3
"""Joins tb's recording of one flow into a single video and caption file.

On Android, tb (with --record) splits a recording longer than screenrecord's 3 minutes into
recording.mp4, recording-part2.mp4, ..., and with --annotate writes the captions of each beside
it (recording.srt, recording-part2.srt, ...). This joins the parts in order, with ffmpeg (no
re-encoding), and shifts each part's captions by the length of the parts before it.

Without ffmpeg (or ffprobe), or when joining fails, the first part is kept as OUT.mp4 and the
others as OUT-part2.mp4, ... with their own captions.

Usage: join_recording.py DEBUG_DIR OUT   (writes OUT.mp4 and, when tb wrote captions, OUT.srt)
"""
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

PART = re.compile(r"^recording(?:-part(\d+))?\.mp4$")
TIME = re.compile(r"(\d+):(\d\d):(\d\d)[,.](\d{3})")


def parts(debug_dir):
    found = []
    for p in Path(debug_dir).rglob("recording*.mp4"):
        m = PART.match(p.name)
        if m:
            found.append((int(m.group(1) or 1), p))
    return [p for _, p in sorted(found)]


def captions_for(video):
    srt = video.with_suffix(".srt")
    return srt if srt.exists() else None


def duration(video):
    out = subprocess.run(["ffprobe", "-v", "error", "-show_entries", "format=duration",
                          "-of", "default=nw=1:nk=1", str(video)],
                         check=True, capture_output=True, text=True).stdout
    return float(out.strip())


def shift(srt_text, seconds):
    def move(m):
        h, mi, s, ms = (int(x) for x in m.groups())
        t = round((h * 3600 + mi * 60 + s) * 1000 + ms + seconds * 1000)
        return f"{t // 3600000:02d}:{t // 60000 % 60:02d}:{t // 1000 % 60:02d},{t % 1000:03d}"
    return TIME.sub(move, srt_text)


def join_captions(videos, offsets):
    cues, n = [], 0
    for video, offset in zip(videos, offsets):
        srt = captions_for(video)
        if not srt:
            continue
        for block in re.split(r"\n\s*\n", srt.read_text(encoding="utf-8").strip()):
            lines = block.splitlines()
            if len(lines) >= 2 and TIME.search(lines[1]):
                n += 1
                cues.append("\n".join([str(n), shift(lines[1], offset), *lines[2:]]))
    return "\n\n".join(cues) + "\n" if cues else None


def keep_apart(videos, out):
    for i, v in enumerate(videos, 1):
        name = out if i == 1 else Path(f"{out}-part{i}")
        shutil.move(str(v), f"{name}.mp4")
        srt = captions_for(v)
        if srt:
            shutil.move(str(srt), f"{name}.srt")


def main():
    if len(sys.argv) != 3:
        print(__doc__, file=sys.stderr)
        sys.exit(2)
    videos, out = parts(sys.argv[1]), Path(sys.argv[2])
    if not videos:
        print("no recording", file=sys.stderr)
        sys.exit(1)
    out.parent.mkdir(parents=True, exist_ok=True)
    if len(videos) == 1:
        keep_apart(videos, out)
        return
    if not (shutil.which("ffmpeg") and shutil.which("ffprobe")):
        print("ffmpeg missing: keeping the recording's parts apart", file=sys.stderr)
        keep_apart(videos, out)
        return
    try:
        offsets, total = [], 0.0
        for v in videos:
            offsets.append(total)
            total += duration(v)
        with tempfile.NamedTemporaryFile("w", suffix=".txt", delete=False) as f:
            for v in videos:
                f.write("file '%s'\n" % str(v.resolve()).replace("'", "'\\''"))
            listing = f.name
        subprocess.run(["ffmpeg", "-loglevel", "error", "-y", "-f", "concat", "-safe", "0",
                        "-i", listing, "-c", "copy", f"{out}.mp4"], check=True)
        Path(listing).unlink()
    except (subprocess.CalledProcessError, ValueError) as e:
        print(f"joining the parts failed ({e}): keeping them apart", file=sys.stderr)
        Path(f"{out}.mp4").unlink(missing_ok=True)
        keep_apart(videos, out)
        return
    srt = join_captions(videos, offsets)
    if srt:
        Path(f"{out}.srt").write_text(srt, encoding="utf-8")
    for v in videos:
        v.unlink()
        if captions_for(v):
            captions_for(v).unlink()


if __name__ == "__main__":
    main()
