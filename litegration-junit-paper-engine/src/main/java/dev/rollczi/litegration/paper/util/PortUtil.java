package dev.rollczi.litegration.paper.util;

public final class PortUtil {

    private PortUtil() {
    }

    public static int findFreePort() {
        try (java.net.ServerSocket socket = new java.net.ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        } catch (java.io.IOException e) {
            throw new RuntimeException("Could not find a free TCP/IP port", e);
        }
    }

}
