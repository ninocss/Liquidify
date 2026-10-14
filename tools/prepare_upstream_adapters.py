"""Separate compatibility diffs; original Radiant patches remain byte-for-byte intact."""
import difflib
from prepare_visual_patch import ROOT


def write(relative, before, after, name):
    target=ROOT/'compatibility-patches'/name;target.parent.mkdir(exist_ok=True)
    target.write_text(''.join(difflib.unified_diff(before.splitlines(True),after.splitlines(True),
        fromfile='a/'+relative,tofile='b/'+relative)),newline='\n')


def main():
    relative='com/tidal/wave2/foundation/WaveScaffoldKt.smali'
    before=(ROOT/'research/probe-hook-smali'/relative).read_text()
    marker='    new-instance v0, Lcom/tidal/wave2/foundation/WaveScaffoldKt$WaveScaffold$2;'
    assert before.count(marker)==1
    after=before.replace(marker,'''    const/4 v0, 0x0
    invoke-static {v1, v0}, Lradiant/HomeBackdrop;->render(Landroidx/compose/runtime/Composer;I)V

'''+marker)
    marker='    move-wide v15, v10'
    assert after.count(marker)==1
    after=after.replace(marker,'    const-wide/16 v15, 0x0    # transparent containerColor')
    write(relative,before,after,'collection-backdrop-liquidify.patch')
    relative='com/tidal/android/feature/playerscreen/ui/composables/p3.smali'
    before=(ROOT/'research/probe-hook-smali'/relative).read_text()
    original=(ROOT/'research/tidal/smali_classes10'/relative).read_text()
    signature='.method public static final b('
    a=before.index(signature);b=before.index('.end method',a)+len('.end method')
    x=original.index(signature);y=original.index('.end method',x)+len('.end method')
    write(relative,before,before[:a]+original[x:y]+before[b:],'player-native-backdrop-liquidify.patch')


if __name__=='__main__':main()
