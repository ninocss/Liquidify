"""Apply every generated diff with the Manager engine to fresh stock DEX text."""
from pathlib import Path
import hashlib
import json
import subprocess
from build_runtime import java_tool

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
    report=dict(passed=True,patches=len(patches),engine='Manager strict java-diff-utils',
                source='Fresh pinned stock APK disassembly',expected='research/probe-hook-smali',
                extensionDexSha256=hashlib.sha256((ROOT/'research/probe-remapped.dex').read_bytes()).hexdigest(),
                result=result.strip())
    (ROOT/'reports/strict-patches-alpha22.json').write_text(json.dumps(report,indent=2)+'\n')
    print(result)


if __name__=='__main__':main()
