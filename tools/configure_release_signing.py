"""Create a dedicated release key locally; optionally install its GitHub Actions secrets.

Private files stay in ignored research/release-signing. Back up this directory securely.
Uses normal keytool and GitHub CLI authentication; never reads an existing Manager key.
"""
import argparse
import base64
import hashlib
import os
from pathlib import Path
import secrets
import subprocess
from build_runtime import ROOT, java_tool


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--upload', action='store_true')
    parser.add_argument('--gh', type=Path, default=ROOT / 'research/github-cli/gh.exe')
    args = parser.parse_args()
    directory = ROOT / 'research/release-signing'
    directory.mkdir(parents=True, exist_ok=True)
    store = directory / 'liquidify-release.jks'
    password_file = directory / 'password.txt'
    alias_file = directory / 'alias.txt'
    if not store.exists():
        if password_file.exists() or alias_file.exists():
            raise RuntimeError('Incomplete signing setup; inspect private files before retrying')
        password = secrets.token_urlsafe(36)
        alias = 'liquidify-release'
        password_file.write_text(password)
        alias_file.write_text(alias)
        environment = dict(os.environ, LIQUIDIFY_KEY_PASSWORD=password)
        subprocess.run([str(java_tool('keytool')), '-genkeypair', '-noprompt',
                        '-keystore', str(store), '-storetype', 'JKS',
                        '-storepass:env', 'LIQUIDIFY_KEY_PASSWORD',
                        '-keypass:env', 'LIQUIDIFY_KEY_PASSWORD', '-alias', alias,
                        '-keyalg', 'RSA', '-keysize', '3072', '-validity', '10000',
                        '-dname', 'CN=Liquidify, OU=Release, O=ninocss'],
                       env=environment, check=True, capture_output=True)
    password = password_file.read_text().strip()
    alias = alias_file.read_text().strip()
    encoded = base64.b64encode(store.read_bytes())
    (directory / 'keystore-base64.txt').write_bytes(encoded)
    environment = dict(os.environ, LIQUIDIFY_KEY_PASSWORD=password)
    certificate = subprocess.run([str(java_tool('keytool')), '-exportcert',
                                  '-keystore', str(store), '-storepass:env',
                                  'LIQUIDIFY_KEY_PASSWORD', '-alias', alias],
                                 env=environment, check=True, capture_output=True).stdout
    fingerprint = hashlib.sha256(certificate).hexdigest()
    (directory / 'certificate-sha256.txt').write_text(fingerprint + '\n')
    print('Release certificate SHA-256:', fingerprint)
    if args.upload:
        subprocess.run([str(args.gh), 'auth', 'status'], check=True, capture_output=True)
        values = {'LIQUIDIFY_SIGNING_KEYSTORE': encoded,
                  'LIQUIDIFY_SIGNING_PASSWORD': password.encode(),
                  'LIQUIDIFY_SIGNING_ALIAS': alias.encode()}
        for name, value in values.items():
            subprocess.run([str(args.gh), 'secret', 'set', name, '--repo', 'ninocss/liquidify'],
                           input=value, check=True, capture_output=True)
            print('Configured:', name)


if __name__ == '__main__':
    main()
