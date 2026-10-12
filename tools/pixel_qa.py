"""Explicitly scoped, foreground-only QA for the separate physical test app."""
import argparse
import os
from pathlib import Path
import subprocess
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
config = ROOT / "research/pixel-device.txt"
serial = os.environ.get("SQUAREGLASS_PIXEL_SERIAL") or (config.read_text().strip() if config.exists() else "")
if not serial:
    raise SystemExit("Set SQUAREGLASS_PIXEL_SERIAL to the explicitly authorized test device")
ADB = ["C:/platform-tools/adb.exe", "-s", serial]
PACKAGE = "dev.nino.tidalglass.test"


def read(*args):
    return subprocess.check_output(ADB + list(args))


def guard():
    policy = read("shell", "dumpsys", "window", "policy").decode()
    if any(line.strip() == "showing=true" for line in policy.splitlines()):
        raise SystemExit("Pixel is locked; no input or capture performed")
    activities = read("shell", "dumpsys", "activity", "activities").decode()
    if not any(PACKAGE + "/" in line for line in activities.splitlines() if "topResumedActivity=" in line):
        raise SystemExit("Separate test app is not in foreground; no input or capture performed")


def main():
    sys.stdout.reconfigure(encoding="utf-8")
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("action", choices=["capture", "ui", "tap", "swipe", "back", "text"])
    parser.add_argument("args", nargs="*")
    args = parser.parse_args()
    guard()
    if args.action == "capture":
        name = args.args[0]
        if Path(name).name != name or not name.endswith(".png"):
            raise SystemExit("Use a PNG basename within ignored research")
        (ROOT / "research" / name).write_bytes(read("exec-out", "screencap", "-p"))
    elif args.action == "ui":
        # Never reuse a stale dump if UIAutomator cannot reach idle state.
        remote = "/sdcard/squareglass-qa.xml"
        subprocess.run(ADB + ["shell", "rm", "-f", remote], check=True)
        result = subprocess.run(ADB + ["shell", "uiautomator", "dump", remote], capture_output=True, timeout=18)
        if result.returncode or b"dumped to:" not in result.stdout:
            raise SystemExit("UI did not become idle; use a fresh screenshot")
        guard()
        root = ET.fromstring(read("shell", "cat", remote))
        for node in root.iter("node"):
            label = node.get("text") or node.get("content-desc")
            if label: print(node.get("bounds"), label)
    elif args.action == "back":
        subprocess.run(ADB + ["shell", "input", "keyevent", "4"], check=True)
    else:
        if args.action != "text":
            for value in args.args: int(value)
        subprocess.run(ADB + ["shell", "input", args.action, *args.args], check=True)


if __name__ == "__main__":
    main()
