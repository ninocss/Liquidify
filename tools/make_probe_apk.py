"""Make a LOCAL probe APK. Separate test package is the default."""
from pathlib import Path
import argparse
import json
import hashlib
import zipfile

root = Path(__file__).resolve().parents[1]
report = json.loads((root / "reports/probe-linkage.json").read_text())
if not report["passed"] or report.get("sourceDexSha256") != hashlib.sha256((root / "research/probe-remapped.dex").read_bytes()).hexdigest():
    raise SystemExit("Linkage audit did not pass; refusing to package an APK")
apk = root / "research/tidal-stock-2.200.0.apk"
lock = json.loads((root / "upstream-lock.json").read_text())
with apk.open("rb") as stream:
    if hashlib.file_digest(stream, "sha256").hexdigest() != lock["tidal"]["sha256"]:
        raise SystemExit("Wrong input APK")
p = argparse.ArgumentParser(description=__doc__)
p.add_argument("--emulator-internal", action="store_true", help="Use exported internal Activity manifest; emulator ONLY")
args = p.parse_args()
manifest = root / ("research/manifest-probe.xml" if args.emulator_internal else "research/manifest-test.xml")
design = json.loads((root / "research/design-resources.json").read_text())
font_targets = {item["target"]: item for item in design["fonts"]}
xml_targets = {item["target"]: item for item in design.get("themeXml", [])}
dest = root / ("research/tidal-probe-unsigned.apk" if args.emulator_internal else "research/squareglass-test-unsigned.apk")
with zipfile.ZipFile(apk) as src, zipfile.ZipFile(dest, "w") as out:
    dexes = sorted((n for n in src.namelist() if n.startswith("classes") and n.endswith(".dex")), key=lambda n: 1 if n == "classes.dex" else int(n[7:-4]))
    for entry in src.infolist():
        if entry.filename.startswith("META-INF/") and entry.filename.upper().endswith((".SF", ".RSA", ".DSA", ".MF", ".EC")):
            continue
        if entry.filename == "AndroidManifest.xml":
            out.writestr(entry, manifest.read_bytes())
        elif entry.filename == "resources.arsc":
            out.writestr(entry, (root / "research/square-resources.arsc").read_bytes())
        elif entry.filename in xml_targets:
            spec = xml_targets[entry.filename]
            replacement = (root / "research/design-xml" / entry.filename).read_bytes()
            if hashlib.sha256(src.read(entry)).hexdigest() != spec["originalSha256"] or hashlib.sha256(replacement).hexdigest() != spec["sha256"]:
                raise SystemExit("XML fingerprint mismatch")
            out.writestr(entry, replacement)
        elif entry.filename in font_targets:
            spec = font_targets[entry.filename]
            original = src.read(entry)
            replacement = (root / "research/design-fonts" / Path(spec["source"]).name).read_bytes()
            if hashlib.sha256(original).hexdigest() != spec["originalSha256"] or hashlib.sha256(replacement).hexdigest() != spec["sha256"]:
                raise SystemExit("Font fingerprint mismatch")
            out.writestr(entry, replacement)
        elif entry.filename == "classes.dex":
            entry.filename = f"classes{len(dexes) + 1}.dex"
            out.writestr(entry, src.read("classes.dex"))
        else:
            out.writestr(entry, src.read(entry))
    out.writestr("classes.dex", (root / "research/probe-combined.dex").read_bytes(), compress_type=zipfile.ZIP_STORED)
print(dest)
