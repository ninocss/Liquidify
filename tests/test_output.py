import subprocess
import unittest
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
JAVA=Path('C:/Program Files/Android/Android Studio/jbr/bin')


class OutputTests(unittest.TestCase):
    def test_confirmed_selection_and_stale_responses(self):
        output=ROOT/'build/output-checks'
        output.mkdir(parents=True,exist_ok=True)
        subprocess.run([str(JAVA/'javac.exe'),'-d',str(output),
                        str(ROOT/'extension/src/main/java/dev/nino/squareglass/OutputSelection.java'),
                        str(ROOT/'tests/OutputSelectionChecks.java')],check=True)
        subprocess.run([str(JAVA/'java.exe'),'-cp',str(output),'dev.nino.squareglass.OutputSelectionChecks'],check=True)
