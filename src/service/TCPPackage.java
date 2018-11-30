package service;

import java.io.*;

public class TCPPackage implements Serializable {
    private int ACK; // 确认号
    private boolean FIN; // FIN标志位
    private boolean SYN; // SYN标志位
    private int seq;     // 序列号
    private byte[] data; // 数据包

    public TCPPackage(int ACK, boolean FIN, boolean SYN, int seq, byte[] data){
        this.ACK = ACK;
        this.FIN = FIN;
        this.SYN = SYN;
        this.seq = seq;
        this.data = data;
    }

    public byte[] Data() {
        return data;
    }

    public int ACK() {
        return ACK;
    }

    public int Seq() {
        return seq;
    }

    public boolean FIN() {
        return FIN;
    }

    public boolean SYN() {
        return SYN;
    }
}
