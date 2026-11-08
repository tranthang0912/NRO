package web;

import database.DatabaseManager;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Immutable snapshots, coalesced by character. Network writes never pause simulation. */
public final class WebPersistence {
    private record Snapshot(String query,Object[] values) {}
    private static final ConcurrentMap<Long,Snapshot> pending=new ConcurrentHashMap<>();
    private static final AtomicBoolean writing=new AtomicBoolean();
    private static final ScheduledExecutorService worker=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"postgresql-saves");t.setDaemon(true);return t;});
    private static volatile long retryAt;
    private static volatile long lastError;
    static {worker.scheduleWithFixedDelay(WebPersistence::drain,100,100,TimeUnit.MILLISECONDS);}
    private WebPersistence(){}
    public static void save(long id,String query,Object[] values){pending.put(id,new Snapshot(query,values.clone()));}
    private static void drain(){
        if(System.currentTimeMillis()<retryAt||!writing.compareAndSet(false,true))return;
        try {
            for(var entry:pending.entrySet()) {
                long id=entry.getKey();Snapshot snapshot=entry.getValue();
                if(!pending.remove(id,snapshot))continue;
                try {DatabaseManager.executeUpdate(snapshot.query,snapshot.values);}
                catch(Exception e){
                    pending.putIfAbsent(id,snapshot);retryAt=System.currentTimeMillis()+3000;
                    if(System.currentTimeMillis()-lastError>10000){lastError=System.currentTimeMillis();System.err.println("PostgreSQL save will retry for player "+id+": "+e.getClass().getSimpleName());}
                    break;
                }
            }
        }finally{writing.set(false);}
    }
    public static void flush(long timeoutMs){
        long deadline=System.currentTimeMillis()+timeoutMs;
        while((!pending.isEmpty()||writing.get())&&System.currentTimeMillis()<deadline){
            try{Thread.sleep(50);}catch(InterruptedException e){Thread.currentThread().interrupt();break;}
        }
        if(!pending.isEmpty())System.err.println("WARNING: "+pending.size()+" character snapshots could not be persisted before shutdown.");
        worker.shutdown();
    }
}
