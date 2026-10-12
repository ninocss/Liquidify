package dev.nino.squareglass;
/** Compile-only ABI for the exact TIDAL 2.200.0 output manager. */
public final class NativeOutputBridge {
    public static Object open() { return null; }
    public static void close(Object listener) {}
    public static void scan() {}
    public static Object request(Object callback) { return null; }
    public static void cancel(Object request) {}
    public static Object active() { return null; }
    public static String id(Object device) { return null; }
    public static String name(Object device) { return null; }
    public static int type(Object device) { return 0; }
    public static boolean local(Object device) { return false; }
    public static void discard(Object dialog) {}
    public static void select(Object device) {}
}
