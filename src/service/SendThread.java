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
    private int nextseqnum = 0;  // 下一个序号
    private InetAddress IP;      // 目标IP
    private long time;           // 记录时间用于定时器

    private volatile  boolean ReSend = false;
    private List<byte[]> datas;

    private DatagramSocket datagramSocket; // 发送方socket

    private final int N = 10;

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
            if(nextseqnum < base + N && !ReSend) {
                TCPPackage tcpPackage = new TCPPackage(0, false, nextseqnum, ACTION_SEND, datas.get(nextseqnum));
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
                DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length); // 1024
                TCPPackage ACKPackage = null;
                try {
                    datagramSocket.receive(datagramPacket);
                    ACKPackage = Convert.ByteToPackage(buf);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                assert ACKPackage != null;
                if(ACKPackage.FIN()){
                    System.out.println("Send finish. Close in 10s");
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
                    System.out.println("重发 " + base + "-" + nextseqnum);
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
                    System.out.println("重发结束");
                    ReSend = false;
                    time = System.currentTimeMillis();
                }
            }
        }
    }

}