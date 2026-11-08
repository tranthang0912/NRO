package network;

import java.util.ArrayList;
import java.util.Iterator;
import network.inetwork.ISession;
import java.util.List;

public class SessionManager {
    private static SessionManager instance;
    private final List<ISession> sessions = new ArrayList<ISession>();

    public static SessionManager gI() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void putSession(ISession session) {
        this.sessions.add(session);
    }

    public void removeSession(ISession session) {
        this.sessions.remove(session);
    }

    public List<ISession> getSessions() {
        return this.sessions;
    }

    public void cleanupSessions() {
        Iterator<ISession> iterator = sessions.iterator();
        while (iterator.hasNext()) {
            ISession session = iterator.next();
            if (!session.isConnected()) {
                iterator.remove();
                removeSession(session);
                session.dispose();
            }
        }
    }
    public void startCleanupThread() {
        Thread cleanupThread = new Thread(() -> {
            while (true) {
                cleanupSessions();
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        cleanupThread.setDaemon(true);
        cleanupThread.start();
    }
    
    public ISession findByID(long id) throws Exception {
        if (this.sessions.isEmpty()) {
            throw new Exception("Session " + id + " does not exist");
        }
        for (ISession session : this.sessions) {
            if (session.getID() > id) {
                throw new Exception("Session " + id + " does not exist");
            }
            if (session.getID() != id) continue;
            return session;
        }
        throw new Exception("Session " + id + " does not exist");
    }

    public int getNumSession() {
        return this.sessions.size();
    }
}

