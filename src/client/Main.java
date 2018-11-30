package client;

import service.Convert;
import service.NetSocket;

import java.io.IOException;
import java.net.*;
import java.util.Random;
import service.TCPPackage;

public class Main {
    public static void main(String[] args) throws IOException {
        sentPackage();
    }

    private static void sentPackage() throws IOException {
        DatagramSocket datagramSocket = new DatagramSocket();

        TCPPackage data = new TCPPackage(0, false,true, getClientSeq(), null);
        byte[] bytes = Convert.PackageToByte(data);

        DatagramPacket packet = new DatagramPacket(bytes, bytes.length, InetAddress.getLocalHost() , 9090);
        datagramSocket.send(packet);
        datagramSocket.close();
    }

    private static int getClientSeq(){
        Random rand =new Random(25);
        return rand.nextInt(100);
    }

    public static class ClientThread implements Runnable {

        private NetSocket socket; // 客户空闲socket
        private InetAddress IP;   // 服务器IP
        private int port;         // 服务器口

        ClientThread(NetSocket socket, InetAddress IP, int port){
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
