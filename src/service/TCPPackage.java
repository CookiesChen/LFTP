package service;

import java.io.*;

public class TCPPackage implements Serializable {
    private int ACK; // 确认号
    private boolean FIN; // FIN标志位
    private int seq;     // 序列号
    private boolean action; // 客户端动作
    private byte[] data; // 数据包

    public TCPPackage(int ACK, boolean FIN, int seq, boolean action, byte[] data){
        this.ACK = ACK;
        this.FIN = FIN;
        this.seq = seq;
        this.action = action;
        this.data = data;
    }

    public boolean action() {
        return action;
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
}
