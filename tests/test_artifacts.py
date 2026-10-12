"""Checks real artifacts; skips integration fixtures when they are not built."""
from pathlib import Path
import hashlib
import json
import struct
import unittest
import zipfile

ROOT = Path(__file__).resolve().parents[1]


def chunks(data, start=0, end=None):
    end = len(data) if end is None else end
    while start < end:
        kind, header, size = struct.unpack_from("<HHI", data, start)
        if size < header or header < 8 or start + size > end:
            raise AssertionError("Invalid resource chunk bounds")
        yield kind, header, data[start:start + size]
        start += size
    if start != end: raise AssertionError("Truncated resource table")


def leaves(data):
    for kind, header, block in chunks(data):
        if kind in (0x2, 0x200):
            yield from leaves(block[header:])
        else:
            yield kind, block


class ArtifactSafety(unittest.TestCase):
    @unittest.skipUnless((ROOT / "research/square-resources.arsc").exists(), "Build resources first")
    def test_unrelated_resource_chunks_are_byte_exact(self):
        with zipfile.ZipFile(ROOT / "research/tidal-stock-2.200.0.apk") as apk:
            original = list(leaves(apk.read("resources.arsc")))
        patched = list(leaves((ROOT / "research/square-resources.arsc").read_bytes()))
        self.assertEqual(len(original), len(patched))
        modified_types = set()
        for (old_kind, old), (new_kind, new) in zip(original, patched):
            self.assertEqual(old_kind, new_kind)
            if old != new:
                self.assertIn(old_kind, (0x201, 0x202))
                self.assertEqual(old[8], 6, "Only color type 6 may change")
                modified_types.add(old_kind)
        self.assertEqual(modified_types, {0x201, 0x202})
        # String pools and every layout/style/string resource stay byte-exact.

    @unittest.skipUnless((ROOT / "reports/package-alpha.json").exists(), "Package first")
    def test_zip_contents_and_fingerprints(self):
        report = json.loads((ROOT / "reports/package-alpha.json").read_text())
        path = ROOT / "dist" / report["file"]
        self.assertEqual(hashlib.sha256(path.read_bytes()).hexdigest(), report["sha256"])
        with zipfile.ZipFile(path) as pack:
            names = pack.namelist()
            self.assertEqual(len(names), len(set(names)))
            self.assertFalse(any(n.endswith((".apk", ".dex", ".png", ".jpg", ".keystore")) for n in names))
            self.assertFalse(any(".." in Path(n).parts or n.startswith(("/", "extension/androidx/", "extension/kotlin/")) for n in names))
            checksums = json.loads(pack.read("checksums.json"))
            self.assertEqual(set(names), set(checksums) | {"checksums.json"})
            for name, expected in checksums.items():
                self.assertEqual(hashlib.sha256(pack.read(name)).hexdigest(), expected)
            manifest = json.loads(pack.read("manifest.json"))
            for spec in manifest["patches"]:
                for helper in spec.get("extensionFiles", []):
                    self.assertIn("extension/" + helper, names)
                for patch in spec.get("fileNames", []):
                    self.assertTrue(any(n.rsplit("/", 1)[-1] == patch for n in names), patch)
            settings = pack.read("extension/dev/nino/squareglass/SquareSettings.smali").decode()
            self.assertIn("__RL_SQUARE_MANAGER_READY__", settings)
            self.assertIn("__RL_SQUARE_THEME_MODE__", settings)
            self.assertIn("__RL_SQUARE_THEME_ENABLED__", settings)
            self.assertEqual(manifest["compatibility"]["minSdk"], 33)
            for font in manifest["designResources"]["fonts"]:
                self.assertEqual(hashlib.sha256(pack.read(font["source"])).hexdigest(), font["sha256"])
            for rule in manifest["designResources"]["themeXml"]:
                with zipfile.ZipFile(ROOT / "research/tidal-stock-2.200.0.apk") as apk:
                    self.assertEqual(hashlib.sha256(apk.read(rule["target"])).hexdigest(), rule["originalSha256"])
                replacement = ROOT / "research/design-xml" / rule["target"]
                self.assertEqual(hashlib.sha256(replacement.read_bytes()).hexdigest(), rule["sha256"])


if __name__ == "__main__":
    unittest.main()
