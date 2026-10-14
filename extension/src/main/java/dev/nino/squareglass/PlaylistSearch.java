/* GPL-3.0-or-later. Decoration only: TIDAL retains query, clear, focus and navigation. */
package dev.nino.squareglass;

import android.view.View;
import android.widget.TextView;
import androidx.appcompat.widget.SearchView;

public final class PlaylistSearch {
    private PlaylistSearch() {}
    public static void style(SearchView search) {
        if(SquareSettings.themeEnabled()==0) return;
        search.setQueryHint("Search this playlist");
        search.setMaxWidth(Integer.MAX_VALUE);
        ((View)search).addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            public void onViewAttachedToWindow(View view) { paint(search); }
            public void onViewDetachedFromWindow(View view) {}
        });
        if(((View)search).isAttachedToWindow()) paint(search);
    }
    private static void paint(SearchView search) {
        SquareExplore.search(search);
        View view=search;
        int padding=Math.round(12*view.getResources().getDisplayMetrics().density);
        view.setPaddingRelative(padding,0,padding,0);
        int id=view.getResources().getIdentifier("search_src_text","id",view.getContext().getPackageName());
        TextView input=view.findViewById(id);
        if(input!=null) { input.setTextSize(16); input.setSingleLine(true); }
    }
}
