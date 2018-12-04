package com.cookieschen.course.lftp.service;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import static com.cookieschen.course.lftp.client.Main.ACTION_SEND;
import static com.cookieschen.course.lftp.service.FileIO.BLOCK_PACKAGE_NUM;

public class SendThread implements Runnable{

    private int desPort;         // 接收方端口
    private volatile int base = 0;        // 基序号
    private volatile int nextseqnum = 0;  // 下一个序号
    private InetAddress IP;      // 目标IP

    /* GBN*/
    private volatile long time;           // 记录时间用于定时器
    private volatile int rwnd = 500;          // 接收窗口
    private volatile  boolean ReSend = false;

    /* Congestion control*/
    private static final int SLOW_START = 0;        // 慢启动
    private static final int CONGESTION_AVOID = 1;  // 拥塞避免
    private volatile int state = SLOW_START;        // 拥塞控制状态
    private double cwnd = 10;               // 拥塞窗口
    private volatile int lastACK = -1;               // 最后确认ACK
    private volatile int duplicateACK = 0;          // 冗余ACK
    private volatile double ssthresh = 50;                   // 慢启动阈值

    /* big file*/
    private int packageTotal;					    // package总数
    private long bytesTotal; 						// byte[]总数目
    private volatile int blockCur = 0;				    // 当前块序号

    private volatile List<TCPPackage> datas = new ArrayList<>();

    private DatagramSocket datagramSocket; // socket
    private String filename;

    long startTime = 0;

    public SendThread(DatagramSocket datagramSocket, int desPort, InetAddress IP, String filename){
        this.datagramSocket = datagramSocket;
        this.desPort = desPort;
        this.IP =  IP;
        this.filename = filename;
    }

    @Override
    public void run() {

        String path = "./out/" + filename;
        packageTotal = FileIO.getPackageTotal(path);
        bytesTotal = FileIO.getByteTotal(path);

        Thread rThread = new Thread(new ReceiveACKThread());
        rThread.start();

        Thread tThread = new Thread(new TimeOut());
        tThread.start();

        startTime = System.currentTimeMillis();
        for (blockCur = 0; blockCur < Math.floor(packageTotal / (float) BLOCK_PACKAGE_NUM) + 1; blockCur++){
            List<byte[]> byteList = FileIO.getByteList(blockCur, path, packageTotal, bytesTotal);
            datas.clear();
            for(int j = 0; j < byteList.size(); j++) {
                datas.add(new TCPPackage(0, false, blockCur*BLOCK_PACKAGE_NUM + j, true, byteList.get(j)));
            }
            while(nextseqnum < datas.size() + blockCur * BLOCK_PACKAGE_NUM) {
                if(!ReSend && nextseqnum < base + cwnd) {
                    TCPPackage tcpPackage;
                    if (rwnd <= 0){
                        tcpPackage = new TCPPackage(0, false, -1, ACTION_SEND, null);
                    } else {
                        tcpPackage = datas.get(nextseqnum % BLOCK_PACKAGE_NUM);
                    }
                    byte[] bytes = Convert.PackageToByte(tcpPackage);
                    DatagramPacket packet = new DatagramPacket(bytes, bytes.length, IP , desPort);
                    try {
                        datagramSocket.send(packet);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (base == nextseqnum){
                        time = System.currentTimeMillis();
                    }
                    nextseqnum++;
                }
            }
            while (lastACK < datas.size() - 1 + blockCur * BLOCK_PACKAGE_NUM) {}
        }
        System.out.println("\n[Success] Send File.");
        // 阻塞等待最后传输完成
        while (base < packageTotal);

        // Send FIN
        TCPPackage tcpPackage = new TCPPackage(0, true, nextseqnum, ACTION_SEND, null);
        byte[] bytes = Convert.PackageToByte(tcpPackage);
        DatagramPacket packet = new DatagramPacket(bytes, bytes.length, IP , desPort);
        try {
            datagramSocket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class ReceiveACKThread implements Runnable{

        @Override
        public void run() {
            while (true) {
                byte[] buf = new byte[1024];
                DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length);
                TCPPackage ACKPackage = null;
                try {
                    datagramSocket.receive(datagramPacket);
                    ACKPackage = Convert.ByteToPackage(buf);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                assert ACKPackage != null;
                rwnd = Convert.byteArrayToInt(ACKPackage.Data());
                if(ACKPackage.FIN()){
                    System.out.println("[Client] Send finish. Close in 10s");
                    break;
                }
                System.out.print("\rSeep: " + (lastACK + 1)*1024/(System.currentTimeMillis() - startTime + 1) + "KB/s, Finished: "
                            +  String.format("%.2f", ((float)(lastACK+1)/(float) packageTotal * 100)) + "%"
                            + ", in " + (System.currentTimeMillis() - startTime + 1)/1000 + "s");
                if (lastACK + 1 == ACKPackage.ACK()) {
                    if (state == SLOW_START) {
                        cwnd++;
                        if (cwnd > ssthresh) state = CONGESTION_AVOID;
                    }
                    else {
                        cwnd += (double)(1 / cwnd);
                    }
                    duplicateACK = 0;
                }   else{
                    duplicateACK++;
                }

                if (duplicateACK >= 3) {
                    ssthresh = cwnd / 2;
                    cwnd = ssthresh + 3;
                    state = CONGESTION_AVOID;
                }

                lastACK = ACKPackage.ACK();
                base = ACKPackage.ACK() + 1;
                if (base != nextseqnum){
                    time = System.currentTimeMillis();
                }
                if (ACKPackage.ACK() == packageTotal - 1) break;
            }
        }
    }

    class TimeOut implements Runnable{

        private long TTL = 300;

        @Override
        public void run() {
            while(true){
                if (base >= packageTotal - 1) break;
                if(System.currentTimeMillis() - time > TTL){
                    ssthresh = cwnd/2 + 1;
                    cwnd = ssthresh;
                    state = SLOW_START;
                    ReSend = true;
                    // 重发数据包
                    int start = base;
                    int end = nextseqnum;
                    for (int i = start; i < end; i++){
                        TCPPackage tcpPackage = datas.get(i % BLOCK_PACKAGE_NUM);
                        byte[] bytes = Convert.PackageToByte(tcpPackage);
                        DatagramPacket packet = new DatagramPacket(bytes, bytes.length, IP , desPort);
                        try {
                            datagramSocket.send(packet);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    ReSend = false;
                }
            }
        }
    }
}