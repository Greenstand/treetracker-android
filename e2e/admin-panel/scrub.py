import os
import sys


def secrets():
    return [v.encode() for k in ("DEV_ADMIN_PASSWORD", "PROD_ADMIN_PASSWORD") if (v := os.environ.get(k))]


def main(root):
    values = secrets()
    if not values:
        return 0
    removed = 0
    for folder, _, files in os.walk(root):
        for name in files:
            path = os.path.join(folder, name)
            with open(path, "rb") as f:
                data = f.read()
            if not any(v in data for v in values):
                continue
            if name.endswith((".png", ".jpg", ".mp4", ".webm")):
                os.remove(path)
                removed += 1
                continue
            for v in values:
                data = data.replace(v, b"***")
            with open(path, "wb") as f:
                f.write(data)
            removed += 1
    print(f"Removed the admin password from {removed} file(s)")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1]))
