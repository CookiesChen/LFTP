package server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Main {

    public static void main(String[] args) throws IOException {
        DatagramSocket socket = new DatagramSocket(9090);
        // 监听9090端口
        while(true) {
            byte[] buf = new byte[1024];
            DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length); // 1024
            socket.receive(datagramPacket);

            MyThread myThread = new MyThread(NetSocket.custom(), datagramPacket.getAddress(), datagramPacket.getPort());

            Thread thread = new Thread(myThread);
            thread.start();
        }
    }

    public static class MyThread implements Runnable {

        private NetSocket socket; // 服务器空闲socket
        private InetAddress IP;   // 接收方IP
        private int port;         // 接收方端口

        MyThread(NetSocket socket, InetAddress IP, int port){
            this.socket = socket;
            this.IP = IP;
            this.port = port;
        }

        @Override
        public void run() {
            System.out.println("Receiver's IP: " + IP.getHostAddress());
            System.out.println("Receiver's port: " + port);
        }
    }
}
