from pathlib import Path
import subprocess
import unittest
ROOT=Path(__file__).resolve().parents[1]
JAVA=Path('C:/Program Files/Android/Android Studio/jbr/bin')
class PlaybackProgressTests(unittest.TestCase):
    def test_native_duration_boundaries_and_queue_switch(self):
        output=ROOT/'build/progress-checks';output.mkdir(parents=True,exist_ok=True)
        subprocess.run([str(JAVA/'javac.exe'),'-d',str(output),str(ROOT/'extension/src/main/java/dev/nino/squareglass/PlaybackProgress.java'),str(ROOT/'tests/PlaybackProgressChecks.java')],check=True)
        subprocess.run([str(JAVA/'java.exe'),'-cp',str(output),'dev.nino.squareglass.PlaybackProgressChecks'],check=True,capture_output=True)
