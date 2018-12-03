package service;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FileIO {

    static final int MAX_BYTE = 1024;	                   // 每个byte[]的容量,当前1Kb
    private final static int BLOCK_SIZE = 1024 * 1024 * 10;
    final static int BLOCK_PACKAGE_NUM = BLOCK_SIZE / MAX_BYTE; // 每块package数
    private static int packageTotal;
    private static long bytesTotal;

    public static List<byte[]> fileToByte(String path) {
        try {
            FileInputStream inStream =new FileInputStream(new File(path));
            List<byte[]> bytes = new ArrayList<>();
            long BytesTotal = inStream.available();
            int packageNum = (int)Math.floor(BytesTotal / MAX_BYTE);
            int leave = (int)BytesTotal % MAX_BYTE;
            if(packageNum > 0) {
                for(int i = 0; i < packageNum; i++) {
                    byte[] data;
                    data = new byte[MAX_BYTE];
                    inStream.read(data, 0, MAX_BYTE);
                    bytes.add(data);
                }
            }
            // 处理最后剩余的部分字符
            byte[] data = new byte[leave];
            inStream.read(data, 0, leave);
            bytes.add(data);
            inStream.close();
            System.out.println("一共需要发送" + packageNum + "段");
            return bytes;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void byteToFile(FileOutputStream outputStream, List<byte[]> datas) {
        try {
            for (byte[] data : datas) {
                outputStream.write(data);
                outputStream.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 获得总bytes
    static long getByteTotal(String path) {
        File file = new File(path);
        bytesTotal = file.length();
        return bytesTotal;
    }

    // 获得总package
    static int getPackageTotal(String path) {
        File file = new File(path);
        long BytesTotal = file.length();
        packageTotal = (int)Math.floor(BytesTotal / MAX_BYTE) + 1;
        return packageTotal;
    }
    // 获得对应分隔文件块
    static List<byte[]> getByteList(int blockNum, String path){
        List<byte[]> ByteList = new ArrayList<>();
        try {
            FileInputStream inStream = new FileInputStream(new File(path));
            for(int i = 0; i < blockNum; i++){
                inStream.skip(BLOCK_SIZE);
            }
            if((blockNum + 1) * BLOCK_PACKAGE_NUM >  packageTotal){
                int len = packageTotal % BLOCK_PACKAGE_NUM;
                for (int i = 0; i < len - 1; i++){
                    byte[] data = new byte[MAX_BYTE];
                    inStream.read(data, 0, MAX_BYTE);
                    ByteList.add(data);
                }
                byte[] data = new byte[(int) (bytesTotal % MAX_BYTE)];
                inStream.read(data, 0, (int) (bytesTotal % MAX_BYTE));
                ByteList.add(data);
            } else{
                for (int i = 0; i < BLOCK_PACKAGE_NUM; i++){
                    byte[] data = new byte[MAX_BYTE];
                    inStream.read(data, 0, MAX_BYTE);
                    ByteList.add(data);
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        return ByteList;
    }
}
