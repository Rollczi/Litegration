package dev.rollczi.litegration;

public class Litegration {

    private static final ThreadLocal<Litegration> CURRENT = new ThreadLocal<>();

    private final String address;
    private final int port;

    private Litegration(String address, int port) {
        this.address = address;
        this.port = port;
    }

    public String getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public static Litegration getCurrent() {
        return CURRENT.get();
    }

    public static Runnable initialize(String address, int port) {
        if (CURRENT.get() != null) {
            throw new IllegalStateException("Litegration is already initialized in this thread");
        }
        CURRENT.set(new Litegration(address, port));
        Thread thread = Thread.currentThread();
        return () -> {
            if (Thread.currentThread() != thread) {
                throw new IllegalStateException("Litegration can only be cleaned up from the same thread it was initialized in");
            }
            CURRENT.remove();
        };
    }

}
