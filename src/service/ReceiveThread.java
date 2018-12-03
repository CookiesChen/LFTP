package service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class ReceiveThread implements Runnable {

    private InetAddress IP;          // 发送方IP
    private int port;                // 发送方端口
    DatagramSocket datagramSocket;   // socket

    private final int RevBuff = 20000; // 接收缓存
    private volatile int rwnd = RevBuff; // 接收窗口

    private volatile int expectedSeqNum = 0;  // GBN控制

    String filename;


    public ReceiveThread(DatagramSocket datagramSocket, InetAddress IP, int port, String filename){
        this.datagramSocket = datagramSocket;
        this.IP = IP;
        this.port = port;
        this.filename = filename;
    }

    @Override
    public void run() {
        List<byte[]> datas = new ArrayList<>();
        TCPPackage replyACK = null;
        FileOutputStream outputStream = null;
        try {
            outputStream  = new FileOutputStream(new File("./src/test/" + filename));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
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
                break;
            }
            if(receivePackage.Seq() == expectedSeqNum) {
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
    }

    private void sendPackage(TCPPackage tcpPackage) throws IOException {
        byte[] bytes = Convert.PackageToByte(tcpPackage);
        DatagramPacket packet = new DatagramPacket(bytes, bytes.length, IP , port);
        datagramSocket.send(packet);
    }

    private TCPPackage receivePackage() throws IOException {
        byte[] buf = new byte[1400];
        DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length); // 1024
        datagramSocket.receive(datagramPacket);
        return Convert.ByteToPackage(buf);
    }
}
