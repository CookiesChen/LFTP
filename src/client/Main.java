package client;

import java.io.IOException;
import java.net.*;

public class Main {
    public static void main(String[] args) throws IOException {
        sentPackage();
    }

    private static void sentPackage() throws IOException {
        DatagramSocket datagramSocket = new DatagramSocket();
        String data = "这个是我第一个udp的例子..";
        DatagramPacket packet = new DatagramPacket(data.getBytes(), data.getBytes().length, InetAddress.getLocalHost() , 9090);
        datagramSocket.send(packet);
        datagramSocket.close();
    }
}
