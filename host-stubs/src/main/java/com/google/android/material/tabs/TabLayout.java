/* Compile-only ABI; the actual class is supplied by the pinned TIDAL host. */
package com.google.android.material.tabs;
public class TabLayout extends android.widget.HorizontalScrollView {
    public TabLayout(android.content.Context context) { super(context); }
    public void setSelectedTabIndicatorColor(int color) { throw new AssertionError("Host ABI only"); }
    public void setTabTextColors(int normal,int selected) { throw new AssertionError("Host ABI only"); }
}
