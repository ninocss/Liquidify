"""Pinned Alpha18 search decoration and icon motion; no native action replacement."""
from prepare_visual_patch import save

def search_container(text):
    marker='    .line 189\n    invoke-virtual/range {v1 .. v23}, Landroidx/compose/material3/TextFieldDefaults;->DecorationBox('
    assert text.count(marker)==1
    return text.replace(marker,'''    invoke-static {}, Ldev/nino/squareglass/SearchChrome;->container()Lam0/p;
    move-result-object v19
    invoke-static {}, Ldev/nino/squareglass/SearchChrome;->shape()Landroidx/compose/ui/graphics/Shape;
    move-result-object v16
    const v23, 0x38c0

'''+marker)

def search_cursor(text):
    marker='    .line 45\n    invoke-static/range {v8 .. v27}, Landroidx/compose/foundation/text/BasicTextFieldKt;->BasicTextField('
    assert text.count(marker)==1
    return text.replace(marker,'''    invoke-static {}, Ldev/nino/squareglass/SearchChrome;->cursor()Landroidx/compose/ui/graphics/Brush;
    move-result-object v22

'''+marker)

def favorite(text):
    marker='    invoke-static {v15}, Lcom/tidal/android/feature/playerscreen/ui/composables/anim/BouncePressKt;->a(Landroidx/compose/ui/Modifier;)Landroidx/compose/ui/Modifier;\n'
    assert text.count(marker)==1
    return text.replace(marker,'''    iget-boolean v4, v0, Lcom/tidal/android/feature/playerscreen/ui/e;->a:Z
    const/4 v6, 0x0
    invoke-static {v15, v4, v11, v6}, Ldev/nino/squareglass/IconMotion;->favorite(Landroidx/compose/ui/Modifier;ZLandroidx/compose/runtime/Composer;I)Landroidx/compose/ui/Modifier;
''')

def settings(text):
    marker='    .line 205\n    move-object v6, v0'
    assert text.count(marker)==1
    return text.replace(marker,'''    const/4 v14, 0x0
    invoke-static {v8, v13, v14}, Ldev/nino/squareglass/IconMotion;->settings(Landroidx/compose/ui/Modifier;Landroidx/compose/runtime/Composer;I)Landroidx/compose/ui/Modifier;
    move-result-object v8

'''+marker)

def collection_settings(text):
    marker='    .line 273\n    invoke-static/range {v7 .. v14}, Landroidx/compose/material3/IconKt;->Icon-ww6aTOc('
    assert text.count(marker)==1
    return text.replace(marker,'''    invoke-static {v9, v1, v12, v13}, Ldev/nino/squareglass/IconMotion;->settingsResource(Landroidx/compose/ui/Modifier;ILandroidx/compose/runtime/Composer;I)Landroidx/compose/ui/Modifier;
    move-result-object v9

'''+marker)

def legacy_favorite(text):
    assert '.method protected drawChild(' not in text
    return text+'''
.method protected onAttachedToWindow()V
    .locals 0
    invoke-super {p0}, Landroidx/constraintlayout/widget/ConstraintLayout;->onAttachedToWindow()V
    invoke-static {p0}, Ldev/nino/squareglass/NativeIconMotion;->attach(Landroid/view/View;)V
    return-void
.end method

.method protected onDetachedFromWindow()V
    .locals 0
    invoke-static {p0}, Ldev/nino/squareglass/NativeIconMotion;->detach(Landroid/view/View;)V
    invoke-super {p0}, Landroidx/constraintlayout/widget/ConstraintLayout;->onDetachedFromWindow()V
    return-void
.end method

.method public dispatchTouchEvent(Landroid/view/MotionEvent;)Z
    .locals 1
    invoke-static {p0, p1}, Ldev/nino/squareglass/NativeIconMotion;->touch(Landroid/view/View;Landroid/view/MotionEvent;)V
    invoke-super {p0, p1}, Landroidx/constraintlayout/widget/ConstraintLayout;->dispatchTouchEvent(Landroid/view/MotionEvent;)Z
    move-result v0
    return v0
.end method

.method protected drawChild(Landroid/graphics/Canvas;Landroid/view/View;J)Z
    .locals 2
    invoke-static {p0, p2, p1}, Ldev/nino/squareglass/NativeIconMotion;->before(Landroid/view/View;Landroid/view/View;Landroid/graphics/Canvas;)I
    move-result v0
    invoke-super/range {p0 .. p4}, Landroidx/constraintlayout/widget/ConstraintLayout;->drawChild(Landroid/graphics/Canvas;Landroid/view/View;J)Z
    move-result v1
    invoke-static {p0, p2, p1, v0}, Ldev/nino/squareglass/NativeIconMotion;->after(Landroid/view/View;Landroid/view/View;Landroid/graphics/Canvas;I)V
    return v1
.end method
'''

def slider_thumb(text):
    marker='    invoke-static/range {v0 .. v7}, Landroidx/compose/animation/core/AnimateAsStateKt;->animateFloatAsState(FLandroidx/compose/animation/core/AnimationSpec;FLjava/lang/String;Lam0/l;Landroidx/compose/runtime/Composer;II)Landroidx/compose/runtime/State;'
    assert text.count(marker)==1
    return text.replace(marker,'''    const/4 v6, 0x0
    invoke-static {v0, v5, v6}, Ldev/nino/squareglass/SliderMotion;->nativeScale(FLandroidx/compose/runtime/Composer;I)Landroidx/compose/runtime/State;''')

if __name__=='__main__':
    save('com/tidal/android/feature/search/ui/composables/a0.smali','smali_classes12','150-square-search-input-container.patch',search_container)
    save('com/tidal/android/feature/search/ui/composables/c0.smali','smali_classes12','151-square-search-input-cursor.patch',search_cursor)
    save('com/tidal/android/feature/playerscreen/ui/composables/e1.smali','smali_classes10','152-square-favorite-motion.patch',favorite)
    save('com/tidal/android/feature/home/ui/composables/g.smali','smali_classes13','153-square-settings-icon-motion.patch',settings,hooked=True)
    save('com/aspiro/wamp/mycollectionscreen/presentation/MyCollectionScreenKt.smali','smali_classes20','154-square-collection-settings-motion.patch',collection_settings,hooked=True)
    save('com/aspiro/wamp/widgets/SecondaryActionButton.smali','smali_classes21','155-square-native-favorite-motion.patch',legacy_favorite)
    save('com/tidal/android/feature/playerscreen/ui/composables/z4.smali','smali_classes10','156-square-slider-thumb-motion.patch',slider_thumb,hooked=True)
    print('Prepared native search input, confirmed favorite state and decorative settings icon motion')
