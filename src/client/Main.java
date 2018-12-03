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


    public static void main(String[] args) throws IOException {

        String filename = "1.zip";
        boolean action = ACTION_GET;
        IP = InetAddress.getLocalHost();
        datagramSocket = NetSocket.getFreePort();
        if(filename == null){
            System.out.println("Empty file name");
        }
        TCPPackage data = new TCPPackage(0, false, 0, action, filename.getBytes());
        sendPackage(data, IP, 9090);
        if(action == ACTION_SEND){
            System.out.println("[Client] Send file " + filename + " at local port: " + datagramSocket.getLocalPort());
            // 等待服务器发回端口
            TCPPackage receivePackage = receivePackage();
            int desPort = Convert.byteArrayToInt(receivePackage.Data());
            System.out.println("[Client] Get Server port: " + desPort);
            System.out.println("[Client] Start to send file");
            SendThread sendThread = new SendThread(datagramSocket, desPort, IP, filename);
            Thread thread = new Thread(sendThread);
            thread.start();

        } else if(action == ACTION_GET){
            System.out.println("[Client] Get file " + filename + " at local port: " + datagramSocket.getLocalPort());
            // 等待服务器发回端口
            TCPPackage receivePackage = receivePackage();
            int desPort = Convert.byteArrayToInt(receivePackage.Data());
            System.out.println("[Client] Get Server port: " + desPort);
            System.out.println("[Client] Start to get file");
            ReceiveThread receiveThread = new ReceiveThread(datagramSocket, IP, desPort, filename);
            Thread thread = new Thread(receiveThread);
            thread.start();
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
