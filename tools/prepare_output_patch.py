"""Pinned output dialog override and native icon geometry. No OpenHome player hook."""
from prepare_visual_patch import save, PLAYER


def dialog(text):
    for signature in ('show()V', 'dismiss()V', 'isShowing()Z'):
        assert '.method public ' + signature not in text
    return text + '''
.method public show()V
    .locals 1
    invoke-static {p0}, Ldev/nino/squareglass/SquareOutput;->show(Landroid/app/Dialog;)Z
    move-result v0
    if-nez v0, :done
    invoke-super {p0}, Lcom/google/android/material/bottomsheet/BottomSheetDialog;->show()V
    :done
    return-void
.end method

.method public dismiss()V
    .locals 0
    invoke-static {p0}, Ldev/nino/squareglass/SquareOutput;->dismissed(Landroid/app/Dialog;)V
    invoke-super {p0}, Landroidx/appcompat/app/AppCompatDialog;->dismiss()V
    return-void
.end method

.method public isShowing()Z
    .locals 1
    invoke-static {p0}, Ldev/nino/squareglass/SquareOutput;->showing(Landroid/app/Dialog;)Z
    move-result v0
    if-nez v0, :done
    invoke-super {p0}, Landroid/app/Dialog;->isShowing()Z
    move-result v0
    :done
    return v0
.end method
'''


def anchor(text):
    # The local-output icon is static and never executes AnimatedIcon. Capture the
    # outer native clickable button so every provider state and TalkBack has the
    # same authoritative anchor, including the full 48 dp touch target.
    needle='    invoke-static/range {v1 .. v9}, Landroidx/compose/foundation/ClickableKt;->clickable-'
    assert text.count(needle)==1
    start=text.index(needle)
    result=text.index('    move-result-object v1',start)+len('    move-result-object v1')
    return text[:result]+'''

    invoke-static {v1, v11, v12}, Ldev/nino/squareglass/OutputAnchor;->watch(Landroidx/compose/ui/Modifier;Landroidx/compose/runtime/Composer;I)Landroidx/compose/ui/Modifier;

    move-result-object v1
'''+text[result:]


if __name__ == '__main__':
    save('com/aspiro/wamp/broadcast/f.smali','smali_classes18','157-square-output-popup.patch',dialog)
    save(PLAYER+'BroadcastButtonKt.smali','smali_classes10','158-square-output-anchor.patch',anchor,hooked=True)
