package com.cookieschen.course.lftp.client;

import picocli.CommandLine;
import com.cookieschen.course.lftp.service.*;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Main {

    public static final boolean ACTION_SEND  = true;
    public static final boolean ACTION_GET  = false;
    private static InetAddress IP;   // 服务器IP

    private static DatagramSocket datagramSocket;

    public static void main(String[] args) throws IOException {
        Cmd cmd = new Cmd();
        new CommandLine(cmd).parse(args);
        String filename = cmd.filename;
        boolean action;
        String information = "";
        if(cmd.action.equals("lsend")){
            action = ACTION_SEND;
        } else if(cmd.action.equals("lget")){
            action = ACTION_GET;
        } else {
            System.out.println("[Client] [Error] Invalid Action! Try -a lsend/lget !");
            return;
        }
        try {
            IP = InetAddress.getByName(cmd.ip);
        } catch (Exception e){
            System.out.println("[Client] [Error] Invalid IP or URL !");
        }
        int packageTotal = 0;
        if(filename == null){
            System.out.println("[Client] [Error] Empty file name");
            return;
        }
        if (action == ACTION_SEND) {
            packageTotal = (int) FileIO.getPackageTotal("./out/" + filename);
        }
        information = filename + "/" + packageTotal;
        datagramSocket = NetSocket.getFreePort();
        TCPPackage data = new TCPPackage(0, false, 0, action, information.getBytes());
        sendPackage(data, IP, 9090);
        if(action == ACTION_SEND){
            System.out.println("[Client] Send file " + filename + " at local port: " + datagramSocket.getLocalPort() + " to " + IP);
            // 等待服务器发回端口
            TCPPackage receivePackage = receivePackage();
            String portAndPackageTotal = new String(receivePackage.Data());
            int desPort = Integer.parseInt(portAndPackageTotal.split("/")[0]);
            System.out.println("[Client] Get Server port: " + desPort);
            System.out.println("[Client] Start to send file");
            SendThread sendThread = new SendThread(datagramSocket, desPort, IP, filename);
            Thread thread = new Thread(sendThread);
            thread.start();

        } else if(action == ACTION_GET){
            System.out.println("[Client] Get file " + filename + " at local port: " + datagramSocket.getLocalPort() + " from " + IP);
            // 等待服务器发回端口和总数据
            TCPPackage receivePackage = receivePackage();
            String portAndPackageTotal = new String(receivePackage.Data());
            int desPort = Integer.parseInt(portAndPackageTotal.split("/")[0]);
            packageTotal = Integer.parseInt(portAndPackageTotal.split("/")[1]);
            System.out.println("[Client] Get Server port: " + desPort);
            System.out.println("[Client] Start to get file");
            ReceiveThread receiveThread = new ReceiveThread(datagramSocket, IP, desPort, filename, packageTotal);
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
