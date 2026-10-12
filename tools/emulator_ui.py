"""Developer QA: compact UI hierarchy of the explicitly scoped test emulator."""
import subprocess
import sys
import xml.etree.ElementTree as ET

sys.stdout.reconfigure(encoding="utf-8")

ADB = "C:/platform-tools/adb.exe"
prefix = [ADB, "-s", "emulator-5554"]
subprocess.run(prefix + ["shell", "rm", "-f", "/sdcard/squareglass-qa.xml"], check=True)
result = subprocess.run(prefix + ["shell", "uiautomator", "dump", "/sdcard/squareglass-qa.xml"], capture_output=True, timeout=18)
if result.returncode or b"dumped to:" not in result.stdout:
    raise SystemExit("Emulator UI did not become idle; no stale hierarchy reused")
xml = subprocess.check_output(prefix + ["shell", "cat", "/sdcard/squareglass-qa.xml"])
root = ET.fromstring(xml)
for node in root.iter("node"):
    label = node.get("text") or node.get("content-desc")
    if label:
        print(node.get("bounds"), label)
