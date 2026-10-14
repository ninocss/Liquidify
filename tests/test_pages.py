from pathlib import Path
import subprocess
import unittest
import json
ROOT=Path(__file__).resolve().parents[1]
from tools.build_runtime import java_tool
class PageTests(unittest.TestCase):
    @unittest.skipUnless((ROOT/'research/host-index.json').exists(), 'Build host index first')
    def test_pinned_host_exposes_disappearing_fragment_transition_lists(self):
        host=json.loads((ROOT/'research/host-index.json').read_text())
        fields=host['Landroidx/fragment/app/FragmentContainerView;']['fields']
        self.assertIn('transitioningFragmentViews:Ljava/util/List;',fields)
        self.assertIn('disappearingFragmentChildren:Ljava/util/List;',fields)

    def test_backdrop_capture_waits_for_exit_and_stable_traversals(self):
        output=ROOT/'build/backdrop-capture-checks';output.mkdir(parents=True,exist_ok=True)
        subprocess.run([str(java_tool('javac')),'-d',str(output),str(ROOT/'extension/src/main/java/dev/nino/squareglass/BackdropCapturePolicy.java'),str(ROOT/'tests/BackdropCaptureChecks.java')],check=True)
        subprocess.run([str(java_tool('java')),'-cp',str(output),'dev.nino.squareglass.BackdropCaptureChecks'],check=True,capture_output=True)

    def test_header_round_trip_at_multiple_densities(self):
        output=ROOT/'build/page-checks';output.mkdir(parents=True,exist_ok=True)
        subprocess.run([str(java_tool('javac')),'-d',str(output),str(ROOT/'extension/src/main/java/dev/nino/squareglass/HeaderProgress.java'),str(ROOT/'tests/HeaderProgressChecks.java')],check=True)
        subprocess.run([str(java_tool('java')),'-cp',str(output),'dev.nino.squareglass.HeaderProgressChecks'],check=True,capture_output=True)
