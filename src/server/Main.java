package server;

import service.Convert;
import service.FileIO;
import service.NetSocket;
import service.TCPPackage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static client.Main.ACTION_GET;
import static client.Main.ACTION_SEND;

public class Main {

    public static void main(String[] args) throws IOException {

        DatagramSocket socket = new DatagramSocket(9090);
        while(true) {
            byte[] buf = new byte[1024];
            DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length); // 1024
            socket.receive(datagramPacket);
            TCPPackage clientAction = Convert.ByteToPackage(buf);
            ServerThread myThread = new ServerThread(datagramPacket.getAddress(), datagramPacket.getPort(), clientAction);
            Thread thread = new Thread(myThread);
            thread.start();
        }
    }

    public static class ServerThread implements Runnable {

        private InetAddress IP;          // 客户端IP
        private int port;                // 客户端端口
        private TCPPackage clientAction; // 客户端动作
        DatagramSocket datagramSocket;   // socket

        private final int RevBuff = 20000; // 接收缓存
        private volatile int rwnd = RevBuff; // 接收窗口

        private int expectedSeqNum = 0;  // GBN控制

        private long startTime = 0;      // 下载速度计算
        private int count = 0;

        ServerThread(InetAddress IP, int port, TCPPackage clientAction){
            this.IP = IP;
            this.port = port;
            this.clientAction = clientAction;
        }

        @Override
        public void run() {
            datagramSocket = NetSocket.getFreePort();
            if (clientAction.action() == ACTION_SEND){
                System.out.println("[Server] Receive file from " + IP.getHostAddress() + " Port "+ port);
                TCPPackage data = new TCPPackage(0, false, 0, true, Convert.intToByteArray(datagramSocket.getLocalPort()));
                try {
                    sendPackage(data);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println("[Server] Receive file at port " + datagramSocket.getLocalPort());
                List<byte[]> datas = new ArrayList<>();
                TCPPackage replyACK = null;
                FileOutputStream outputStream = null;
                try {
                    outputStream  = new FileOutputStream(new File("./src/test/out.mp3"));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                startTime = System.currentTimeMillis();
                while(true) {
                    TCPPackage receivePackage = null;
                    try {
                        receivePackage = receivePackage();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    assert receivePackage != null;
                    if (receivePackage.FIN()){
                        replyACK = new TCPPackage(expectedSeqNum, true, 0, true, Convert.intToByteArray(rwnd));
                        try {
                            sendPackage(replyACK);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        try {
                            FileIO.byteToFile(outputStream, datas);
                            outputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        System.out.println();
                        System.out.println("Receive file finish.");
                        break;
                    }
                    if(receivePackage.Seq() == expectedSeqNum) {
                        count++;
                        long downloadTime = (System.currentTimeMillis() - startTime + 1);
                        System.out.print("\r" + count * 1024 / downloadTime + "KB/s in " + downloadTime / 1000 + "s" );
                        rwnd--;
                        datas.add(receivePackage.Data());
                        replyACK = new TCPPackage(expectedSeqNum, false, 0, true, Convert.intToByteArray(rwnd));
                        expectedSeqNum++;
                        try {
                            replyACK.setData(Convert.intToByteArray(rwnd));
                            sendPackage(replyACK);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        try {
                            assert replyACK != null;
                            replyACK.setData(Convert.intToByteArray(rwnd));
                            sendPackage(replyACK);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    // 接收窗口已满 阻塞
                    if (rwnd == 0){
                        FileIO.byteToFile(outputStream, datas);
                        datas.clear();
                        rwnd = RevBuff;
                    }
                }
            }else if(clientAction.action() == ACTION_GET) {
                System.out.println("[Server] Send file to " + IP.getHostAddress());

            }
        }

        void sendPackage(TCPPackage tcpPackage) throws IOException {
            byte[] bytes = Convert.PackageToByte(tcpPackage);
            DatagramPacket packet = new DatagramPacket(bytes, bytes.length, IP , port);
            datagramSocket.send(packet);
        }

        TCPPackage receivePackage() throws IOException {
            byte[] buf = new byte[1400];
            DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length); // 1024
            datagramSocket.receive(datagramPacket);
            return Convert.ByteToPackage(buf);
        }
    }
}
