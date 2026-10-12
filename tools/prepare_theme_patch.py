"""Exact-version semantic palette hooks. Does not recolor media or video frames."""
from pathlib import Path
import difflib
import json
import re

ROOT = Path(__file__).resolve().parents[1]


def save(relative, dex, name, transform):
    original = (ROOT / "research/tidal" / dex / relative).read_text(encoding="utf-8")
    patched = transform(original)
    target = ROOT / "research/probe-hook-smali" / relative
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(patched, encoding="utf-8", newline="\n")
    (ROOT / "experimental-patches" / name).write_text("".join(difflib.unified_diff(
        original.splitlines(keepends=True), patched.splitlines(keepends=True),
        fromfile="a/" + relative, tofile="b/" + relative,
    )), encoding="utf-8", newline="\n")


def wave(text):
    roles = json.loads((ROOT / "mappings/wave-color-roles.json").read_text())
    start = text.index(".method public constructor <init>()V")
    end = text.index(".end method", start)
    ctor = text[start:end]
    fields = re.findall(r"iput-wide[^\n]*Lal0/a;->(\w+):J", ctor)
    if fields != list(roles) or len(fields) != 112 or ctor.count("    return-void") != 1:
        raise SystemExit("Wave palette ABI changed")
    statements = []
    for field, role in roles.items():
        if not role.startswith(("solidBase", "solidContrast", "solidAccent", "opacityBase", "opacityContrast", "opacityAccent")):
            continue
        statements.append(f'''    iget-wide v1, v0, Lal0/a;->{field}:J

    const-string v3, "{role}"

    invoke-static {{v1, v2, v3}}, Ldev/nino/squareglass/SquarePalette;->wave(JLjava/lang/String;)J

    move-result-wide v1

    iput-wide v1, v0, Lal0/a;->{field}:J

''')
    ctor = ctor.replace("    return-void", "".join(statements) + "    return-void")
    return text[:start] + ctor + text[end:]


def wave_provider(text):
    needle = "    sget-object v9, Lcom/tidal/wave2/theme/WaveThemeKt;->a:Lal0/a;"
    if text.count(needle) != 1:
        raise SystemExit("Wave provider ABI changed")
    return text.replace(needle, '''    const/4 v9, 0x0

    invoke-static {v4, v9}, Ldev/nino/squareglass/NativeTheme;->waveColors(Landroidx/compose/runtime/Composer;I)Lal0/a;

    move-result-object v9''')


def market(text):
    roles = json.loads((ROOT / "mappings/market-color-roles.json").read_text())
    count = 0
    def getter(match):
        nonlocal count
        method = match[0]
        accesses = re.findall(r"iget-object v0, p0, Lcom/squareup/ui/market/core/theme/MarketStylesheet\$c;->(\w+):Lf20/a;", method)
        if len(accesses) != 1 or method.count("    return-object v0") != 1:
            raise SystemExit("Market getter ABI changed")
        role = roles[accesses[0]]
        count += 1
        return method.replace(".locals 1", ".locals 2").replace("    return-object v0", f'''    const-string v1, "{role}"

    invoke-static {{v0, v1}}, Ldev/nino/squareglass/SquarePalette;->market(Lf20/a;Ljava/lang/String;)Lf20/a;

    move-result-object v0

    return-object v0''')
    result = re.sub(r"\.method public final \w+\(\)Lf20/a;.*?\.end method", getter, text, flags=re.S)
    if count != 90:
        raise SystemExit(f"Expected 90 Market color accessor methods, got {count}")
    return result


def market_system(text):
    start = text.index("    new-instance v1, Lm20/a$a;")
    end = text.index("    const/4 v4, 0x6", start)
    old = text[start:end]
    if old.count("ColorMode;->DARK") != 1:
        raise SystemExit("Market system-theme ABI changed")
    text = text[:start] + "    sget-object v1, Lm20/a$b;->b:Lm20/a$b;\n\n" + text[end:]
    needle = "    sget-object p0, Lcom/tidal/android/ui/market/a;->a:Lm20/b;"
    if text.count(needle) != 1:
        raise SystemExit("Market configuration provider ABI changed")
    return text.replace(needle, "    invoke-static {}, Ldev/nino/squareglass/SquareMarketConfig;->create()Lm20/b;\n\n    move-result-object p0")


def corners(text):
    # Keep zero and the capsule sentinel; grow explicit small/medium/large radii.
    sizes = {"0x4": "0x8", "0x8": "0xc", "0xc": "0x12", "0x10": "0x18", "0x14": "0x1c", "0x18": "0x20"}
    start = text.index(".method public constructor <init>()V")
    end = text.index(".end method", start)
    ctor = text[start:end]
    count = 0
    def change(match):
        nonlocal count
        old = match[2]
        if old in sizes:
            count += 1
            return match[1].replace("const/4", "const/16") + sizes[old]
        return match[0]
    ctor = re.sub(r"(const(?:/16|/4) v\d+, )(0x[0-9a-f]+)", change, ctor)
    if count != 6:
        raise SystemExit("Wave corner ABI changed")
    return text[:start] + ctor + text[end:]


def wave_one(text):
    # The still-used Wave1 interface exposes 25 retained semantic palette fields.
    # Read at the accessor boundary so a system theme change does not retain a
    # palette allocated before Activity recreation.
    roles = {
        "a": "solidBaseFill", "b": "solidBaseBrighter", "c": "solidContrastDarker",
        "d": "solidContrastFill", "e": "solidBaseFill", "f": "solidBaseBrighter",
        "g": "solidContrastDarkest", "h": "solidBaseBrighter", "i": "solidContrastFill",
        "j": "solidBaseFill", "k": "solidContrastDark", "l": "solidBaseBrighter",
        "m": "solidBaseBrighter", "n": "solidContrastFill", "o": "solidContrastFill",
        "p": "solidContrastDarkest", "q": "solidContrastFill", "r": "solidAccentFill",
        "s": "solidBaseBrightest", "t": "solidBaseBright", "u": "solidContrastFill",
        "v": "solidContrastDarker", "w": "solidContrastDarkest", "x": "solidAccentFill",
        "y": "solidBaseBrightest",
    }
    count = 0
    def getter(match):
        nonlocal count
        method = match[0]
        fields = re.findall(r"iget-wide v0, p0, Lyk0/b;->(\w+):J", method)
        if len(fields) != 1 or method.count("    return-wide v0") != 1:
            raise SystemExit("Wave1 getter ABI changed")
        count += 1
        role = roles[fields[0]]
        return method.replace(".locals 2", ".locals 3").replace("    return-wide v0", f'''    const-string v2, "{role}"

    invoke-static {{v0, v1, v2}}, Ldev/nino/squareglass/SquarePalette;->wave(JLjava/lang/String;)J

    move-result-wide v0

    return-wide v0''')
    result = re.sub(r"\.method public final \w+\(\)J.*?\.end method", getter, text, flags=re.S)
    if count != 25:
        raise SystemExit(f"Expected 25 Wave1 accessors, got {count}")
    return result


if __name__ == "__main__":
    save("al0/a.smali", "smali_classes13", "050-square-wave-colors.patch", wave)
    save("com/tidal/wave2/theme/WaveThemeKt.smali", "smali_classes13", "051-square-wave-theme.patch", wave_provider)
    save("com/squareup/ui/market/core/theme/MarketStylesheet$c.smali", "smali_classes23", "052-square-market-colors.patch", market)
    save("com/tidal/android/ui/market/a.smali", "smali_classes17", "053-square-market-system-theme.patch", market_system)
    save("al0/b.smali", "smali_classes13", "054-square-wave-corners.patch", corners)
    save("yk0/b.smali", "smali_classes17", "055-square-wave-one-colors.patch", wave_one)
    print("Prepared semantic Wave and Market theme hooks")
