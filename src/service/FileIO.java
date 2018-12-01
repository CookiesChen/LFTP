package service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class FileIO {

    static final int MAX_BYTE = 1024;	//每个byte[]的容量,当前1Kb

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

}
