/* GPL-3.0-or-later. Bounded, process-local metadata cache; never stores OAuth. */
package dev.nino.squareglass;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.LongSupplier;

final class LinnCache<K,V> {
    private final int capacity;
    private final long ttl;
    private final LongSupplier clock;
    private final Map<K,Entry<V>> entries = new LinkedHashMap<>(16, .75f, true);
    private static final class Entry<V> {
        final V value; final long created;
        Entry(V value,long created) { this.value=value; this.created=created; }
    }
    LinnCache(int capacity,long ttl) { this(capacity,ttl,System::nanoTime); }
    LinnCache(int capacity,long ttl,LongSupplier clock) {
        this.capacity=capacity; this.ttl=ttl; this.clock=clock;
    }
    synchronized V get(K key) {
        Entry<V> entry=entries.get(key);
        if(entry==null)return null;
        if(clock.getAsLong()-entry.created>=ttl){entries.remove(key);return null;}
        return entry.value;
    }
    synchronized void put(K key,V value) {
        entries.put(key,new Entry<>(value,clock.getAsLong()));
        while(entries.size()>capacity)entries.remove(entries.keySet().iterator().next());
    }
    synchronized void remove(K key) { entries.remove(key); }
    synchronized void clear() { entries.clear(); }
}
