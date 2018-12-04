package com.cookieschen.course.lftp.service;

import java.io.*;

public class TCPPackage implements Serializable {
    private int ACK; // 确认号
    private boolean FIN; // FIN标志位
    private int seq;     // 序列号
    private boolean action; // 客户端动作
    private byte[] data; // 数据包

    public TCPPackage(){

    }

    public TCPPackage(int ACK, boolean FIN, int seq, boolean action, byte[] data){
        this.ACK = ACK;
        this.FIN = FIN;
        this.seq = seq;
        this.action = action;
        this.data = data;
    }

    public void setData(byte[] data) {
        this.data = new byte[data.length];
        for(int i = 0; i < data.length; i++){
            this.data[i] = data[i];
        }
    }

    public boolean action() {
        return action;
    }

    public byte[] Data() {
        return data;
    }

    int ACK() {
        return ACK;
    }

    int Seq() {
        return seq;
    }

    boolean FIN() {
        return FIN;
    }
}
