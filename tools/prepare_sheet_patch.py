"""Pinned sheet lifecycle, playlist sort metadata and native playlist search."""
from prepare_visual_patch import save


def sheets(text):
    start=text.index('.method public onStart()V')
    end=text.index('.end method',start)
    body=text[start:end]
    needle='    return-void'
    assert body.count(needle)==1
    body=body.replace(needle,'    invoke-static {p0}, Ldev/nino/squareglass/SheetChrome;->opened(Landroid/app/Dialog;)V\n\n'+needle)
    return text[:start]+body+text[end:]


def sort(text):
    needle='    return-object p1'
    assert text.count(needle)==1
    return text.replace(needle,'''    iget-object v0, p0, Lua/k1;->b:[Ljava/lang/String;
    iget v1, p0, Lua/k1;->c:I
    invoke-static {p0, p1, v0, v1}, Ldev/nino/squareglass/PlaylistSortMenu;->create(Landroidx/fragment/app/DialogFragment;Landroid/app/Dialog;[Ljava/lang/String;I)Landroid/app/Dialog;
    move-result-object p1
    return-object p1''')


def search(text):
    needle='    iput-object p1, p0, Lnj/q;->g:Landroidx/appcompat/widget/SearchView;'
    assert text.count(needle)==1
    return text.replace(needle,needle+'''
    invoke-static {p1}, Ldev/nino/squareglass/PlaylistSearch;->style(Landroidx/appcompat/widget/SearchView;)V
''')


def compose(text):
    needle='    invoke-static/range {v7 .. v19}, Landroidx/compose/material3/SurfaceKt;->Surface-T9BRK9s(Landroidx/compose/ui/Modifier;Landroidx/compose/ui/graphics/Shape;JJFFLandroidx/compose/foundation/BorderStroke;Lam0/p;Landroidx/compose/runtime/Composer;II)V'
    assert text.count(needle)==1
    return text.replace(needle,'''    move-object/from16 v0, v17
    const/4 v1, 0x0
    invoke-static {v7, v8, v0, v1}, Ldev/nino/squareglass/ComposeSheetGlass;->surface(Landroidx/compose/ui/Modifier;Landroidx/compose/ui/graphics/Shape;Landroidx/compose/runtime/Composer;I)Landroidx/compose/ui/Modifier;
    move-result-object v7
    const-wide/16 v9, 0x0    # Glass modifier owns the sheet's surface paint
'''+needle)


if __name__=='__main__':
    save('com/google/android/material/bottomsheet/BottomSheetDialog.smali','smali_classes22','175-square-sheet-lifecycle.patch',sheets)
    save('ua/k1.smali','smali_classes20','176-square-playlist-sort-menu.patch',sort)
    save('nj/q.smali','smali_classes21','177-square-playlist-search.patch',search)
    save('androidx/compose/material3/ModalBottomSheetKt.smali','smali_classes18','178-square-compose-sheet-glass.patch',compose)
