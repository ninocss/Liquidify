"""Pinned native mini gestures, listening observer and configurable home actions."""
from prepare_visual_patch import save
import re


def mini_gestures(text):
    needle = """    invoke-static/range {v40 .. v48}, Landroidx/compose/foundation/ClickableKt;->clickable-O2vRcR0$default(Landroidx/compose/ui/Modifier;Landroidx/compose/foundation/interaction/MutableInteractionSource;Landroidx/compose/foundation/Indication;ZLjava/lang/String;Landroidx/compose/ui/semantics/Role;Lam0/a;ILjava/lang/Object;)Landroidx/compose/ui/Modifier;

    move-result-object v4"""
    assert text.count(needle) == 1
    return text.replace(needle, needle + """

    move-object/from16 v5, v46

    const/4 v6, 0x0

    invoke-static {v4, v5, v7, v6}, Ldev/nino/squareglass/NativeMiniGestures;->decorate(Landroidx/compose/ui/Modifier;Lam0/a;Landroidx/compose/runtime/Composer;I)Landroidx/compose/ui/Modifier;

    move-result-object v4""")


def observer(text):
    start = text.index('.method public final c()Lcom/tidal/android/feature/appscaffold/ui/t;')
    end = text.index('.end method', start)
    body = text[start:end]
    needle = '    iget-object v1, v0, Lcom/tidal/android/feature/appscaffold/ui/MiniPlayerViewModel;->a:Lbi/e;'
    assert body.count(needle) == 1
    body = body.replace(needle, needle + """

    invoke-static {v1}, Ldev/nino/squareglass/NativePlayback;->bind(Lbi/e;)V""")
    needle = '    return-object v7'
    assert body.count(needle) == 1
    body = body.replace(needle, """    invoke-virtual {v2}, Ljava/lang/Enum;->name()Ljava/lang/String;

    move-result-object v1

    invoke-static {v1}, Ldev/nino/squareglass/NativePlayback;->observeName(Ljava/lang/String;)V

""" + needle)
    return text[:start] + body + text[end:]


def home_actions(text):
    start = text.index('.method public static final b(Lcom/tidal/android/feature/home/ui/u;Lam0/l;Landroidx/compose/runtime/Composer;I)V')
    end = text.index('.end method', start)
    body = text[start:end]
    needle = '    move-object/from16 v0, p0'
    assert body.count(needle) == 1
    body = body.replace(needle, """    const/4 v0, 0x0

    invoke-static {p2, v0}, Ldev/nino/squareglass/SquareHome;->actionsEnabled(Landroidx/compose/runtime/Composer;I)Z

    move-result v0

    if-eqz v0, :square_native_home

    const/4 v0, 0x0

    invoke-static {p2, v0}, Ldev/nino/squareglass/SquareHome;->Actions(Landroidx/compose/runtime/Composer;I)V

    return-void

    :square_native_home
""" + needle)
    # Parameters are above v15 in this method: use range moves for invokes.
    body = body.replace('    invoke-static {p2, v0},', '    move-object/from16 v1, p2\n\n    invoke-static {v1, v0},')
    return text[:start] + body + text[end:]


def logout(text):
    needle = '.method public final call()Ljava/lang/Object;'
    assert text.count(needle) == 1
    start = text.index(needle)
    end = text.index('.end method', start)
    body = text[start:end]
    locals_line = re.search(r'    \.locals \d+\n', body).group()
    body = body.replace(locals_line, locals_line + '\n    invoke-static {}, Ldev/nino/squareglass/NativePlayback;->logout()V\n', 1)
    return text[:start] + body + text[end:]


if __name__ == '__main__':
    save('com/tidal/android/feature/appscaffold/ui/composable/i.smali', 'smali_classes5', '132-square-mini-gestures.patch', mini_gestures, hooked=True)
    save('com/tidal/android/feature/appscaffold/ui/MiniPlayerViewModel.smali', 'smali_classes5', '133-square-listening-observer.patch', observer)
    save('f80/f.smali', 'smali_classes8', '134-square-home-actions.patch', home_actions, hooked=True)
    from pathlib import Path
    Path('experimental-patches/135-square-linn-native-transport.patch').unlink(missing_ok=True)
    save('com/aspiro/wamp/logout/service/a.smali', 'smali_classes20', '136-square-clear-listening-on-logout.patch', logout)
    print('Prepared mini gestures, local taste and home actions hooks')
