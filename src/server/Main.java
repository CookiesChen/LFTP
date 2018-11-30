package server;

import service.Convert;
import service.NetSocket;
import service.TCPPackage;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Main {

    public static void main(String[] args) throws IOException {

        DatagramSocket socket = new DatagramSocket(9090);
        while(true) {
            byte[] buf = new byte[1024];
            DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length); // 1024
            socket.receive(datagramPacket);

            TCPPackage SYNSegment = Convert.ByteToPackage(buf);

            ServerThread myThread = new ServerThread(NetSocket.custom(), datagramPacket.getAddress(), datagramPacket.getPort(), SYNSegment);

            Thread thread = new Thread(myThread);
            thread.start();
        }
    }

    public static class ServerThread implements Runnable {

        private NetSocket socket;       // 服务器空闲socket
        private InetAddress IP;         // 接收方IP
        private int port;               // 接收方端口
        private TCPPackage SYNSegment; //  SYN报文段

        ServerThread(NetSocket socket, InetAddress IP, int port, TCPPackage SYNSegment){
            this.socket = socket;
            this.IP = IP;
            this.port = port;
            this.SYNSegment = SYNSegment;
        }

        @Override
        public void run() {

        }
    }
}
