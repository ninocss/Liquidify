from pathlib import Path
import subprocess
import unittest
ROOT=Path(__file__).resolve().parents[1]
JAVA=Path('C:/Program Files/Android/Android Studio/jbr/bin')
class MotionTests(unittest.TestCase):
    def test_idle_quiet_time_cadence_hold_and_eligibility(self):
        output=ROOT/'build/motion-checks';output.mkdir(parents=True,exist_ok=True)
        subprocess.run([str(JAVA/'javac.exe'),'-d',str(output),str(ROOT/'extension/src/main/java/dev/nino/squareglass/MotionPolicy.java'),str(ROOT/'tests/MotionPolicyChecks.java')],check=True)
        subprocess.run([str(JAVA/'java.exe'),'-cp',str(output),'dev.nino.squareglass.MotionPolicyChecks'],check=True,capture_output=True)
