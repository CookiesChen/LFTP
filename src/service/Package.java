package service;

public class Package {
    private boolean ACK; // 确认号
    private boolean FIN; // FIN标志位
    private boolean SYN; // SYN标志位
    private int seq;     // 序列号
    private byte[] data; // 数据包
}
