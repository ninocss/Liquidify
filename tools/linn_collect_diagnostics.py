"""Collect a read-only, redacted Linn handoff report from an attached Android phone."""
import argparse
import datetime
import json
import re
import subprocess
from pathlib import Path


def redact(text):
    text = re.sub(r"(?:https?|tidal)://[^\s<>]+", "[URI]", text)
    text = re.sub(r"\b(?:\d{1,3}\.){3}\d{1,3}\b", "[LAN]", text)
    text = re.sub(r"[\w.+-]+@[\w.-]+", "[ACCOUNT]", text)
    return re.sub(r"(?i)(token|password|authorization)([=: ]+)[^\s,;]+", r"\1=[REDACTED]", text)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--serial", help="ADB device serial; required if multiple devices are attached")
    parser.add_argument("--package", default="com.aspiro.tidal", choices=["com.aspiro.tidal", "dev.nino.tidalglass.test"])
    parser.add_argument("--output", type=Path, default=Path("research/linn-phone-diagnostics.json"))
    args = parser.parse_args()
    base = ["adb"] + (["-s", args.serial] if args.serial else [])

    def adb(*command):
        result = subprocess.run(base + list(command), capture_output=True, text=True, timeout=20, encoding="utf-8", errors="replace")
        if result.returncode:
            raise RuntimeError("ADB unavailable: connect and authorize the Android phone")
        return result.stdout.strip()

    report = {"collectedAt": datetime.datetime.now(datetime.timezone.utc).isoformat(), "mode": "read-only", "package": args.package,
              "physicalPhoneAcceptance": False, "originalFailureReproduced": False}
    try:
        report["androidRelease"] = adb("shell", "getprop", "ro.build.version.release")
        report["model"] = adb("shell", "getprop", "ro.product.model")
        report["emulator"] = adb("shell", "getprop", "ro.kernel.qemu") == "1"
        package = adb("shell", "dumpsys", "package", args.package)
        report["version"] = [line.strip() for line in package.splitlines() if "versionName=" in line or "versionCode=" in line]
        report["linnLog"] = redact(adb("logcat", "-d", "-v", "brief", "SquareLinn:I", "*:S"))
        report["status"] = "Captured; reproduction and audible playback require review"
    except (RuntimeError, subprocess.TimeoutExpired) as error:
        report["status"] = str(error)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(json.dumps({"output": str(args.output), "status": report["status"]}))


if __name__ == "__main__":
    main()
