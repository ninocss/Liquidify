from pathlib import Path
import subprocess
import unittest

ROOT = Path(__file__).resolve().parents[1]
from tools.build_runtime import java_tool


class FeatureTests(unittest.TestCase):
    def test_time_taste(self):
        output = ROOT / 'build/feature-checks'
        output.mkdir(parents=True, exist_ok=True)
        source = ROOT / 'extension/src/main/java/dev/nino/squareglass'
        subprocess.run([str(java_tool('javac')), '-d', str(output), str(source / 'TimeTaste.java'),
                        str(ROOT / 'tests/FeatureChecks.java')], check=True)
        result = subprocess.run([str(java_tool('java')), '-cp', str(output), 'dev.nino.squareglass.FeatureChecks'], capture_output=True, text=True, check=True)
        self.assertIn('5 feature checks passed', result.stdout)


if __name__ == '__main__':
    unittest.main()
