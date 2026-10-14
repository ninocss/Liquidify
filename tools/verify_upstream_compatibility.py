"""Check the original v1.1.8 WIP patches against Liquidify's actual native hooks."""
from pathlib import Path
import hashlib
import json
import shutil
import subprocess
import re
import struct
from build_runtime import ROOT, java_tool
from upstream_compatibility import restore_specs
from check_linkage import audit

UPSTREAM=ROOT/'radiant-base/patches'


def configurations():
    specs={p['id']:p for p in restore_specs(json.loads((UPSTREAM/'manifest.json').read_text())['patches'])}
    out={}
    debug=specs['DebugMenuUnlock']['fileNames']
    out['DebugMenuUnlock']=debug
    lyrics=specs['LyricsRlApi']
    for index,variant in enumerate(lyrics['variants']):
        out[f'LyricsRlApi-{index}']=lyrics['fileNames']+variant.get('fileNames',[])
        out[f'DebugAndLyrics-{index}']=debug+out[f'LyricsRlApi-{index}']
    backdrop=specs['PlayerBackdrop']
    cover=next(o for o in backdrop['advancedOptions'] if o['key']=='cover_everywhere')['fileNames']
    for index,variant in enumerate(backdrop['variants']):
        out[f'PlayerBackdrop-{index}']=variant['fileNames']
        out[f'CoverEverywhere-{index}']=variant['fileNames']+cover
        for lyric_index in range(len(lyrics['variants'])):
            out[f'All-{lyric_index}-{index}']=debug+out[f'LyricsRlApi-{lyric_index}']+variant['fileNames']+cover
    return out


def substitutions(specs):
    tokens={}
    for spec in specs:
        for option in spec.get('advancedOptions',[]):
            if not option.get('token'):continue
            kind=option['type'];value=option.get('default',False)
            if kind=='choice':value=option['values'][option.get('defaultIndex',0)]
            elif kind=='toggle':value='0x1' if value else '0x0'
            elif kind=='slider':
                encode=option.get('encode',{'kind':'int','scale':1})
                value=value*encode.get('scale',1)
                if encode['kind']=='floatBits':value=struct.unpack('<i',struct.pack('<f',value))[0]
                elif encode['kind']=='argbAlpha':value=int(value/100*255)<<24
                else:value=int(value)
            tokens['__'+option['token']+'__']=str(value)
    return tokens


def dex(command,source,destination):
    run=subprocess.run([str(java_tool('java')),'-cp','build/dex-tools/*','tools/DexTool.java',command,str(source),str(destination)],cwd=ROOT,text=True,capture_output=True)
    if run.returncode:raise RuntimeError(run.stderr[-4000:])


def linkage(name,output,specs,baseline_refs):
    helpers=set();tokens=substitutions(specs)
    for spec in specs:
        if (spec['id']=='DebugMenuUnlock' and ('Debug' in name or name.startswith('All-'))) or \
           (spec['id']=='LyricsRlApi' and ('Lyrics' in name or name.startswith('All-'))) or \
           (spec['id']=='PlayerBackdrop' and ('Backdrop' in name or 'Everywhere' in name or name.startswith('All-'))):
            helpers.update(spec.get('extensionFiles',[]))
            index=int(name.split('-')[-1]) if spec['id']!='DebugMenuUnlock' else 0
            if spec['id']=='LyricsRlApi' and name.startswith('All-'):index=int(name.split('-')[1])
            variants=spec.get('variants',[])
            if variants:helpers.update(variants[index].get('extensionFiles',[]))
    for helper in helpers:
        path=output/helper;path.parent.mkdir(parents=True,exist_ok=True)
        text=path.read_text() if path.exists() else (UPSTREAM/'extension'/helper).read_text()
        path.write_text(text,newline='\n')
    # Manager substitutes both extracted helpers and patched native instructions.
    for path in output.rglob('*.smali'):
        text=path.read_text()
        for key,value in tokens.items():text=text.replace(key,value)
        if re.search(r'__RL_[A-Z0-9_]+__',text):raise ValueError('Unresolved option token: '+str(path))
        path.write_text(text,newline='\n')
    target=ROOT/'research'/f'upstream-{name}.dex'
    dex('assemble',output,target)
    dex('index',target,target.with_suffix('.index.json'))
    dex('refs',target,target.with_suffix('.refs.json'))
    host=json.loads((ROOT/'research/host-index.json').read_text())
    index=json.loads(target.with_suffix('.index.json').read_text())
    extensions=json.loads((ROOT/'research/probe-remapped-index.json').read_text())
    for key,value in index.items():
        if key in host:host[key]=value
        else:extensions[key]=value
    refs=json.loads(target.with_suffix('.refs.json').read_text())
    introduced={kind:sorted(set(values)-set(baseline_refs[kind])) for kind,values in refs.items()}
    return audit(host,extensions,introduced)


def main():
    configs=configurations()
    paths={p.name:p for p in list(UPSTREAM.rglob('*.patch'))+list((ROOT/'compatibility-patches').glob('*.patch'))}
    targets=set()
    for names in configs.values():
        for name in names:
            targets.update(line[6:] for line in paths[name].read_text().splitlines() if line.startswith('--- a/'))
    base=ROOT/'research/upstream-compat-base';base.mkdir(exist_ok=True)
    for target in targets:
        hook=ROOT/'research/probe-hook-smali'/target
        stock=next((ROOT/'research/tidal').glob('smali*/'+target),None)
        source=hook if hook.exists() else stock or UPSTREAM/'extension'/target
        dest=base/target;dest.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(source,dest)
    results={}
    baseline=ROOT/'research/upstream-native-baseline';baseline.mkdir(exist_ok=True)
    for target in targets:
        if target.startswith('radiant/'):continue
        dest=baseline/target;dest.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(base/target,dest)
    dex('assemble',baseline,ROOT/'research/upstream-baseline.dex')
    dex('refs',ROOT/'research/upstream-baseline.dex',ROOT/'research/upstream-baseline-refs.json')
    baseline_refs=json.loads((ROOT/'research/upstream-baseline-refs.json').read_text())
    specs=restore_specs(json.loads((UPSTREAM/'manifest.json').read_text())['patches'])
    for name,names in configs.items():
        plan=ROOT/'research'/f'upstream-{name}.txt'
        ordered=sorted((paths[n] for n in names),key=lambda p: ('compatibility/'+p.name) if p.parent.name=='compatibility-patches' else p.relative_to(UPSTREAM).as_posix())
        plan.write_text('\n'.join(str(p) for p in ordered)+'\n')
        output=ROOT/'research/upstream-compat'/name
        if output.resolve().parent != (ROOT/'research/upstream-compat').resolve():raise ValueError('Unsafe output directory')
        if output.exists():shutil.rmtree(output)
        run=subprocess.run([str(java_tool('java')),'-cp','build/dex-tools/*','tools/UpstreamPatchCheck.java',str(base),str(plan),str(output)],cwd=ROOT,text=True,capture_output=True)
        detail=(run.stdout if run.returncode==0 else run.stderr).strip()
        results[name]={'passed':run.returncode==0,'detail':detail}
        if run.returncode==0:
            try:
                result=linkage(name,output,specs,baseline_refs)
                results[name]['linkage']=result;results[name]['passed']=result['passed']
            except Exception as error:
                results[name]['passed']=False;results[name]['detail']=str(error)
    report={'upstreamCommit':json.loads((ROOT/'upstream-lock.json').read_text())['radiant']['commit'],
        'extensionDexSha256':hashlib.sha256((ROOT/'research/probe-remapped.dex').read_bytes()).hexdigest(),
        'engine':'Manager upstream fuzzy-context algorithm, assembly and introduced-reference linkage; Liquidify hooks applied',
        'patchSha256':{n:hashlib.sha256(paths[n].read_bytes()).hexdigest() for n in sorted({n for names in configs.values() for n in names})},
        'configurations':results}
    (ROOT/'reports/upstream-compatibility.json').write_text(json.dumps(report,indent=2)+'\n')
    failures={name:result for name,result in results.items() if not result['passed']}
    print(json.dumps({'passed':not failures,'configurations':len(results),'failures':failures},indent=2))
    if failures:raise SystemExit(1)


if __name__=='__main__':main()
