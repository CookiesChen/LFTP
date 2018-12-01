package server;

import service.Convert;
import service.FileIO;
import service.NetSocket;
import service.TCPPackage;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
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
        DatagramSocket datagramSocket;

        private int expectedSeqNum = 0;

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
                while(true) {
                    TCPPackage receivePackage = null;
                    try {
                        receivePackage = receivePackage();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    assert receivePackage != null;
                    if (receivePackage.FIN()){
                        replyACK = new TCPPackage(expectedSeqNum, true, 0, true, null);
                        try {
                            sendPackage(replyACK);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        FileIO.byteToFile("./src/test/out.mp3" ,datas);
                        System.out.println("Receive file finish.");
                        break;
                    }
                    if(receivePackage.Seq() == expectedSeqNum) {
                        datas.add(receivePackage.Data());
                        replyACK = new TCPPackage(expectedSeqNum, false, 0, true, null);
                        expectedSeqNum++;
                        try {
                            sendPackage(replyACK);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        try {
                            sendPackage(replyACK);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
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
