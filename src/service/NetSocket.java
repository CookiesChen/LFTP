package service;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Random;

public class NetSocket {
    private static Random random = new Random();

    private Socket socket;

    public static NetSocket custom() throws IOException {
        return new NetSocket();
    }

    private NetSocket() throws IOException {
        socket = new Socket();
        InetSocketAddress inetAddress = new InetSocketAddress(0);
        socket.bind(inetAddress);
    }

    public void freePort() throws IOException {
        if (null == socket || socket.isClosed()) {
            return;
        }

        socket.close();
    }

    int getPort() {
        if (null == socket || socket.isClosed()) {
            return -1;
        }

        return socket.getLocalPort();
    }

}
