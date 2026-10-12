"""Prepare the experimental hook; not a finished UI redesign."""
from pathlib import Path
import difflib
import json
import argparse

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("--probe", action="store_true", help="Include the visible native-back test button")
args = parser.parse_args()
hook = "GlassProbe" if args.probe else "GlassHost"

root = Path(__file__).resolve().parents[1]
class_name = "com/aspiro/wamp/MainActivity.smali"
original = root / "research/tidal/smali" / class_name
text = original.read_text(encoding="utf-8")
needle = "    invoke-virtual {v0, v2}, Landroidx/compose/ui/platform/ComposeView;->setContent(Lam0/p;)V"
replacement = f"""    invoke-static {{p0, v2}}, Ldev/nino/squareglass/{hook};->wrapNative(Landroid/app/Activity;Lam0/p;)Lam0/p;

    move-result-object v2

""" + needle
if text.count(needle) != 1:
    raise SystemExit("Expected exactly one native Compose root; refusing to guess")
patched = text.replace(needle, replacement)
target = root / "research/probe-hook-smali" / class_name
target.parent.mkdir(parents=True, exist_ok=True)
target.write_text(patched, encoding="utf-8", newline="\n")
patch = "".join(difflib.unified_diff(text.splitlines(keepends=True), patched.splitlines(keepends=True), fromfile="a/" + class_name, tofile="b/" + class_name))
out = root / "experimental-patches"
out.mkdir(exist_ok=True)
(out / "000-square-glass-host.patch").write_text(patch, encoding="utf-8", newline="\n")
old = out / "000-square-glass-probe.patch"
if old.exists():
    old.unlink()
print("Prepared experimental hook for the exact TIDAL MainActivity")
