package com.cookieschen.course.lftp.service;

import java.io.IOException;
import java.net.DatagramSocket;

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
