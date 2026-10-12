from pathlib import Path
import subprocess
import unittest
ROOT=Path(__file__).resolve().parents[1]
from tools.build_runtime import java_tool
class PlaybackProgressTests(unittest.TestCase):
    def test_native_duration_boundaries_and_queue_switch(self):
        output=ROOT/'build/progress-checks';output.mkdir(parents=True,exist_ok=True)
        subprocess.run([str(java_tool('javac')),'-d',str(output),str(ROOT/'extension/src/main/java/dev/nino/squareglass/PlaybackProgress.java'),str(ROOT/'tests/PlaybackProgressChecks.java')],check=True)
        subprocess.run([str(java_tool('java')),'-cp',str(output),'dev.nino.squareglass.PlaybackProgressChecks'],check=True,capture_output=True)
