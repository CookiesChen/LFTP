package server;

import service.*;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import static client.Main.ACTION_SEND;

public class Main {

    public static void main(String[] args) throws IOException {

        DatagramSocket socket = new DatagramSocket(9090);
        while(true) {
            byte[] buf = new byte[1024];
            DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length); // 1024
            socket.receive(datagramPacket);
            InetAddress IP = datagramPacket.getAddress();
            int port = datagramPacket.getPort();
            System.out.println(port + " " + IP.getHostAddress());
            TCPPackage clientAction = Convert.ByteToPackage(buf);
            String filename = new String(clientAction.Data());
            DatagramSocket datagramSocket = NetSocket.getFreePort();
            TCPPackage data = new TCPPackage(0, false, 0, true, Convert.intToByteArray(datagramSocket.getLocalPort()));
            try {
                byte[] bytes = Convert.PackageToByte(data);
                DatagramPacket packet = new DatagramPacket(bytes, bytes.length, IP, port);
                datagramSocket.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (clientAction.action() == ACTION_SEND){
                // 服务器接收用户发送数据
                ReceiveThread myThread = new ReceiveThread(datagramSocket, IP, port, filename);
                Thread thread = new Thread(myThread);
                thread.start();
            } else {
                // 服务器给用户发送数据
                SendThread myThread = new SendThread(datagramSocket, port, IP, filename);
                Thread thread = new Thread(myThread);
                thread.start();
            }
        }
    }

}
