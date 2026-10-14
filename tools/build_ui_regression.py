"""Build local instrumentation against the separate test package; never signs or replaces TIDAL."""
from pathlib import Path
import os
import subprocess
import zipfile

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'research/ui-regression'
OUT.mkdir(parents=True,exist_ok=True)
JAVA=Path(r'C:\Program Files\Android\Android Studio\jbr')/'bin'
SDK=Path(os.environ.get('ANDROID_HOME',str(Path.home()/'AppData/Local/Android/Sdk')))
BT=SDK/'build-tools/37.0.0'
ANDROID=SDK/'platforms/android-36/android.jar'
def run(*args): subprocess.run([str(x) for x in args],cwd=ROOT,check=True)
(OUT/'AndroidManifest.xml').write_text('''<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="dev.nino.ui.check">
<uses-sdk android:minSdkVersion="33" android:targetSdkVersion="36"/>
<application android:label="Liquidify UI regression"/>
<instrumentation android:name="dev.nino.ui.check.AndroidUiRegression" android:targetPackage="dev.nino.tidalglass.test"/>
</manifest>''')
classes=OUT/'classes'; classes.mkdir(exist_ok=True)
run(JAVA/'javac.exe','-cp',ANDROID,'-d',classes,ROOT/'tools/AndroidUiRegression.java')
run(BT/'aapt2.exe','link','-I',ANDROID,'--manifest',OUT/'AndroidManifest.xml','-o',OUT/'unsigned.apk')
env=dict(os.environ,JAVA_HOME=str(JAVA.parent))
subprocess.run([str(BT/'d8.bat'),'--lib',str(ANDROID),'--output',str(OUT),
    *map(str,classes.rglob('*.class'))],cwd=ROOT,env=env,check=True)
with zipfile.ZipFile(OUT/'unsigned.apk','a') as apk: apk.write(OUT/'classes.dex','classes.dex')
run(BT/'zipalign.exe','-f','4',OUT/'unsigned.apk',OUT/'aligned.apk')
subprocess.run([str(BT/'apksigner.bat'),'sign','--ks',str(Path.home()/'.android/debug.keystore'),
    '--ks-key-alias','androiddebugkey','--ks-pass','pass:android','--key-pass','pass:android',
    '--out',str(OUT/'checks.apk'),str(OUT/'aligned.apk')],cwd=ROOT,env=env,check=True)
print(OUT/'checks.apk')
