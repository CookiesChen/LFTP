package service;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Random;

public class NetSocket {
    public static DatagramSocket getFreePort() {
        DatagramSocket socket = null;
        for(int i = 5001; i < 65535; i++){
            try {
                socket = new DatagramSocket(i);
            } catch (IOException ex) {
                continue; // try next port
            }
            break;
        }
        return socket;
    }
}
