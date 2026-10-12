"""Require the stable public signing certificate recorded for Liquidify releases."""
import json
import os
import re
import subprocess
import sys
from build_runtime import ROOT, SDK_HOME


def main():
    tool = SDK_HOME / 'build-tools/37.0.0' / ('apksigner.bat' if os.name == 'nt' else 'apksigner')
    result = subprocess.run([str(tool), 'verify', '--print-certs', sys.argv[1]],
                            check=True, capture_output=True, text=True)
    digests = re.findall(r'^.*certificate SHA-256 digest: ([0-9a-f]{64})\s*$', result.stdout, re.M)
    expected = json.loads((ROOT / 'release.json').read_text())['managerSigningCertificateSha256']
    assert digests and set(digests) == {expected}, 'Wrong Manager release signing identity'
    print(json.dumps({'passed': True, 'certificateSha256': expected}))


if __name__ == '__main__':
    main()
