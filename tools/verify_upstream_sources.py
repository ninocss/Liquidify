"""Verify restored original sources directly against the locked upstream commit."""
import hashlib
import json
import urllib.request
from concurrent.futures import ThreadPoolExecutor
from build_runtime import ROOT
from upstream_compatibility import helper_closure, RESTORED


def main():
    base=ROOT/'radiant-base/patches'
    revision=json.loads((ROOT/'upstream-lock.json').read_text())['radiant']['commit']
    files={'manifest.json'}
    specs=json.loads((base/'manifest.json').read_text())['patches']
    paths={p.name:p.relative_to(base).as_posix() for p in base.rglob('*.patch')}
    for spec in specs:
        if spec['id'] not in RESTORED:continue
        files.update(paths[name] for name in spec.get('fileNames',[]))
        files.update('extension/'+name for name in helper_closure(spec.get('extensionFiles',[])))
        for item in spec.get('variants',[])+spec.get('advancedOptions',[]):
            files.update(paths[name] for name in item.get('fileNames',[]))
            files.update('extension/'+name for name in helper_closure(item.get('extensionFiles',[])))
    files.add('extension/radiant/HomeBackdrop.smali')
    def verify(name):
        request=urllib.request.Request(f'https://raw.githubusercontent.com/meowarex/rl-mobile/{revision}/patches/{name}',headers={'User-Agent':'Liquidify-source-verification'})
        with urllib.request.urlopen(request,timeout=30) as response:data=response.read()
        canonical=data.replace(b'\r\n',b'\n')
        local=(base/name).read_bytes().replace(b'\r\n',b'\n')
        if canonical!=local:raise ValueError('Upstream source differs: '+name)
        return name,hashlib.sha256(canonical).hexdigest()
    with ThreadPoolExecutor(max_workers=4) as pool:hashes=dict(pool.map(verify,sorted(files)))
    report={'passed':True,'commit':revision,'files':hashes}
    (ROOT/'reports/upstream-source-verification.json').write_text(json.dumps(report,indent=2)+'\n')
    print(json.dumps({'passed':True,'commit':revision,'files':len(hashes)}))


if __name__=='__main__':main()
