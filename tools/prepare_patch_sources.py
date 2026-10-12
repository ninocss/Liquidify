"""Regenerate all native hooks from the pinned decoded stock APK."""
from pathlib import Path
import shutil
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]

def run(*args):
    subprocess.run([str(a) for a in args], cwd=ROOT, check=True)

def main():
    hooks = ROOT / "research/probe-hook-smali"
    if hooks.resolve().parent != (ROOT / "research").resolve():
        raise SystemExit("Unexpected hook build directory")
    if hooks.exists():
        shutil.rmtree(hooks)
    run(sys.executable, "tools/prepare_probe_patch.py")
    run(sys.executable, "tools/prepare_shell_patch.py")
    run(sys.executable, "tools/prepare_theme_patch.py")
    run(sys.executable, "tools/prepare_component_patch.py")
    run(sys.executable, "tools/prepare_visual_patch.py")
    run(sys.executable, "tools/prepare_home_patch.py")
    run(sys.executable, "tools/prepare_screens_patch.py")
    run(sys.executable, "tools/prepare_runtime_patch.py")
    run(sys.executable, "tools/prepare_features_patch.py")
    run(sys.executable, "tools/prepare_artwork_patch.py")
    run(sys.executable, "tools/prepare_polish_patch.py")
    run(sys.executable, "tools/prepare_pages_patch.py")
    run(sys.executable, "tools/prepare_motion_patch.py")
    run(sys.executable, "tools/prepare_output_patch.py")
    run(sys.executable, "tools/prepare_linn_patch.py")

if __name__ == "__main__":
    main()
