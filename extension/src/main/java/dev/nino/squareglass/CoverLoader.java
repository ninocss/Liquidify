/* Native catalog artwork only; fixed CDN, bounded decode and cache. */
package dev.nino.squareglass;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import java.net.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public final class CoverLoader {
    private static final ExecutorService io=Executors.newFixedThreadPool(2);
    private static final Handler main=new Handler(Looper.getMainLooper());
    private static final LruCache<String,Bitmap> cache=new LruCache<>(32);
    private CoverLoader() {}
    public static void load(String id,Consumer<Bitmap> callback) {
        if(!id.matches("[a-zA-Z0-9-]{20,80}"))return;
        Bitmap hit=cache.get(id);if(hit!=null){callback.accept(hit);return;}
        io.execute(() -> {
            HttpURLConnection connection=null;
            try {
                connection=(HttpURLConnection)new URL("https://resources.tidal.com/images/"+id.replace('-','/')+"/160x160.jpg").openConnection();
                connection.setInstanceFollowRedirects(false);connection.setConnectTimeout(5000);connection.setReadTimeout(5000);
                if(connection.getResponseCode()!=200)return;
                byte[] bytes;try(java.io.InputStream input=connection.getInputStream()){bytes=input.readNBytes(524289);}
                if(bytes.length>524288)return;
                BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(bytes,0,bytes.length,bounds);
                if(bounds.outWidth>320 || bounds.outHeight>320)return;
                Bitmap bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.length);if(bitmap!=null){cache.put(id,bitmap);main.post(() -> callback.accept(bitmap));}
            } catch(Exception ignored) { /* A missing cover retains the procedural artwork. */ }
            finally {if(connection!=null)connection.disconnect();}
        });
    }
}
