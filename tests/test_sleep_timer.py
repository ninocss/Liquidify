from pathlib import Path
import subprocess
import unittest

ROOT=Path(__file__).resolve().parents[1]
from tools.build_runtime import java_tool

class SleepTimerTests(unittest.TestCase):
    def test_replace_cancel_expiry_and_one_shot(self):
        output=ROOT/'build/sleep-checks'
        output.mkdir(parents=True,exist_ok=True)
        subprocess.run([str(java_tool('javac')),'-d',str(output),
                        str(ROOT/'extension/src/main/java/dev/nino/squareglass/SleepDeadline.java'),
                        str(ROOT/'extension/src/main/java/dev/nino/squareglass/TrackEndGate.java'),
                        str(ROOT/'tests/SleepTimerChecks.java')],check=True)
        result=subprocess.run([str(java_tool('java')),'-cp',str(output),'dev.nino.squareglass.SleepTimerChecks'],capture_output=True,text=True,check=True)
        self.assertIn('12 sleep deadline checks passed',result.stdout)
