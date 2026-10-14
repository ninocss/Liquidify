/* Local instrumentation for the side-by-side integration APK only. */
package dev.nino.ui.check;

import android.app.Activity;
import android.app.Dialog;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.Bundle;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import android.view.View;
import android.view.ViewGroup;

public final class AndroidUiRegression extends Instrumentation {
    private Dialog dialog;
    private Activity activity;
    private ClassLoader host;
    private Throwable failure;
    private final StringBuilder results = new StringBuilder();
    @Override public void onCreate(Bundle args) { super.onCreate(args); start(); }
    private Class<?> type(String name) throws Exception { return Class.forName(name, true, host); }
    private void main(Runnable work) {
        runOnMainSync(() -> { try { work.run(); } catch (Throwable error) { failure=error; } });
        if (failure!=null) throw new RuntimeException(failure);
    }
    @Override public void onStart() {
        Bundle output=new Bundle();
        try {
            host=getTargetContext().getClassLoader();
            for(String name:new String[]{
                "com.tidal.android.feature.playerscreen.ui.PlayerScreenKt",
                "com.tidal.android.feature.playerscreen.ui.composables.p3",
                "com.tidal.android.feature.playerscreen.ui.composables.CoverPagerKt",
                "androidx.compose.material3.ModalBottomSheetKt", "ua.k1",
                "dev.nino.squareglass.PlayerArtwork", "dev.nino.squareglass.PlaylistSortMenu"}) {
                type(name).getDeclaredMethods();
                results.append("Verified ").append(name).append('\n');
            }
            activity=startActivitySync(new Intent().setClassName(getTargetContext().getPackageName(),
                "com.aspiro.wamp.MainActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            waitForIdleSync();
            for(int repeat=0;repeat<3;repeat++) {
                main(() -> {
                    try {
                        Constructor<?> ctor=type("dev.nino.squareglass.PlaylistSortMenu").getDeclaredConstructor(
                            Activity.class,type("androidx.fragment.app.DialogFragment"),String[].class,int.class);
                        ctor.setAccessible(true);
                        dialog=(Dialog)ctor.newInstance(activity,type("jj.a").getConstructor().newInstance(),
                            new String[]{"Custom order","Date added","Title","Artist"},1);
                        dialog.show();
                    } catch(Exception error) { throw new RuntimeException(error); }
                });
                waitForIdleSync();
                main(() -> {
                    if(!dialog.isShowing() || dialog.getWindow().getDecorView().getWidth()<=0)
                        throw new AssertionError("Sort menu not laid out");
                    dialog.dismiss();
                });
                waitForIdleSync();
            }
            results.append("Sort menu: three real window open/layout/dismiss cycles passed\n");
            main(() -> {
                try {
                    Object modifier=type("androidx.compose.ui.Modifier").getField("Companion").get(null);
                    Object rectangle=type("androidx.compose.ui.graphics.RectangleShapeKt")
                        .getMethod("getRectangleShape").invoke(null);
                    Class<?> composer=type("androidx.compose.runtime.Composer");
                    Class<?> modifierType=type("androidx.compose.ui.Modifier");
                    Object filled=type("androidx.compose.foundation.layout.SizeKt").getMethod("fillMaxSize$default",
                        modifierType,float.class,int.class,Object.class).invoke(null,modifier,0f,1,null);
                    Object content=Proxy.newProxyInstance(host,new Class<?>[]{type("am0.p")},(proxy,method,args) -> {
                        if(method.getName().equals("invoke")) {
                            Object glass=type("dev.nino.squareglass.ComposeSheetGlass").getMethod("surface",
                                modifierType,type("androidx.compose.ui.graphics.Shape"),composer,int.class)
                                .invoke(null,filled,rectangle,args[0],0);
                            type("androidx.compose.foundation.layout.BoxKt").getMethod("Box",modifierType,composer,int.class)
                                .invoke(null,glass,args[0],0);
                            return type("kotlin.u").getField("a").get(null);
                        }
                        if(method.getName().equals("hashCode"))return System.identityHashCode(proxy);
                        if(method.getName().equals("equals"))return proxy==args[0];
                        return "Rectangle sheet regression content";
                    });
                    View pane=(View)type("androidx.compose.ui.platform.ComposeView").getConstructor(android.content.Context.class)
                        .newInstance(activity);
                    pane.getClass().getMethod("setContent",type("am0.p")).invoke(pane,content);
                    activity.addContentView(pane,new ViewGroup.LayoutParams(-1,-1));
                } catch(Exception error) { throw new RuntimeException(error); }
            });
            waitForIdleSync();
            results.append("Fullscreen RectangleShape sheet attached and drawn\n");
            output.putString("stream",results.toString()); finish(Activity.RESULT_OK,output);
        } catch(Throwable error) {
            while(error instanceof InvocationTargetException && error.getCause()!=null)error=error.getCause();
            output.putString("stream",results+android.util.Log.getStackTraceString(error));
            finish(Activity.RESULT_CANCELED,output);
        }
    }
}
