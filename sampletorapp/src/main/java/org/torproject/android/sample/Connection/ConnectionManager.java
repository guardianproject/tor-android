package org.torproject.android.sample.Connection;

public class ConnectionManager {
    private static final ConnectionManager ourInstance = new ConnectionManager();

    public static ConnectionManager getInstance() {
        return ourInstance;
    }

    private ConnectionManager() {
    }

    public synchronized Address getDestination(String destination, int Port) {
        return new Address(destination, Port);
    }
}
