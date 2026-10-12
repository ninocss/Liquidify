"""Export permitted source and the separate Manager; excludes research and keys."""
from pathlib import Path
import hashlib
import json
import subprocess
import zipfile
from package_alpha import VERSION

ROOT = Path(__file__).resolve().parents[1]


def main():
    payload = {}
    files = ["README.md", "BUILDING.md", "IMPLEMENTATION.md", "LINN_OUTPUT.md", "VALIDATION.md", "RELEASE_NOTES.md", "release.json", "LICENSE", "radiant-base/LICENSE", "radiant-base/THIRD-PARTY.md", "upstream-lock.json",
             "build.gradle.kts", "settings.gradle.kts", "gradle.properties", "gradlew", "gradlew.bat", ".gitignore"]
    for name in files:
        payload[name] = (ROOT / name).read_bytes()
    for folder in (".github", "patches", "radiant-base/patches", "extension", "extension-smali", "host-stubs", "gradle", "licenses", "mappings", "tools", "tests", "experimental-patches"):
        for path in (ROOT / folder).rglob("*"):
            relative = path.relative_to(ROOT)
            if not path.is_file() or any(p in ("build", "bin", "__pycache__", ".gradle", ".kotlin") for p in relative.parts) or path.suffix in (".class", ".pyc", ".aar"):
                continue
            payload[relative.as_posix()] = path.read_bytes()
    # Git supplies the upstream source inventory; explicitly include added source.
    clone = ROOT / "radiant-base"
    tracked = subprocess.check_output(["git", "ls-files", "Manager"], cwd=clone, text=True).splitlines()
    added = subprocess.check_output(["git", "ls-files", "--others", "--exclude-standard", "Manager"], cwd=clone, text=True).splitlines()
    for name in tracked + added:
        if name.startswith("Manager/.github/") or name.startswith("Manager/META-INF/"):
            continue
        path = clone / name
        if path.is_file(): payload["radiant-base/" + name] = path.read_bytes()
    payload["manager-fork.patch"] = subprocess.check_output(["git", "diff", "HEAD", "--binary", "--", "Manager"], cwd=clone)
    for name in ("output-qa-alpha22.json", "strict-patches-alpha22.json", "probe-linkage.json", "package-alpha.json", "manager-roundtrip.json", "output-qa-alpha21.json", "strict-patches-alpha21.json", "design-resources-build.json", "pixel-qa.json", "emulator-qa-alpha6.json", "emulator-qa-alpha7.json", "emulator-qa-alpha8.json", "emulator-qa-alpha9.json", "emulator-qa-alpha10.json", "emulator-qa-alpha11.json", "emulator-qa-alpha12.json", "emulator-qa-alpha13.json", "emulator-qa-alpha14.json", "emulator-qa-alpha15.json", "emulator-qa-alpha16.json", "emulator-qa-alpha17.json", "emulator-qa-alpha18.json", "emulator-qa-alpha19.json", "emulator-qa-alpha20.json", "squareglass-test-build.json", "strict-patches-alpha18.json", "strict-patches-alpha19.json", "strict-patches-alpha20.json"):
        report = ROOT / "reports" / name
        if report.exists(): payload["reports/" + name] = report.read_bytes()
    for name in payload:
        if name.endswith((".apk", ".apkm", ".dex", ".class", ".pyc", ".aar", ".keystore", ".jks")) or any(p in ("research", "local.properties", "__pycache__", "bin") for p in Path(name).parts):
            raise SystemExit("Excluded source payload: " + name)
    payload["source-checksums.json"] = (json.dumps({n: hashlib.sha256(b).hexdigest() for n, b in sorted(payload.items())}, indent=2) + "\n").encode()
    target = ROOT / f"dist/Liquidify-{VERSION}-source.zip"
    target.parent.mkdir(exist_ok=True)
    with zipfile.ZipFile(target, "w", zipfile.ZIP_DEFLATED, compresslevel=9) as out:
        for name, data in sorted(payload.items()):
            info = zipfile.ZipInfo(name, (2026, 10, 9, 0, 0, 0))
            info.compress_type = zipfile.ZIP_DEFLATED
            if name.endswith("gradlew"): info.external_attr = 0o100755 << 16
            out.writestr(info, data)
    result = dict(file=target.name, entries=len(payload), sha256=hashlib.sha256(target.read_bytes()).hexdigest())
    (ROOT / "reports/source-package.json").write_text(json.dumps(result, indent=2) + "\n")
    print(json.dumps(result))


if __name__ == "__main__":
    main()
