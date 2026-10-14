"""WCAG contrast regressions against the shipped semantic paints and film floors."""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "extension/src/main/java/dev/nino/squareglass"


def rgb(argb):
    return tuple((argb >> shift) & 255 for shift in (16, 8, 0))


def composite(foreground, background, opacity):
    return tuple(a * opacity + b * (1 - opacity) for a, b in zip(foreground, background))


def luminance(color):
    def linear(value):
        value /= 255
        return value / 12.92 if value <= .04045 else ((value + .055) / 1.055) ** 2.4
    return sum(weight * linear(value) for weight, value in zip((.2126, .7152, .0722), color))


def contrast(a, b):
    a, b = sorted((luminance(a), luminance(b)))
    return (b + .05) / (a + .05)


class DesignContrast(unittest.TestCase):
    def setUp(self):
        self.palette = (JAVA / "SquarePalette.java").read_text(encoding="utf-8")

    def colors(self, role):
        match = re.search(rf"int {role}\(boolean dark\).*?dark \? 0x([a-f0-9]+) : 0x([a-f0-9]+)", self.palette)
        self.assertIsNotNone(match, role)
        return [rgb(int(value, 16)) for value in match.groups()]

    def test_semantic_labels_on_page_and_card(self):
        for paint in ("ink", "secondary", "tertiary"):
            for surface in ("base", "surface"):
                for dark, (foreground, background) in enumerate(zip(self.colors(paint), self.colors(surface))):
                    self.assertGreaterEqual(contrast(foreground, background), 4.5, (paint, surface, dark))

    def test_glass_accessory_worst_case_artwork(self):
        settings = (JAVA / "SquareSettings.java").read_text(encoding="utf-8")
        match = re.search(r"readableOpacity\(boolean dark\).*?dark \? ([.\d]+)f : ([.\d]+)f", settings, re.S)
        self.assertIsNotNone(match)
        for ink, film, opacity in zip(self.colors("ink"), ((51, 52, 58), (255, 255, 255)), map(float, match.groups())):
            # Every grayscale seed, including the extremes of any RGB artwork.
            for value in range(256):
                self.assertGreaterEqual(contrast(ink, composite(film, (value,) * 3, opacity)), 4.5)

    def test_accessory_material_uses_the_checked_contrast_floor(self):
        source=(ROOT/'extension/src/main/kotlin/dev/nino/squareglass/AccessoryGlass.kt').read_text()
        self.assertIn('Color(0xFF33343A) else Color.White',source)
        self.assertIn('SquareSettings.readableOpacity(dark)',source)

    def test_library_navigation_has_one_animated_icon_row(self):
        tabs=(ROOT/'extension/src/main/kotlin/dev/nino/squareglass/NativeTabs.kt').read_text()
        source=(ROOT/'extension/src/main/kotlin/dev/nino/squareglass/material/LiquidBottomTabs.kt').read_text()
        self.assertIn('SquarePalette.ink(dark)',tabs)
        self.assertIn('val dark = NativeGlass.dark',tabs)
        self.assertIn('val iconInk = ink',tabs)
        self.assertIn('AccessoryGlass.surface(',source)
        self.assertIn('IconMotion.icon(',tabs)
        self.assertEqual(source.count('content = content'),1)
        self.assertNotIn('ColorFilter.tint',source)
        self.assertNotIn('accentColor',source)
        self.assertIn('rememberCombinedBackdrop(backdrop, tabsBackdrop)',source)
        # Capture the single live row before projecting it through the lens.
        # No duplicate composable callbacks or icon animations are created.
        captured=source[source.index('// Record the one live'):source.index('// The original catalog')]
        self.assertEqual(captured.count('content = content'),1)

    def test_navigation_labels_during_selection_on_extreme_artwork(self):
        # The selection lens and highlight compose on the same contrast-checked
        # accessory film. Include black/white artwork and the pressed state.
        settings=(JAVA/'SquareSettings.java').read_text()
        match=re.search(r"readableOpacity\(boolean dark\).*?dark \? ([.\d]+)f : ([.\d]+)f",settings,re.S)
        for dark, (ink, film, opacity) in enumerate(zip(self.colors('ink'),
                ((51,52,58),(255,255,255)), map(float,match.groups()))):
            for value in range(256):
                surface=composite(film,(value,)*3,opacity)
                for selection in (0, .03, .10):
                    selected=composite(ink,surface,selection)
                    # InteractiveHighlight uses BlendMode.Plus. Its two dark
                    # glints can add at most (.015 + .02) * 255 per channel.
                    for glint in (0, .035 if dark == 0 else .23):
                        lit=tuple(min(255,c+255*glint) for c in selected)
                        self.assertGreaterEqual(contrast(ink,lit),4.5,(dark,value,selection,glint))

    def test_static_muted_labels_on_all_native_surfaces(self):
        rows={parts[1]:parts for line in (ROOT/'mappings/resource-palette.tsv').read_text().splitlines()
              if line and not line.startswith('#') for parts in [line.split('\t')]}
        for role in ('contrastDark','contrastDarker','contrastDarkest','gray'):
            for surface in ('baseFill','baseBright','baseBrighter','baseBrightest'):
                for column in (4,5):
                    self.assertGreaterEqual(contrast(rgb(int(rows[role][column],16)),
                        rgb(int(rows[surface][column],16))),4.5,(role,surface,column))

    def test_player_secondary_labels_on_extreme_artwork(self):
        paint = (JAVA / "NativePlayerPaint.java").read_text(encoding="utf-8")
        alpha = int(re.search(r"Math.max\(alpha, (\d+)\)", paint)[1]) / 255
        hook = (ROOT / "tools/prepare_runtime_patch.py").read_text(encoding="utf-8")
        film = float(re.findall(r"# (0\.\d+)f", hook)[-1])
        for ink, background in zip(self.colors("ink"), ((0, 0, 0), (255, 255, 255))):
            for value in range(256):
                surface = composite(background, (value,) * 3, film)
                self.assertGreaterEqual(contrast(composite(ink, surface, alpha), surface), 4.5)

    def test_menu_and_quick_action_labels_on_extreme_artwork(self):
        settings = (JAVA / "SquareSettings.java").read_text(encoding="utf-8")
        match = re.search(r"readableOpacity\(boolean dark\).*?dark \? ([.\d]+)f : ([.\d]+)f", settings, re.S)
        pane = (ROOT / "extension/src/main/kotlin/dev/nino/squareglass/LiquidMenuSurface.kt").read_text(encoding="utf-8")
        emphasis = float(re.search(r"if \(emphasized\) drawRect.*?alpha = ([.\d]+)f", pane)[1])
        for ink, film, opacity in zip(self.colors("ink"), ((51, 52, 58), (255, 255, 255)), map(float, match.groups())):
            for value in range(256):
                background = composite(film, (value,) * 3, opacity)
                for alpha in (0, emphasis):
                    self.assertGreaterEqual(contrast(ink, composite(ink, background, alpha)), 4.5)


if __name__ == "__main__":
    unittest.main()
