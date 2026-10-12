import subprocess
import unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
JAVA=Path('C:/Program Files/Android/Android Studio/jbr/bin')
class LinnQueueTests(unittest.TestCase):
    def test_lan_xml_and_http_bounds(self):
        output=ROOT/'build/linn-protocol-checks';output.mkdir(parents=True,exist_ok=True)
        android=Path('C:/Users/nino/AppData/Local/Android/Sdk/platforms/android-36/android.jar')
        sources=[ROOT/'extension/src/main/java/dev/nino/squareglass'/name for name in ['OpenHome.java','LinnEvents.java','LinnQueuePlan.java']]
        subprocess.run([str(JAVA/'javac.exe'),'-cp',str(android),'-d',str(output),*[str(p) for p in sources],str(ROOT/'tests/OpenHomeChecks.java')],check=True)
        subprocess.run([str(JAVA/'java.exe'),'-cp',str(output)+';'+str(android),'dev.nino.squareglass.OpenHomeChecks'],check=True)
    def test_capacity_occurrences_and_event_sequence_wrap(self):
        output=ROOT/'build/linn-queue-checks';output.mkdir(parents=True,exist_ok=True)
        subprocess.run([str(JAVA/'javac.exe'),'-d',str(output),str(ROOT/'extension/src/main/java/dev/nino/squareglass/LinnQueuePlan.java'),str(ROOT/'tests/LinnQueueChecks.java')],check=True)
        subprocess.run([str(JAVA/'java.exe'),'-cp',str(output),'dev.nino.squareglass.LinnQueueChecks'],check=True)
