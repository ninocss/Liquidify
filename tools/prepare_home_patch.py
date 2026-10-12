"""Version-pinned Home filter and shortcut presentation adapters."""
from prepare_visual_patch import save

HOME = "com/tidal/android/feature/home/ui/composables/"
SHORTCUT = HOME + "shortcutlist/"


def filters(text):
    signature = ".method public static final a(Lcom/tidal/android/feature/home/ui/u$a;Lam0/l;Landroidx/compose/runtime/Composer;I)V"
    assert text.count(signature) == 1
    start = text.index(signature)
    end = text.index(".end method", start) + len(".end method")
    body = text[start:end]
    # Verify the native callback variant before keeping it in the adapter.
    assert "Lcom/tidal/android/feature/search/ui/composables/m0;-><init>(Lam0/l;Ljava/lang/Object;I)V" in body
    assert "Lcom/tidal/android/feature/home/ui/u$a;->c:Z" in body
    return text[:start] + signature + """
    .locals 5
    .annotation build Landroidx/compose/runtime/Composable;
    .end annotation

    iget-object v0, p0, Lcom/tidal/android/feature/home/ui/u$a;->b:Ljava/lang/String;

    iget-boolean v1, p0, Lcom/tidal/android/feature/home/ui/u$a;->c:Z

    new-instance v2, Lcom/tidal/android/feature/search/ui/composables/m0;

    const/4 v4, 0x1

    invoke-direct {v2, p1, p0, v4}, Lcom/tidal/android/feature/search/ui/composables/m0;-><init>(Lam0/l;Ljava/lang/Object;I)V

    move-object v3, p2

    const/4 v4, 0x0

    invoke-static/range {v0 .. v4}, Ldev/nino/squareglass/SquareHome;->Filter(Ljava/lang/String;ZLam0/a;Landroidx/compose/runtime/Composer;I)V

    return-void
.end method""" + text[end:]


def shortcuts(text):
    # More breathing room around artwork and two text lines, with the original
    # combined-click, long-press, haptic and task handlers left intact.
    assert text.count("const/16 v0, 0x3c") == 1
    text = text.replace("const/16 v0, 0x3c", "const/16 v0, 0x48", 1)
    old = "invoke-static {v14}, Landroidx/compose/foundation/shape/RoundedCornerShapeKt;->RoundedCornerShape-0680j_4(F)Landroidx/compose/foundation/shape/RoundedCornerShape;"
    assert text.count(old) == 1
    text = text.replace(old, "invoke-static {v14}, Ldev/nino/squareglass/SquareShapes;->shortcut(F)Landroidx/compose/foundation/shape/RoundedCornerShape;")
    old = "invoke-static/range {v29 .. v34}, Landroidx/compose/foundation/BackgroundKt;->background-bw27NRU$default(Landroidx/compose/ui/Modifier;JLandroidx/compose/ui/graphics/Shape;ILjava/lang/Object;)Landroidx/compose/ui/Modifier;"
    assert text.count(old) == 1
    text = text.replace(old, "invoke-static/range {v29 .. v29}, Ldev/nino/squareglass/SquareHome;->shortcutSurface(Landroidx/compose/ui/Modifier;)Landroidx/compose/ui/Modifier;")
    # Both artwork branches get the same 10dp corner, separate from card shape.
    for reg in ("v3", "v6"):
        old = f"invoke-static {{{reg}}}, Landroidx/compose/foundation/shape/RoundedCornerShapeKt;->RoundedCornerShape-0680j_4(F)Landroidx/compose/foundation/shape/RoundedCornerShape;"
        assert text.count(old) == 1
        text = text.replace(old, f"invoke-static {{{reg}}}, Ldev/nino/squareglass/SquareShapes;->artwork(F)Landroidx/compose/foundation/shape/RoundedCornerShape;")
    return text


def shortcut_icon(text):
    # Only the symbolic icon branch, never the album/playlist artwork branches.
    needle = "    invoke-static/range {v2 .. v11}, Landroidx/compose/foundation/ImageKt;->Image("
    assert text.count(needle) == 1
    return text.replace(needle, """    const/16 v11, 0x38

    invoke-static {}, Ldev/nino/squareglass/NativeGlass;->iconTint()Landroidx/compose/ui/graphics/ColorFilter;

    move-result-object v8

""" + needle)


def toolbar(text):
    needle = "invoke-static {v5, v0}, Landroidx/compose/foundation/layout/PaddingKt;->padding-3ABfNKs(Landroidx/compose/ui/Modifier;F)Landroidx/compose/ui/Modifier;"
    assert text.count(needle) == 1
    text = text.replace(needle, "invoke-static {v5}, Ldev/nino/squareglass/SquareHome;->toolbarButton(Landroidx/compose/ui/Modifier;)Landroidx/compose/ui/Modifier;")
    needle = "    invoke-static/range {v6 .. v15}, Landroidx/compose/foundation/ImageKt;->Image("
    assert text.count(needle) == 1
    return text.replace(needle, """    const/16 v15, 0x38

    invoke-static {}, Ldev/nino/squareglass/NativeGlass;->iconTint()Landroidx/compose/ui/graphics/ColorFilter;

    move-result-object v12

""" + needle)


def library(text):
    signature = ".method public static final g(Lkotlinx/coroutines/CoroutineScope;Lam0/l;ZZLandroidx/compose/runtime/Composer;I)V"
    start = text.index(signature)
    end = text.index(".end method", start)
    body = text[start:end]
    begin = body.index("    .line 55\n    invoke-interface {v0}, Ljava/util/List;->iterator()Ljava/util/Iterator;")
    finish = body.index("    .line 66", begin)
    # Retain native model construction, conditional purchases, Downloads action,
    # coroutine lifetime and parent recomposition. Only replace category layout.
    body = body.replace("    .locals 23", "    .locals 24")
    begin = body.index("    .line 55\n    invoke-interface {v0}, Ljava/util/List;->iterator()Ljava/util/Iterator;")
    finish = body.index("    .line 66", begin)
    body = body[:begin] + """    move-object/from16 v23, v0

    move/from16 v17, v6

    const/4 v14, 0x0

    const/4 v9, 0x0

""" + body[finish:]
    # Defer rendering until the native parent has remembered its Downloads
    # callback. Keep the list in v23 while its original action is constructed.
    needle = "    invoke-static {v6, v12, v14}, Lcom/aspiro/wamp/mycollectionscreen/presentation/MyCollectionScreenKt;->c(Lam0/a;Landroidx/compose/runtime/Composer;I)V"
    assert body.count(needle) == 1
    body = body.replace(needle, """    move-object/from16 v17, v23
    move-object/from16 v18, v1
    move-object/from16 v19, v2
    move/from16 v20, v4
    move-object/from16 v21, v6
    move-object/from16 v22, v12
    const/16 v23, 0x0
    invoke-static/range {v17 .. v23}, Ldev/nino/squareglass/SquareLibrary;->Grid(Ljava/util/List;Lkotlinx/coroutines/CoroutineScope;Lam0/l;ZLam0/a;Landroidx/compose/runtime/Composer;I)V""")
    text = text[:start] + body + text[end:]
    signature = ".method public static final f(IIIZLjava/lang/String;Lam0/a;Landroidx/compose/runtime/Composer;I)V"
    start = text.index(signature)
    end = text.index(".end method", start) + len(".end method")
    return text[:start] + signature + """
    .locals 0
    .annotation build Landroidx/compose/runtime/Composable;
    .end annotation

    invoke-static/range {p0 .. p7}, Ldev/nino/squareglass/SquareLibrary;->Entry(IIIZLjava/lang/String;Lam0/a;Landroidx/compose/runtime/Composer;I)V

    return-void
.end method""" + text[end:]


def shortcut_shadow(text):
    old = "const/16 v1, 0x20"
    assert text.count(old) == 1
    return text.replace(old, "const/16 v1, 0x6")


if __name__ == "__main__":
    save("f80/f.smali", "smali_classes8", "084-square-home-filters.patch", filters)
    save(SHORTCUT + "ShortcutListModuleKt.smali", "smali_classes8", "085-square-home-shortcuts.patch", shortcuts)
    save(SHORTCUT + "p.smali", "smali_classes8", "086-square-home-shortcut-icon.patch", shortcut_icon)
    save(HOME + "g.smali", "smali_classes13", "087-square-home-toolbar-surface.patch", toolbar, hooked=True)
    save("com/aspiro/wamp/mycollectionscreen/presentation/MyCollectionScreenKt.smali", "smali_classes20", "088-square-library-grid.patch", library)
    save(SHORTCUT + "m.smali", "smali_classes8", "089-square-home-artwork-shadow.patch", shortcut_shadow)
    print("Prepared Square Home and native library grid adapters")
