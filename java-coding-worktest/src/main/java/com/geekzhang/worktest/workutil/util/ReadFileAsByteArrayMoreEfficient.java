package com.geekzhang.worktest.workutil.util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

/**
 * @author zwm
 * @desc ReadFileAsByteArrayMoreEfficient
 * @date 2024年08月19日 15:32
 */
public class ReadFileAsByteArrayMoreEfficient {

    //读取流
    public static byte[] readFileAsByteArray(String urlString) {
        try (ReadableByteChannel channel = Channels.newChannel(new URL(urlString).openStream())) {
            ByteBuffer buffer = ByteBuffer.allocate(1024 * 1024);  // 1MB 缓冲区
            int bytesRead;
            byte[] fileBytes = new byte[0];

            while ((bytesRead = channel.read(buffer))!= -1) {
                buffer.flip();
                byte[] temp = new byte[buffer.remaining()];
                buffer.get(temp);
                byte[] newFileBytes = new byte[fileBytes.length + temp.length];
                System.arraycopy(fileBytes, 0, newFileBytes, 0, fileBytes.length);
                System.arraycopy(temp, 0, newFileBytes, fileBytes.length, temp.length);
                fileBytes = newFileBytes;
                buffer.clear();
            }

            return fileBytes;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String[] args) {

//        String url = "http://12114.22110.21123.117/upload/Recordings/2024-08-19/3ee62ebd-277f-45c3-b9ea-43e263ea36f5.wav";
//        byte[] fileBytes = readFileAsByteArray(url);
//        if (fileBytes!= null) {
//            // 在此处对字节数组进行进一步处理
//        }
        String url = "http://12141s.2ss20.22223.117/upl22oad/Record22ings/2024-08-19/3ee62ebd-277f-45c3-b9ea-43e263ea36f5.wav";
        String localFilePath = "/Users/admin/Downloads/jilsssianyun.wav";  // 替换为您期望的本地文件路径
        generateLocalFile(url, localFilePath);
    }


    //读取到文件
    public static void generateLocalFile(String url, String localFilePath) {
        try (ReadableByteChannel channel = Channels.newChannel(new URL(url).openStream());
             FileOutputStream outputStream = new FileOutputStream(localFilePath)) {

            ByteBuffer buffer = ByteBuffer.allocate(1024 * 1024);  // 1MB 缓冲区
            int bytesRead;

            while ((bytesRead = channel.read(buffer))!= -1) {
                buffer.flip();
                outputStream.write(buffer.array(), 0, bytesRead);
                buffer.clear();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }



}
