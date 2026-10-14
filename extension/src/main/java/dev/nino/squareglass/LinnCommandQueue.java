/* GPL-3.0-or-later. Coalesce pending absolute controls on the serialized write lane. */
package dev.nino.squareglass;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

final class LinnCommandQueue {
    private final Executor executor;
    private final ConcurrentHashMap<String,AtomicLong> revisions=new ConcurrentHashMap<>();
    LinnCommandQueue(Executor executor){this.executor=executor;}
    void execute(String key,Runnable command){
        AtomicLong revision=revisions.computeIfAbsent(key,ignored -> new AtomicLong());
        long requested=revision.incrementAndGet();
        executor.execute(() -> {if(revision.get()==requested)command.run();});
    }
}
