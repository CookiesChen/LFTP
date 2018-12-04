package com.cookieschen.course.lftp.server;

import com.cookieschen.course.lftp.service.*;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import static com.cookieschen.course.lftp.client.Main.ACTION_GET;
import static com.cookieschen.course.lftp.client.Main.ACTION_SEND;

public class Main {

    public static void main(String[] args) throws IOException {

        DatagramSocket socket = new DatagramSocket(9090);
        System.out.println("[Server] Listen at port 9090" );
        while(true) {
            byte[] buf = new byte[1024];
            DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length); // 1024
            socket.receive(datagramPacket);
            InetAddress IP = datagramPacket.getAddress();
            int port = datagramPacket.getPort();
            TCPPackage clientAction = Convert.ByteToPackage(buf);
            String information = new String(clientAction.Data());
            String[] informations = information.split("/");
            String filename = informations[0];
            int packageTotal = Integer.parseInt(informations[1]);
            if (clientAction.action() == ACTION_GET){
                packageTotal = (int) FileIO.getPackageTotal("./out/test/" + filename);
            }
            DatagramSocket datagramSocket = NetSocket.getFreePort();
            information = new String(Integer.toString(datagramSocket.getLocalPort()) + '/' + packageTotal);
            TCPPackage data = new TCPPackage(0, false, 0, true, information.getBytes());
            try {
                byte[] bytes = Convert.PackageToByte(data);
                DatagramPacket packet = new DatagramPacket(bytes, bytes.length, IP, port);
                datagramSocket.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (clientAction.action() == ACTION_SEND){
                // 服务器接收用户发送数据
                System.out.println("[Server] Get file " + filename + " at local port: " + datagramSocket.getLocalPort() + " from " + IP);
                ReceiveThread myThread = new ReceiveThread(datagramSocket, IP, port, filename, packageTotal);
                Thread thread = new Thread(myThread);
                thread.start();
            } else {
                // 服务器给用户发送数据
                System.out.println("[Server] Send file " + filename + " at local port: " + datagramSocket.getLocalPort() + " to " + IP);
                SendThread myThread = new SendThread(datagramSocket, port, IP, filename);
                Thread thread = new Thread(myThread);
                thread.start();
            }
        }
    }

}
