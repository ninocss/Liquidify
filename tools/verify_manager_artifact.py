"""Read back the Manager's installed QA artifact; never installs or accesses accounts."""
import argparse
import hashlib
import json
import re
from pathlib import Path
import shutil
import subprocess
import zipfile
from collections import Counter
import os

ROOT = Path(__file__).resolve().parents[1]
JAVA = Path(r"C:\Program Files\Android\Android Studio\jbr\bin\java.exe")
BT = Path.home() / "AppData/Local/Android/Sdk/build-tools/37.0.0"


def digest(data):
    return hashlib.sha256(data).hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("apk", type=Path)
    parser.add_argument("--package", default="dev.nino.squareglass.qa")
    args = parser.parse_args()
    report = json.loads((ROOT / "reports/package-alpha.json").read_text())
    pack_path = ROOT / "dist" / report["file"]
    assert digest(pack_path.read_bytes()) == report["sha256"]
    with zipfile.ZipFile(pack_path) as pack, zipfile.ZipFile(args.apk) as apk:
        manifest = json.loads(pack.read("manifest.json"))
        design = manifest["designResources"]
        assert apk.read("resources.arsc") == (ROOT / "research/square-resources.arsc").read_bytes()
        for item in design["fonts"] + design["themeXml"]:
            assert digest(apk.read(item["target"])) == item["sha256"], item["target"]
        priority_dex = ROOT / "research/manager-priority.dex"
        priority_dex.write_bytes(apk.read("classes.dex"))
        helpers = [n.removeprefix("extension/") for n in pack.namelist() if n.startswith("extension/dev/nino/squareglass/") and n.endswith(".smali")]
    # Inspect classes.dex explicitly: native originals deliberately remain in
    # lower-priority DEX files, as in Radiant's normal class-shadowing mechanism.
    work = ROOT / "research/manager-priority-smali"
    assert work.resolve().parent == (ROOT / "research").resolve()
    if work.exists(): shutil.rmtree(work)
    subprocess.run([str(JAVA), "-cp", "build/dex-tools/*", "tools/DexTool.java", "disassemble", str(priority_dex), str(work)], cwd=ROOT, check=True)
    for relative in helpers:
        assert (work / relative).is_file(), relative
    canonical=ROOT/'research/manager-expected-extension'
    canonical.mkdir(exist_ok=True)
    subprocess.run([str(JAVA),'-cp','build/dex-tools/*','tools/DexTool.java','disassemble',
                    'research/probe-remapped.dex',str(canonical)],cwd=ROOT,check=True)
    def normalize(text):
        return [line.strip() for line in text.splitlines() if line.strip()
                and not line.strip().startswith(('#','.line ','.source '))]
    for relative in helpers:
        assert normalize((work/relative).read_text()) == normalize((canonical/relative).read_text()), f'Helper differs from current DEX: {relative}'
    added_instructions = 0
    targets=set()
    for patch in sorted((ROOT / "experimental-patches").glob("*.patch")):
        lines = patch.read_text().splitlines()
        target = next(line[6:] for line in lines if line.startswith("--- a/"))
        targets.add(target)
    for target in sorted(targets):
        # Later ordered patches can replace an earlier implementation entirely.
        # Validate the final generated class, never abandoned intermediate lines.
        def instructions(text):
            return Counter(line.strip() for line in text.splitlines()
                           if line.strip().startswith(("invoke-", "sput", "iput")))
        expected=instructions((ROOT / 'research/probe-hook-smali' / target).read_text())
        actual=instructions((work / target).read_text())
        missing=expected-actual
        assert not missing, f"Missing final instructions in {target}: {missing}"
        added_instructions += sum(expected.values())
    settings = (work / "dev/nino/squareglass/SquareSettings.smali").read_text()
    for method, value in (("initialTheme", 0), ("themeEnabled", 1), ("managerReady", 1)):
        body = re.search(rf"\.method public static {method}\(\)I(.*?)\.end method", settings, re.S).group(1)
        assert f"const/4 v0, 0x{value:x}" in body
    inspection = subprocess.run([str(BT / "aapt2.exe"), "dump", "badging", str(args.apk)], capture_output=True, text=True, check=True, timeout=30)
    assert not inspection.stderr.strip(), inspection.stderr
    assert f"name='{args.package}'" in inspection.stdout.splitlines()[0]
    assert "minSdkVersion:'33'" in inspection.stdout
    subprocess.run([str(BT / "apksigner.bat"), "verify", str(args.apk)], check=True,
                   env=dict(os.environ,JAVA_HOME=str(JAVA.parent.parent)),timeout=60)
    result = dict(passed=True, apkSha256=digest(args.apk.read_bytes()), zipSha256=report["sha256"],
        package=args.package, minSdk=33, helpers=len(helpers), patchedInstructions=added_instructions,
        fingerprintedResources=len(design["fonts"])+len(design["themeXml"]), paletteExact=True,
        signatureVerified=True, evidence="Manager ZIP import, patch, signing, installation and APK readback on emulator; no authenticated playback proof")
    (ROOT / "reports/manager-roundtrip.json").write_text(json.dumps(result, indent=2) + "\n")
    print(json.dumps(result))


if __name__ == "__main__":
    main()
