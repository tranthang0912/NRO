package web;

import database.DatabaseManager;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/** Immutable snapshots, coalesced by character. Network writes never pause simulation. */
public final class WebPersistence {
    private enum Kind { PLAYER, ACCOUNT_LOGIN, ACCOUNT_LOGOUT, CLAN }
    private record Key(Kind kind, long id) {}
    private record Snapshot(String query,Object[] values) {}
    private static final ConcurrentMap<Key,Snapshot> pending=new ConcurrentHashMap<>();
    private static final ConcurrentMap<Long, Long> playerVersions = new ConcurrentHashMap<>();
    private static final AtomicLong saveVersion = new AtomicLong();
    private static final AtomicBoolean writing=new AtomicBoolean();
    private static final ScheduledExecutorService worker=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"postgresql-saves");t.setDaemon(true);return t;});
    private static volatile long retryAt;
    private static volatile long lastError;
    static {worker.scheduleWithFixedDelay(WebPersistence::drain,100,100,TimeUnit.MILLISECONDS);}
    private WebPersistence(){}
    public static void save(long id,String query,Object[] values){save(Kind.PLAYER,id,query,values);}
    public static void saveClan(int id,String query,Object[] values){save(Kind.CLAN,id,query,values);}
    public static void accountLogin(int id, String ip) {
        save(Kind.ACCOUNT_LOGIN, id, "UPDATE account SET last_time_login=?, ip_address=? WHERE id=?",
            new Object[]{new java.sql.Timestamp(System.currentTimeMillis()), ip, id});
    }
    public static void accountLogout(int id) {
        save(Kind.ACCOUNT_LOGOUT, id, "UPDATE account SET last_time_logout=? WHERE id=?",
            new Object[]{new java.sql.Timestamp(System.currentTimeMillis()), id});
    }
    static boolean playerSavePending(long id) {
        return pending.containsKey(new Key(Kind.PLAYER, id)) || pending.containsKey(new Key(Kind.PLAYER, -id));
    }
    static long saveVersion() { return saveVersion.get(); }
    static boolean playerChangedSince(long id, long version) {
        return playerSavePending(id) || playerVersions.getOrDefault(id, 0L) > version;
    }
    private static void save(Kind kind,long id,String query,Object[] values){pending.put(new Key(kind,id),new Snapshot(query,values.clone()));}
    private static void drain(){
        if(System.currentTimeMillis()<retryAt||!writing.compareAndSet(false,true))return;
        try {
            for(var entry:pending.entrySet()) {
                Key key=entry.getKey();Snapshot snapshot=entry.getValue();
                if(pending.get(key)!=snapshot)continue;
                try {
                    DatabaseManager.executeUpdate(snapshot.query,snapshot.values);
                    if (key.kind() == Kind.PLAYER) {
                        playerVersions.put(Math.abs(key.id()), saveVersion.incrementAndGet());
                    }
                    pending.remove(key,snapshot);
                }
                catch(Exception e){
                    retryAt=System.currentTimeMillis()+3000;
                    if(System.currentTimeMillis()-lastError>10000){lastError=System.currentTimeMillis();System.err.println("PostgreSQL save will retry for "+key.kind()+" "+key.id()+": "+e.getClass().getSimpleName());}
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
        if(!pending.isEmpty())System.err.println("WARNING: "+pending.size()+" snapshots could not be persisted before shutdown.");
        worker.shutdown();
    }
}
