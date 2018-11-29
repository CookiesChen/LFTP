package server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class Main {
    public static void main(String[] args) throws IOException {
        DatagramSocket socket = new DatagramSocket(9090);
        // 监听9090端口
        while(true) {
            byte[] buf = new byte[1024];
            DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length); // 1024
            socket.receive(datagramPacket);
            System.out.println("Receive packet from: " + datagramPacket.getAddress());
            // TODO 创建子线程处理收到的数据
        }
    }
}
