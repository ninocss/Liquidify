"""Package the versioned Liquidify Beta. Never includes stock TIDAL."""
from pathlib import Path
import hashlib
import json
import re
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parents[1]
VERSION = json.loads((ROOT / "release.json").read_text())["version"]


def digest(data):
    return hashlib.sha256(data).hexdigest()


def main():
    resources = json.loads((ROOT / "reports/design-resources-build.json").read_text())
    if resources.get("themeMode", 0) != 0:
        raise SystemExit("Rebuild System resources before packaging the selectable-theme ZIP")
    report = json.loads((ROOT / "reports/probe-linkage.json").read_text())
    dex = (ROOT / "research/probe-remapped.dex").read_bytes()
    if not report["passed"] or report["sourceDexSha256"] != digest(dex):
        raise SystemExit("Current extension must pass linkage audit")
    upstream = ROOT / "radiant-base/patches"
    manifest = json.loads((upstream / "manifest.json").read_text())
    helpers = sorted(p.relative_to(ROOT / "research/probe-remapped").as_posix()
                     for p in (ROOT / "research/probe-remapped").rglob("*.smali"))
    patches = sorted(p.name for p in (ROOT / "experimental-patches").glob("*.patch"))
    core = [p for p in patches if int(p[:3]) < 50]
    theme = [p for p in patches if int(p[:3]) >= 50]
    specs = [
        dict(id="LiquidifyCore", source="liquidify", order=-30, fileNames=core, extensionFiles=helpers,
             title="Liquidify · Glass navigation",
             description="AndroidLiquidGlass navigation, mini-player and runtime design settings. Keeps native TIDAL routes and actions. Requires Liquidify Manager 1.0.0 Beta.",
             default=True, disables=["EnableLegacyUi", "MiniPlayerRedesign"],
             advancedOptions=[dict(type="choice", key="theme", title="Theme", entries=["System", "Light", "Dark"], values=["0x0", "0x1", "0x2"], defaultIndex=0, token="RL_SQUARE_THEME_MODE", inline=True)]),
        dict(id="LiquidifyTheme", source="liquidify", order=-20, fileNames=theme, requires=["LiquidifyCore"],
             title="Liquidify · Design and gestures",
             description="Anchored Liquid Glass Output popup with native TIDAL devices and an integrated Linn OpenHome provider. Device-side TIDAL queue, central playback adapter, remote volume and reconnect. Requires TIDAL sign-in for the renderer through the Linn app. Beta; see LINN_OUTPUT.md for tested scope.", default=True),
        dict(id="LiquidifyTypography", source="liquidify", order=-10, fileNames=[], requires=["LiquidifyCore"],
             title="Liquidify · Typography", description="Static Inter font instances, with original labels and accessibility text retained.", default=True),
    ]
    manifest["patches"] = specs + manifest["patches"]
    manifest["releaseVersion"] = VERSION
    manifest["name"] = "Liquidify"
    manifest["editor"] = "ninocss"
    manifest["compatibility"] = dict(apkSha256=json.loads((ROOT / "upstream-lock.json").read_text())["tidal"]["sha256"],
                                     minSdk=33, capabilities=["exactBaseSha256", "squareResourcesV1", "squareRuntimeThemeV1"], strictSquarePatches=True)
    manifest["designResources"] = json.loads((ROOT / "research/design-resources.json").read_text())
    outdir = ROOT / "dist"
    outdir.mkdir(exist_ok=True)
    target = outdir / f"Liquidify-{VERSION}-patches.zip"
    payload = {}
    for p in upstream.rglob("*"):
        if p.is_file() and p.name != "manifest.json":
            payload[p.relative_to(upstream).as_posix()] = p.read_bytes()
    for p in (ROOT / "experimental-patches").glob("*.patch"):
        payload[p.name] = p.read_bytes()
    for relative in helpers:
        text = (ROOT / "research/probe-remapped" / relative).read_text()
        if relative == "dev/nino/squareglass/SquareSettings.smali":
            for method, value, token in (("initialTheme", "0x0", "RL_SQUARE_THEME_MODE"), ("managerReady", "0x1", "RL_SQUARE_MANAGER_READY"), ("themeEnabled", "0x1", "RL_SQUARE_THEME_ENABLED")):
                pattern = rf"(\.method public static {method}\(\)I.*?const/4 v0, ){value}"
                text, n = re.subn(pattern, rf"\g<1>__{token}__", text, flags=re.S)
                if n != 1:
                    raise SystemExit(f"Unexpected settings ABI for {method}")
        payload["extension/" + relative] = text.encode()
    for p in (ROOT / "research/design-fonts").glob("*.ttf"):
        payload["fonts/" + p.name] = p.read_bytes()
    payload["manifest.json"] = (json.dumps(manifest, indent=2) + "\n").encode()
    payload["README.md"] = (ROOT / "README.md").read_bytes()
    payload["LINN_OUTPUT.md"] = (ROOT / "LINN_OUTPUT.md").read_bytes()
    payload["LICENSE"] = (ROOT / "LICENSE").read_bytes()
    payload["upstream-lock.json"] = (ROOT / "upstream-lock.json").read_bytes()
    for p in (ROOT / "licenses").glob("*"):
        if p.is_file(): payload["licenses/" + p.name] = p.read_bytes()
    checksums = {name: digest(data) for name, data in sorted(payload.items())}
    payload["checksums.json"] = (json.dumps(checksums, indent=2) + "\n").encode()
    with zipfile.ZipFile(target, "w", zipfile.ZIP_DEFLATED, compresslevel=9) as out:
        for name, data in sorted(payload.items()):
            info = zipfile.ZipInfo(name, date_time=(2026, 10, 9, 0, 0, 0))
            info.compress_type = zipfile.ZIP_DEFLATED
            out.writestr(info, data)
    result = dict(file=target.name, sha256=digest(target.read_bytes()), helpers=len(helpers), squarePatches=len(patches), status="1.0.0 Beta; scoped hardware acceptance documented in LINN_OUTPUT.md")
    (ROOT / "reports/package-alpha.json").write_text(json.dumps(result, indent=2) + "\n")
    print(json.dumps(result))


if __name__ == "__main__":
    main()
