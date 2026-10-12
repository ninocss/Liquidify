package com.aspiro.wamp.playqueue.source.model;
public final class ItemSource extends Source {
    public ItemSource(String id, String title, NavigationType navigation, com.tidal.android.navigation.NavigationInfo.Node node) {}
    public interface NavigationType {
        final class None implements NavigationType { public static final None INSTANCE = new None(); }
    }
}
