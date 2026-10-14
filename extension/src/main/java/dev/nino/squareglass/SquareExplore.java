/* Square Glass for TIDAL. GPL-3.0-or-later. */
package dev.nino.squareglass;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

/** Scoped to the legacy Explore page, whose white labels assumed a black page. */
public final class SquareExplore {
    private SquareExplore() {}
    private static final java.util.WeakHashMap<View,java.lang.ref.WeakReference<Binding>> bindings=new java.util.WeakHashMap<>();
    public static void install(View root) {
        if (SquareSettings.themeEnabled() == 0) return;
        java.lang.ref.WeakReference<Binding> ref=bindings.get(root);
        if(ref!=null && ref.get()!=null) { ref.get().paint(root); return; }
        Binding binding = new Binding(root); bindings.put(root,new java.lang.ref.WeakReference<>(binding));
        root.addOnAttachStateChangeListener(binding);
        binding.paint(root);

    }

    public static void search(View root) {
        if (SquareSettings.themeEnabled() == 0) return;
        float density = root.getResources().getDisplayMetrics().density;
        clearSearch(root);
        PageGlass.install(root,28);
        if(PageGlass.hasSurface(root))root.setBackgroundColor(Color.TRANSPARENT);
        else root.setBackground(PageMaterial.background(root,SquarePalette.base(SquarePalette.isDark()),28));
        root.setClipToOutline(true);
        root.setMinimumHeight(Math.round(48 * density));
    }

    private static void clearSearch(View view) {
        if (view instanceof TextView) {
            TextView text = (TextView)view;
            text.setTextColor(SquarePalette.ink(SquarePalette.isDark()));
            text.setHintTextColor(SquarePalette.isDark() ? 0xffb4b8c4 : 0xff5d626d);
            text.setTypeface(view.getResources().getFont(0x7f090005));
            if(text instanceof android.widget.EditText) {
                text.setHighlightColor((SquarePalette.ink(SquarePalette.isDark())&0xffffff)|0x33000000);
                android.graphics.drawable.Drawable cursor=text.getTextCursorDrawable();
                if(cursor!=null) {cursor.mutate().setTint(SquarePalette.ink(SquarePalette.isDark()));text.setTextCursorDrawable(cursor);}
            }
        } else if (view instanceof ImageView) {
            ((ImageView)view).setImageTintList(ColorStateList.valueOf(SquarePalette.ink(SquarePalette.isDark())));
        } else if (view instanceof ViewGroup) {
            view.setBackgroundColor(Color.TRANSPARENT);
            ViewGroup group = (ViewGroup)view;
            for (int i = 0; i < group.getChildCount(); i++) clearSearch(group.getChildAt(i));
        }
    }

    private static final class Binding implements View.OnAttachStateChangeListener, RecyclerView.OnChildAttachStateChangeListener {
        final View root;
        final ArrayList<RecyclerView> lists = new ArrayList<>();
        Binding(View root) { this.root = root; }
        void paint(View view) {
            if (view instanceof TextView) {
                TextView text = (TextView)view;
                int color = text.getCurrentTextColor();
                int high = Math.max(Color.red(color), Math.max(Color.green(color), Color.blue(color)));
                int low = Math.min(Color.red(color), Math.min(Color.green(color), Color.blue(color)));
                if (high - low < 25 && (high >= 220 || high <= 50))
                    text.setTextColor(SquarePalette.ink(SquarePalette.isDark()));
            }
            if (view instanceof RecyclerView && !lists.contains(view)) {
                RecyclerView list = (RecyclerView)view;
                lists.add(list);
                list.addOnChildAttachStateChangeListener(this);
            }
            if (view instanceof ViewGroup) {
                ViewGroup group = (ViewGroup)view;
                for (int i = 0; i < group.getChildCount(); i++) paint(group.getChildAt(i));
            }
        }
        @Override public void onChildViewAttachedToWindow(View child) { child.post(() -> { if (child.isAttachedToWindow()) paint(child); }); }
        @Override public void onChildViewDetachedFromWindow(View child) {}
        @Override public void onViewAttachedToWindow(View view) { paint(root); }
        @Override public void onViewDetachedFromWindow(View view) {
            for (RecyclerView list : lists) list.removeOnChildAttachStateChangeListener(this);
            lists.clear();

        }
    }
}
