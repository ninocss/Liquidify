from pathlib import Path
import subprocess
import unittest
ROOT=Path(__file__).resolve().parents[1]
from tools.build_runtime import java_tool
class PageTests(unittest.TestCase):
    def test_header_round_trip_at_multiple_densities(self):
        output=ROOT/'build/page-checks';output.mkdir(parents=True,exist_ok=True)
        subprocess.run([str(java_tool('javac')),'-d',str(output),str(ROOT/'extension/src/main/java/dev/nino/squareglass/HeaderProgress.java'),str(ROOT/'tests/HeaderProgressChecks.java')],check=True)
        subprocess.run([str(java_tool('java')),'-cp',str(output),'dev.nino.squareglass.HeaderProgressChecks'],check=True,capture_output=True)
