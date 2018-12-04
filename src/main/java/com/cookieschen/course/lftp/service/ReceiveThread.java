package com.cookieschen.course.lftp.service;

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
    long startTime;
    int packageTotal;

    public ReceiveThread(DatagramSocket datagramSocket, InetAddress IP, int port, String filename, int packageTotal){
        this.datagramSocket = datagramSocket;
        this.IP = IP;
        this.port = port;
        this.filename = filename;
        this.packageTotal = packageTotal;
    }

    @Override
    public void run() {
        List<byte[]> datas = new ArrayList<>();
        TCPPackage replyACK = null;
        FileOutputStream outputStream = null;
        try {
            outputStream  = new FileOutputStream(new File("./" + filename));
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
                System.out.println("\n[Success] Receive File.");
                break;
            }
            if(receivePackage.Seq() == expectedSeqNum) {
                rwnd--;
                System.out.print("\rSeep: " + expectedSeqNum*1024/(System.currentTimeMillis() - startTime + 1) + "KB/s, Finished: "
                        +  String.format("%.2f", ((float)(expectedSeqNum+1)/(float) packageTotal * 100)) + "%"
                        + ", in " + (System.currentTimeMillis() - startTime + 1)/1000 + "s");
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
