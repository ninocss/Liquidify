"""Audited metadata overlay for the three restored upstream patch families."""
from copy import deepcopy
import re
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
RESTORED={'DebugMenuUnlock','LyricsRlApi','PlayerBackdrop'}


def helper_closure(files):
    base=ROOT/'radiant-base/patches/extension'
    found=set(files);todo=list(files)
    while todo:
        path=base/todo.pop()
        if not path.exists():raise ValueError('Missing upstream helper: '+str(path))
        for descriptor in re.findall(r'L((?:radiant|dev/kawarp)/[^;\s]+);',path.read_text()):
            helper=descriptor+'.smali'
            if helper not in found and (base/helper).exists():found.add(helper);todo.append(helper)
    return sorted(found)


def restore_specs(original):
    specs=deepcopy(original)
    for spec in specs:
        if spec['id'] not in RESTORED:continue
        spec['source']='radiantLyrics'
        spec['compatibleWith']=['LiquidifyCore','LiquidifyTheme']
        spec['default']=False
        if spec['id']=='PlayerBackdrop':
            spec['requires']=list(dict.fromkeys(spec.get('requires',[])+['LiquidifyTheme']))
            spec['extensionFiles']=helper_closure(['radiant/HomeBackdrop.smali'])
            spec['variants'][0]['fileNames'].append('player-native-backdrop-liquidify.patch')
            option=next(o for o in spec['advancedOptions'] if o['key']=='cover_everywhere')
            option['fileNames']=[f.replace('collection-backdrop.patch','collection-backdrop-liquidify.patch') for f in option['fileNames']]
        if spec.get('extensionFiles'):spec['extensionFiles']=helper_closure(spec['extensionFiles'])
        for variant in spec.get('variants',[]):
            if variant.get('extensionFiles'):variant['extensionFiles']=helper_closure(variant['extensionFiles'])
    return specs
