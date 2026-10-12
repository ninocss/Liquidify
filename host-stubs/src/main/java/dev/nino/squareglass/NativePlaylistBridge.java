package dev.nino.squareglass;
import android.view.View;
import java.util.List;
/** Compile-only declarations for the pinned DEX bridge. */
public final class NativePlaylistBridge {
    public static View view(Object presenter) { return null; }
    public static List<?> items(Object presenter) { return null; }
    public static void model(Object presenter,Object model) {}
    public static Object callback(Object presenter,List<?> old) { return null; }
    public static void dispatch(Object presenter,Object diff) {}
    public static String revision(Object playlist) {return null;}
    public static String contentRevision(Object presenter) {return null;}
    public static void metadata(Object presenter,Object playlist) {}
}
