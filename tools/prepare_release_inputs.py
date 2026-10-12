"""Download hash-pinned patch-build inputs and decode the exact stock APK."""
import hashlib
import json
import subprocess
import urllib.request
from pathlib import Path
from build_runtime import ROOT, java_tool


def download(url, target, expected):
    target.parent.mkdir(parents=True, exist_ok=True)
    if target.exists() and hashlib.sha256(target.read_bytes()).hexdigest() == expected:
        return
    temporary = target.with_suffix(target.suffix + '.download')
    try:
        request = urllib.request.Request(url, headers={'User-Agent': 'Liquidify-build'})
        with urllib.request.urlopen(request, timeout=120) as response, temporary.open('wb') as out:
            import shutil
            shutil.copyfileobj(response, out)
        actual = hashlib.sha256(temporary.read_bytes()).hexdigest()
        if actual != expected:
            raise ValueError(f'Unexpected SHA-256 for {target.name}: {actual}')
        temporary.replace(target)
    finally:
        temporary.unlink(missing_ok=True)


def main():
    lock = json.loads((ROOT / 'upstream-lock.json').read_text())
    tidal = ROOT / 'research/tidal-stock-2.200.0.apk'
    jar = ROOT / 'research/apktool.jar'
    download(lock['tidal']['url'], tidal, lock['tidal']['sha256'])
    download(lock['apktool']['url'], jar, lock['apktool']['sha256'])
    square = lock['square']
    font = square['font']
    download(f"https://raw.githubusercontent.com/Lelonio/Square/{square['commit']}/{font['path']}",
             ROOT / 'square-reference' / font['path'], font['sha256'])
    marker = ROOT / 'research/decoded-release-inputs.json'
    identity = {'apk': lock['tidal']['sha256'], 'apktool': lock['apktool']['sha256']}
    decoded = (ROOT / 'research/tidal').resolve()
    if decoded.parent != (ROOT / 'research').resolve():
        raise ValueError('Decode target must remain inside the build workspace')
    if marker.exists() and json.loads(marker.read_text()) == identity and (decoded / 'smali').is_dir():
        return
    subprocess.run([str(java_tool('java')), '-jar', str(jar), 'd', '-f', '-r',
                    str(tidal), '-o', str(decoded)], cwd=ROOT, check=True)
    marker.write_text(json.dumps(identity) + '\n')


if __name__ == '__main__':
    main()
