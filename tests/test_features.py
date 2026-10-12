from pathlib import Path
import subprocess
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = Path(r'C:/Program Files/Android/Android Studio/jbr/bin')


class FeatureTests(unittest.TestCase):
    def test_time_taste(self):
        output = ROOT / 'build/feature-checks'
        output.mkdir(parents=True, exist_ok=True)
        source = ROOT / 'extension/src/main/java/dev/nino/squareglass'
        subprocess.run([str(JAVA / 'javac.exe'), '-d', str(output), str(source / 'TimeTaste.java'),
                        str(ROOT / 'tests/FeatureChecks.java')], check=True)
        result = subprocess.run([str(JAVA / 'java.exe'), '-cp', str(output), 'dev.nino.squareglass.FeatureChecks'], capture_output=True, text=True, check=True)
        self.assertIn('5 feature checks passed', result.stdout)


if __name__ == '__main__':
    unittest.main()
