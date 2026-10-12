import subprocess
import unittest
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
from tools.build_runtime import java_tool


class OutputTests(unittest.TestCase):
    def test_confirmed_selection_and_stale_responses(self):
        output=ROOT/'build/output-checks'
        output.mkdir(parents=True,exist_ok=True)
        subprocess.run([str(java_tool('javac')),'-d',str(output),
                        str(ROOT/'extension/src/main/java/dev/nino/squareglass/OutputSelection.java'),
                        str(ROOT/'tests/OutputSelectionChecks.java')],check=True)
        subprocess.run([str(java_tool('java')),'-cp',str(output),'dev.nino.squareglass.OutputSelectionChecks'],check=True)
