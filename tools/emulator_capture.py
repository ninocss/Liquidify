"""Private visual QA capture of the explicitly scoped test emulator only."""
from pathlib import Path
import argparse
import subprocess
p=argparse.ArgumentParser(description=__doc__)
p.add_argument('name');args=p.parse_args()
if not args.name.replace('-','').replace('_','').isalnum():raise SystemExit('Use a simple capture name')
root=Path(__file__).resolve().parents[1]
path=root/'research'/f'{args.name}.png'
path.write_bytes(subprocess.check_output(['C:/platform-tools/adb.exe','-s','emulator-5554','exec-out','screencap','-p']))
print(path)
