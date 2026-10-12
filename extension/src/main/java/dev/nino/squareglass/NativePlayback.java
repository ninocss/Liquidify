/* SquareGlass. GPL-3.0-or-later. Uses the pinned host's own queue pipeline. */
package dev.nino.squareglass;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Base64;
import android.widget.Toast;
import com.aspiro.wamp.model.Track;
import com.aspiro.wamp.model.MediaItem;
import com.aspiro.wamp.playqueue.source.model.ItemSource;
import java.io.*;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;

public final class NativePlayback {
    private NativePlayback() {}
    static Context context;
    private static WeakReference<bi.e> host = new WeakReference<>(null);
    private static boolean playing;
    private static int lastId, lastPosition, listened;
    private static long lastTick;
    private static final Handler main = new Handler(Looper.getMainLooper());
    private static final LinkedHashMap<Integer, Entry> history = new LinkedHashMap<>();
    private static boolean countedCurrent;
    private static boolean initialized;
    private static final ExecutorService storage = Executors.newSingleThreadExecutor();
    private static volatile int historyGeneration;
    static final class Entry {
        Track track; int[] hours = new int[24]; long last;
    }
    public static void initialize(Context base) {
        context = base.getApplicationContext();
        if (initialized) return;
        initialized = true;
        // Preferences contain only bounded local metadata/model copies, never stream URLs or auth tokens.
        int generation=historyGeneration;
        storage.execute(() -> {
        LinkedHashMap<Integer,Entry> loaded=new LinkedHashMap<>();
        try {
            JSONArray rows = new JSONArray(context.getSharedPreferences("squareglass_taste_v1", 0).getString("tracks", "[]"));
            for (int i = 0; i < Math.min(160, rows.length()); i++) {
                JSONObject row = rows.getJSONObject(i);
                byte[] bytes = Base64.decode(row.getString("track"), Base64.NO_WRAP);
                if (bytes.length > 131072) continue;
                try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
                    Entry entry = new Entry(); entry.track = (Track) input.readObject(); entry.last = row.getLong("last");
                    JSONArray hours = row.getJSONArray("hours");
                    for (int h = 0; h < 24; h++) entry.hours[h] = Math.max(0, Math.min(10000, hours.optInt(h)));
                    loaded.put(entry.track.getId(), entry);
                }
            }
        } catch (Exception ignored) { loaded.clear(); }
        main.post(() -> { if(generation==historyGeneration) for(Map.Entry<Integer,Entry> entry : loaded.entrySet()) history.putIfAbsent(entry.getKey(),entry.getValue()); });
        });
        main.postDelayed(tick, 5000);
    }
    public static void bind(bi.e player) { host = new WeakReference<>(player); SleepTimer.playerChanged(player); }
    public static void observe(boolean isPlaying) { playing = isPlaying; }
    public static void observeName(String state) { observe("Playing".equals(state)); }
    public static bi.e host() { return host.get(); }
    public static void next(boolean previous) {
        bi.e p = host.get(); if (p == null) return;
        if (previous && p.canSkipToPreviousOrRewind()) p.h(true);
        else if (!previous && p.hasNext()) p.j();
    }
    public static void shuffleHistory(Context from) { autoplay(from,false,true); }
    public static void autoplay(Context from, boolean rediscover) { autoplay(from,rediscover,false); }
    private static void autoplay(Context from, boolean rediscover, boolean shuffled) {
        if (history.isEmpty()) { message(from, "Listen to a few tracks for at least 30 seconds to build your listening profile."); return; }
        bi.e p = host.get(); if (p == null) { message(from, "Open a track first to connect the TIDAL player."); return; }
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY); long now = System.currentTimeMillis();
        List<Entry> ranked = new ArrayList<>(history.values());
        ranked.sort((a,b) -> Double.compare(TimeTaste.score(b.hours, hour, b.last, now, rediscover), TimeTaste.score(a.hours, hour, a.last, now, rediscover)));
        if(shuffled) java.util.Collections.shuffle(ranked);
        List<Track> selected = new ArrayList<>(); Map<Integer,Integer> albums = new HashMap<>();
        for (Entry entry : ranked) {
            int album = entry.track.getAlbum() == null ? 0 : entry.track.getAlbum().getId();
            if (album != 0 && albums.getOrDefault(album, 0) >= 3) continue;
            selected.add(entry.track); albums.put(album, albums.getOrDefault(album, 0) + 1);
            if (selected.size() == 25) break;
        }
        ItemSource source = new ItemSource("squareglass-time", shuffled ? "Shuffle history" : rediscover ? "Rediscover" : "For this time of day", ItemSource.NavigationType.None.INSTANCE, null);
        for (Track track : selected) source.addSourceItem(new Track(track));
        p.getPlayQueue().prepare(source, new com.aspiro.wamp.playqueue.w0(0, false,
            com.aspiro.wamp.enums.ShuffleMode.TURN_OFF, com.aspiro.wamp.enums.RepeatMode.OFF, false, true));
        p.b(0, false, true);
    }
    private static final Runnable tick = new Runnable() { public void run() {
        try {
            bi.e p = host.get();
            if (p != null && p.getCurrentItem() != null) {
                MediaItem item = p.getCurrentItem().getMediaItem(); int position = p.getCurrentMediaPosition();
                long now = SystemClock.elapsedRealtime();
                if (item != null && item.getId() != lastId) { lastId = item.getId(); listened = 0; countedCurrent=false; lastPosition=position; }
                int delta = position - lastPosition;
                long elapsed=now-lastTick;
                if (playing && SquareSettings.flag("learnTime", true) && delta > 0 && delta <= elapsed+1500 && elapsed <= 10000) {
                    listened += Math.min(delta,(int)elapsed);
                    if (listened >= 30000 && item instanceof Track && !((Track)item).isUpload() && !countedCurrent) { countedCurrent=true; record((Track)item); }
                }
                lastPosition = position; lastTick = now;
            }
        } catch (RuntimeException ignored) { /* Native queue may be changing during account/navigation transitions. */ }
        finally { main.postDelayed(this, 5000); }
    }};
    private static void record(Track track) {
        Entry entry = history.get(track.getId());
        if (entry == null) { entry = new Entry(); entry.track = new Track(track); entry.track.setSource(null); history.put(track.getId(), entry); }
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY); entry.hours[hour] = Math.min(10000, entry.hours[hour] + 1); entry.last = System.currentTimeMillis();
        while (history.size() > 160) history.remove(history.keySet().iterator().next());
        save();
    }
    private static void save() {
        List<Entry> snapshot=new ArrayList<>();
        for(Entry original : history.values()) { Entry copy=new Entry(); copy.track=original.track; copy.hours=java.util.Arrays.copyOf(original.hours,24); copy.last=original.last; snapshot.add(copy); }
        int generation=historyGeneration;
        storage.execute(() -> {
        JSONArray rows = new JSONArray();
        for (Entry e : snapshot) try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ObjectOutputStream output = new ObjectOutputStream(bytes)) { output.writeObject(e.track); }
            if (bytes.size() > 131072) continue;
            JSONArray hours = new JSONArray(); for (int hour : e.hours) hours.put(hour);
            rows.put(new JSONObject().put("track", Base64.encodeToString(bytes.toByteArray(), Base64.NO_WRAP)).put("hours", hours).put("last", e.last));
        } catch (Exception ignored) {}
        if(generation==historyGeneration) context.getSharedPreferences("squareglass_taste_v1", 0).edit().putString("tracks", rows.toString()).apply();
        });
    }
    public static void clearHistory() { historyGeneration++; history.clear(); countedCurrent=false; listened=0; if(context!=null) storage.execute(() -> context.getSharedPreferences("squareglass_taste_v1", 0).edit().clear().apply()); }
    public static void logout() { main.post(() -> { clearHistory(); SleepTimer.cancel(); host.clear(); playing=false; }); }
    static void message(Context context, String text) { main.post(() -> Toast.makeText(context, text, Toast.LENGTH_LONG).show()); }
}
