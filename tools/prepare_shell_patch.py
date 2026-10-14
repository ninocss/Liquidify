"""Prepare exact-version native backdrop and tab hooks. Experimental UI slice."""
from pathlib import Path
import difflib

ROOT = Path(__file__).resolve().parents[1]
HOOKS = ROOT / "research/probe-hook-smali"
PATCHES = ROOT / "experimental-patches"


def write_patch(relative: str, dex: str, name: str, transform):
    original = (ROOT / "research/tidal" / dex / relative).read_text(encoding="utf-8")
    patched = transform(original)
    target = HOOKS / relative
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(patched, encoding="utf-8", newline="\n")
    PATCHES.mkdir(exist_ok=True)
    (PATCHES / name).write_text("".join(difflib.unified_diff(
        original.splitlines(keepends=True), patched.splitlines(keepends=True),
        fromfile="a/" + relative, tofile="b/" + relative,
    )), encoding="utf-8", newline="\n")


def tabs(text: str) -> str:
    signature = ".method public static final a(ILvn0/b;Lam0/l;Landroidx/compose/ui/Modifier;Landroidx/compose/runtime/Composer;I)V"
    if text.count(signature) != 1:
        raise SystemExit("Native tab ABI changed")
    start = text.index(signature)
    end = text.index(".end method", start) + len(".end method")
    return text[:start] + signature + """
    .locals 0
    .annotation build Landroidx/compose/runtime/Composable;
    .end annotation

    invoke-static/range {p0 .. p5}, Ldev/nino/squareglass/NativeTabs;->Render(ILjava/util/List;Lam0/l;Landroidx/compose/ui/Modifier;Landroidx/compose/runtime/Composer;I)V

    return-void
.end method""" + text[end:]


def capture(text: str) -> str:
    needle = "    :cond_20\n    new-instance v0, Lcom/tidal/wave2/foundation/WaveScaffoldKt$WaveScaffold$2;"
    if text.count(needle) != 1:
        raise SystemExit("Native scaffold capture point changed")
    return text.replace(needle, """    :cond_20
    const/4 v0, 0x0

    invoke-static {v9, v1, v0}, Ldev/nino/squareglass/NativeBackdrop;->capture(Landroidx/compose/ui/Modifier;Landroidx/compose/runtime/Composer;I)Landroidx/compose/ui/Modifier;

    move-result-object v9

    new-instance v0, Lcom/tidal/wave2/foundation/WaveScaffoldKt$WaveScaffold$2;""")


def shell(text: str) -> str:
    gradient = """    invoke-static/range {v33 .. v38}, Landroidx/compose/foundation/BackgroundKt;->background$default(Landroidx/compose/ui/Modifier;Landroidx/compose/ui/graphics/Brush;Landroidx/compose/ui/graphics/Shape;FILjava/lang/Object;)Landroidx/compose/ui/Modifier;

    move-result-object v2"""
    fill = """    invoke-static {v2, v3, v4, v5}, Landroidx/compose/foundation/BackgroundKt;->background-bw27NRU(Landroidx/compose/ui/Modifier;JLandroidx/compose/ui/graphics/Shape;)Landroidx/compose/ui/Modifier;

    move-result-object v2"""
    if text.count(gradient) != 1 or text.count(fill) != 1:
        raise SystemExit("Native accessory surfaces changed")
    clip = """    invoke-static {v2, v5}, Landroidx/compose/ui/draw/ClipKt;->clip(Landroidx/compose/ui/Modifier;Landroidx/compose/ui/graphics/Shape;)Landroidx/compose/ui/Modifier;

    move-result-object v2"""
    if text.count(clip) != 1:
        raise SystemExit("Native accessory shell outline changed")
    # The shared stock rounded rectangle clips both accessories, including their
    # padding and moving lens. Each replacement capsule owns its own outline.
    text = text.replace(clip, "")
    text = text.replace(gradient, "    move-object/from16 v2, v33")
    return text.replace(fill, "    invoke-static {v3, v4}, Ldev/nino/squareglass/NativeGlass;->observeHostBackground(J)V")


def mini(text: str) -> str:
    needle = """    invoke-static {v5, v10, v14, v9}, Landroidx/compose/foundation/layout/SizeKt;->fillMaxWidth$default(Landroidx/compose/ui/Modifier;FILjava/lang/Object;)Landroidx/compose/ui/Modifier;

    move-result-object v9"""
    if text.count(needle) != 1:
        raise SystemExit("Native mini-player container changed")
    return text.replace(needle, needle + """

    const/4 v10, 0x0

    invoke-static {v9, v15, v10}, Ldev/nino/squareglass/NativeGlass;->miniSurface(Landroidx/compose/ui/Modifier;Landroidx/compose/runtime/Composer;I)Landroidx/compose/ui/Modifier;

    move-result-object v9""")


write_patch("com/tidal/android/feature/appscaffold/ui/composable/x.smali", "smali_classes5", "010-square-glass-native-tabs.patch", tabs)
write_patch("com/tidal/wave2/foundation/WaveScaffoldKt.smali", "smali_classes13", "020-square-glass-native-backdrop.patch", capture)
write_patch("com/tidal/android/feature/appscaffold/ui/composable/i.smali", "smali_classes5", "030-square-glass-native-shell.patch", shell)
write_patch("com/tidal/android/feature/appscaffold/ui/composable/MiniPlayerKt.smali", "smali_classes5", "040-square-glass-native-mini-player.patch", mini)
print("Prepared exact native tabs and page capture hooks")
