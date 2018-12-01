package client;

import service.*;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.List;

public class Main {

    public static final boolean ACTION_SEND  = true;
    public static final boolean ACTION_GET  = false;
    private static InetAddress IP;   // 服务器IP

    private static DatagramSocket datagramSocket;

    private static List<byte[]> datas = FileIO.fileToByte("./src/test/Over the Horizon.mp3");

    public static void main(String[] args) throws IOException {

        boolean action = ACTION_SEND;
        IP = InetAddress.getLocalHost();
        if(action == ACTION_SEND){
            datagramSocket = NetSocket.getFreePort();
            TCPPackage data = new TCPPackage(0, false, 0, ACTION_SEND, null);
            sendPackage(data, InetAddress.getLocalHost(), 9090);
            System.out.println("[Client] Ask for sending file at port: " + datagramSocket.getLocalPort());
            TCPPackage receivePackage = receivePackage();
            // 服务器端口
            int desPort = Convert.byteArrayToInt(receivePackage.Data());
            System.out.println("[Client] Get Server port: " + desPort);
            System.out.println("[Client] Start to send file");
            SendThread sendThread = new SendThread(datagramSocket,desPort, datas, IP);

            Thread sThread = new Thread(sendThread);
            sThread.start();

        } else if(action == ACTION_GET){

        }
    }

    private static TCPPackage receivePackage() throws IOException {
        byte[] buf = new byte[1024];
        DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length); // 1024
        datagramSocket.receive(datagramPacket);
        return Convert.ByteToPackage(buf);
    }

    private static void sendPackage(TCPPackage tcpPackage, InetAddress IP, int port) throws IOException {
        byte[] bytes = Convert.PackageToByte(tcpPackage);
        DatagramPacket packet = new DatagramPacket(bytes, bytes.length, IP , port);
        datagramSocket.send(packet);
    }

}
