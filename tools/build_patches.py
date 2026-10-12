"""Build, strictly verify, and package Liquidify patches from source."""
import json
import shutil
import subprocess
import sys
import zipfile
from build_runtime import ROOT


def run(script):
    subprocess.run([sys.executable, str(ROOT / 'tools' / script)], cwd=ROOT, check=True)


def main():
    run('prepare_release_inputs.py')
    run('build_probe.py')
    run('prepare_patch_sources.py')
    run('build_design_resources.py')
    run('verify_strict_patches.py')
    run('package_alpha.py')
    release = json.loads((ROOT / 'release.json').read_text())
    archive = ROOT / 'dist' / f'Liquidify-{release["version"]}-patches.zip'
    target = ROOT / release['patches']
    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(archive, target)
    with zipfile.ZipFile(target) as patches:
        target.with_name('manifest.json').write_bytes(patches.read('manifest.json'))
    run('validate_release.py')


if __name__ == '__main__':
    main()
