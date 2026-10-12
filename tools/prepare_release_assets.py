"""Create the Manager's GitHub release contract from the verified APK and patches."""
import hashlib
import json
import shutil
import sys
import zipfile
from pathlib import Path
from verify_manager_bundle import ROOT, verify


def main():
    apk = Path(sys.argv[1])
    verify(apk)
    release = json.loads((ROOT / 'release.json').read_text())
    output = ROOT / 'release-assets'
    output.mkdir(exist_ok=True)
    expected = {f'Liquidify-Manager-{release["version"]}.apk', 'patches.zip',
                'manifest.json', 'data.json', 'SHA256SUMS'}
    assert not any(p.name not in expected for p in output.iterdir()), 'Unexpected release asset'
    shutil.copyfile(apk, output / f'Liquidify-Manager-{release["version"]}.apk')
    shutil.copyfile(ROOT / release['patches'], output / 'patches.zip')
    with zipfile.ZipFile(apk) as manager:
        (output / 'data.json').write_bytes(manager.read('assets/liquidify/data.json'))
    with zipfile.ZipFile(output / 'patches.zip') as patches:
        (output / 'manifest.json').write_bytes(patches.read('manifest.json'))
    files = sorted(p for p in output.iterdir() if p.name != 'SHA256SUMS')
    (output / 'SHA256SUMS').write_text(''.join(
        f'{hashlib.sha256(p.read_bytes()).hexdigest()}  {p.name}\n' for p in files))
    print(json.dumps({'passed': True, 'assets': [p.name for p in files]}))


if __name__ == '__main__':
    main()
