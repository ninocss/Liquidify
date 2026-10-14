"""Validate the exact prepared Liquidify payload before publishing it."""
from pathlib import Path
import hashlib,json,zipfile
import xml.etree.ElementTree as ET
from upstream_compatibility import restore_specs
ROOT=Path(__file__).resolve().parents[1]
def validate():
    release=json.loads((ROOT/'release.json').read_text())
    assert release['tag']=='v'+release['version']
    archive=ROOT/release['patches']
    with zipfile.ZipFile(archive) as z:
        assert z.testzip() is None
        names=z.namelist();assert len(names)==len(set(names))
        checks=json.loads(z.read('checksums.json'))
        assert set(checks)==set(names)-{'checksums.json'}
        for name,digest in checks.items():
            assert not name.startswith('/') and '..' not in name.split('/')
            assert not any(p in name.split('/') for p in ['research','.git','__pycache__'])
            assert not name.endswith(('.apk','.keystore','.jks','.apkm'))
            assert hashlib.sha256(z.read(name)).hexdigest()==digest,name
        manifest=json.loads(z.read('manifest.json'))
        assert manifest==json.loads((ROOT/'patches/manifest.json').read_text())
        own=[p for p in manifest['patches'] if p.get('source')=='liquidify']
        assert {p['id'] for p in own}=={'LiquidifyCore','LiquidifyTheme','LiquidifyTypography'}
        assert all(p['title'].startswith('Liquidify') and p['default'] for p in own)
        upstream=json.loads((ROOT/'radiant-base/patches/manifest.json').read_text())['patches']
        assert [p for p in manifest['patches'] if p.get('source')!='liquidify']==restore_specs(upstream)
        for original in (ROOT/'radiant-base/patches').rglob('*'):
            if original.is_file() and original.name!='manifest.json':
                name=original.relative_to(ROOT/'radiant-base/patches').as_posix()
                payload=z.read(name);reference=original.read_bytes()
                # Git normalizes text line endings on Linux checkout. Keep the
                # archived upstream bytes intact; compare only text EOLs canonically.
                if original.suffix in {'.patch','.smali','.json','.md','.disabled'}:
                    payload=payload.replace(b'\r\n',b'\n')
                    reference=reference.replace(b'\r\n',b'\n')
                assert payload==reference,name
        assert manifest['releaseVersion']==release['version']
        for patch in own:
            assert all(f in names for f in patch.get('fileNames',[]))
            assert all('extension/'+f in names for f in patch.get('extensionFiles',[]))
    report=json.loads((ROOT/'reports/package-alpha.json').read_text())
    assert hashlib.sha256(archive.read_bytes()).hexdigest()==report['sha256']
    gradle=(ROOT/'radiant-base/Manager/app/build.gradle.kts').read_text()
    assert f'versionName = "{release["version"]}"' in gradle
    assert f'versionCode = {release["managerVersionCode"]}' in gradle
    assert release['repository']=='ninocss/liquidify'
    assert '"PATCHES_REPO_NAME", "\\\"liquidify\\\""' in gradle
    resources=ROOT/'radiant-base/Manager/app/src/main'
    application=ET.parse(resources/'AndroidManifest.xml').getroot().find('application')
    assert application.get('{http://schemas.android.com/apk/res/android}label')=='@string/app_name'
    assert ET.parse(resources/'res/values/strings.xml').getroot().find("string[@name='app_name']").text=='Liquidify Manager'
    print(json.dumps({'passed':True,'version':release['version'],'patchesSha256':report['sha256'],'upstreamSpecsUnchanged':len(upstream),'liquidifyDefaultPatches':len(own)}))
if __name__=='__main__':validate()
