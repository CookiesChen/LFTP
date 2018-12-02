package service;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.List;

import static client.Main.ACTION_SEND;

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
    private static final int QUICK_RECOVERY = 2;    // 快速恢复
    private volatile int state = SLOW_START;        // 拥塞控制状态
    private volatile double cwnd = 1;               // 拥塞窗口
    private volatile int lastACK = 0;               // 最后确认ACK
    private volatile int duplicateACK = 0;          // 冗余ACK
    private double ssthresh = 20;                   // 慢启动阈值

    private List<byte[]> datas;

    private DatagramSocket datagramSocket; // 发送方socket



    public SendThread(DatagramSocket datagramSocket, int desPort, List<byte[]> datas, InetAddress IP){
        this.datagramSocket = datagramSocket;
        this.desPort = desPort;
        this.datas = datas;
        this.IP =  IP;
    }

    @Override
    public void run() {
        Thread rThread = new Thread(new ReceiveACKThread());
        rThread.start();

        Thread tThread = new Thread(new TimeOut());
        tThread.start();

        while(nextseqnum < datas.size()) {
            if(nextseqnum < base + cwnd && !ReSend) {
                TCPPackage tcpPackage;
                if (rwnd <= 0){
                    tcpPackage = new TCPPackage(0, false, -1, ACTION_SEND, null);
                } else {
                    tcpPackage = new TCPPackage(0, false, nextseqnum, ACTION_SEND, datas.get(nextseqnum));
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

        // 阻塞等待最后传输完成
        while (base < datas.size());

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
                    System.out.println("Send finish. Close in 10s");
                    break;
                }
                switch (state){
                    case SLOW_START:
                        if(lastACK >= ACKPackage.ACK()){
                            duplicateACK++;
                        } else {
                            cwnd = cwnd + 1;
                            duplicateACK = 0;
                        }
                        /* 状态转移*/
                        if (duplicateACK == 3){
                            ssthresh = cwnd / 2;
                            cwnd = ssthresh + 3;
                            state = QUICK_RECOVERY;
                        }
                        if (cwnd >= ssthresh){
                            state = CONGESTION_AVOID;
                        }
                        break;
                    case QUICK_RECOVERY:
                        if(lastACK >= ACKPackage.ACK()){
                            cwnd = cwnd + 1;
                        } else {
                            cwnd = ssthresh;
                            duplicateACK = 0;
                            state = CONGESTION_AVOID;
                        }
                        break;
                    case CONGESTION_AVOID:
                        if(lastACK >= ACKPackage.ACK()){
                            duplicateACK++;
                        } else {
                            cwnd = cwnd + 1/cwnd;
                            duplicateACK = 0;
                        }
                        /* 状态转移*/
                        if (duplicateACK == 3){
                            ssthresh = cwnd / 2;
                            cwnd = ssthresh + 3;
                            state = QUICK_RECOVERY;
                        }
                        break;
                }

                base = ACKPackage.ACK() + 1;
                if (base != nextseqnum){
                    time = System.currentTimeMillis();
                }
                if (ACKPackage.ACK() == datas.size()) break;
            }
        }
    }

    class TimeOut implements Runnable{

        private long TTL = 300;

        @Override
        public void run() {
            while(true){
                if (base == datas.size()) break;
                if(System.currentTimeMillis() - time > TTL){
                    ssthresh = cwnd/2;
                    cwnd = 1;
                    duplicateACK = 0;
                    state = SLOW_START;
                    time = System.currentTimeMillis();
                    System.out.println("[Client] ReSend. Package Num " + base + " - " + nextseqnum);
                    ReSend = true;
                    // 重发数据包
                    for (int i = base; i < nextseqnum; i++){
                        TCPPackage tcpPackage = new TCPPackage(0, false, i, ACTION_SEND, datas.get(i));
                        byte[] bytes = Convert.PackageToByte(tcpPackage);
                        DatagramPacket packet = new DatagramPacket(bytes, bytes.length, IP , desPort);
                        try {
                            datagramSocket.send(packet);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    System.out.println("[Client] ReSend Finish.");
                    ReSend = false;
                }
            }
        }
    }

}