"""Apply every generated diff with the Manager engine to fresh stock DEX text."""
from pathlib import Path
import hashlib
import json
import subprocess
from build_runtime import java_tool
from check_linkage import audit

ROOT=Path(__file__).resolve().parents[1]
JAVA=java_tool('java')


def main():
    targets=set()
    patches=sorted((ROOT/'experimental-patches').glob('*.patch'))
    for patch in patches:
        target=next(line[6:] for line in patch.read_text().splitlines() if line.startswith('--- a/'))
        targets.add('L'+target.removesuffix('.smali')+';')
    inventory=ROOT/'research/strict-output-targets.txt'
    inventory.write_text('\n'.join(sorted(targets))+'\n')
    def run(*args):
        return subprocess.run([str(JAVA),'-cp','build/dex-tools/*',*map(str,args)],cwd=ROOT,check=True,capture_output=True,text=True).stdout
    run('tools/DexTool.java','disassemble-targets','research/tidal-stock-2.200.0.apk','research/strict-output-original',inventory)
    result=run('tools/StrictPatchCheck.java','research/strict-output-original','experimental-patches','research/probe-hook-smali')
    # Diff application alone cannot detect invalid host method signatures. Audit
    # every reference introduced by the native hooks, including its return type.
    for name, source in (('original', 'strict-output-original'), ('patched', 'probe-hook-smali')):
        run('tools/DexTool.java', 'assemble', 'research/'+source, 'research/hooks-'+name+'.dex')
        run('tools/DexTool.java', 'refs', 'research/hooks-'+name+'.dex', 'research/hooks-'+name+'-refs.json')
    run('tools/DexTool.java', 'index', 'research/hooks-patched.dex', 'research/hooks-patched-index.json')
    original=json.loads((ROOT/'research/hooks-original-refs.json').read_text())
    patched=json.loads((ROOT/'research/hooks-patched-refs.json').read_text())
    introduced={kind:sorted(set(patched[kind])-set(original[kind])) for kind in original}
    host=json.loads((ROOT/'research/host-index.json').read_text())
    host.update(json.loads((ROOT/'research/hooks-patched-index.json').read_text()))
    extension=json.loads((ROOT/'research/probe-remapped-index.json').read_text())
    linkage=audit(host,extension,introduced)
    linkage['introducedReferences']={kind:len(refs) for kind,refs in introduced.items()}
    linkage['hooksDexSha256']=hashlib.sha256((ROOT/'research/hooks-patched.dex').read_bytes()).hexdigest()
    (ROOT/'reports/hook-linkage.json').write_text(json.dumps(linkage,indent=2)+'\n')
    if not linkage['passed']:
        raise SystemExit('Native hook linkage failed: '+json.dumps(linkage))
    report=dict(passed=True,patches=len(patches),engine='Manager strict java-diff-utils',
                patchSha256={patch.name:hashlib.sha256(patch.read_bytes()).hexdigest() for patch in patches},
                source='Fresh pinned stock APK disassembly',expected='research/probe-hook-smali',
                extensionDexSha256=hashlib.sha256((ROOT/'research/probe-remapped.dex').read_bytes()).hexdigest(),
                hookLinkage=linkage,
                result=result.strip())
    (ROOT/'reports/strict-patches-alpha22.json').write_text(json.dumps(report,indent=2)+'\n')
    print(result)


if __name__=='__main__':main()
