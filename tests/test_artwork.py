from pathlib import Path
import subprocess
import unittest

ROOT = Path(__file__).resolve().parents[1]
from tools.build_runtime import java_tool


class ArtworkTests(unittest.TestCase):
    def test_readable_cover_palette(self):
        output = ROOT / 'build/artwork-checks'
        output.mkdir(parents=True, exist_ok=True)
        subprocess.run([str(java_tool('javac')), '-d', str(output),
                        str(ROOT / 'extension/src/main/java/dev/nino/squareglass/CoverColors.java'),
                        str(ROOT / 'tests/ArtworkChecks.java')], check=True)
        result = subprocess.run([str(java_tool('java')), '-cp', str(output), 'dev.nino.squareglass.ArtworkChecks'], capture_output=True, text=True, check=True)
        self.assertIn('16384 artwork contrast cases', result.stdout)

