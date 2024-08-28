package com.ksyun.campus.client.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class testRead {
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        testmd5();
        testread();
    }
    private static void testread() throws IOException {
        InputStream is = null;
        byte[] buffer=new byte[13];
        char c;

        try{
            // 文本内容为 ABCDE
            is = new FileInputStream("D:\\Java\\course-code\\minfs\\easyClient\\src\\main\\java\\com\\ksyun\\campus\\client\\util\\a.txt");
            // 这个2指的是数据偏移 不是流偏移 将读取的字节添加到字节数组buffer中
            is.read(buffer);
            // 循环字节数据
            for(byte b:buffer){
                if(b==0)
                    // 因为偏移了2个单位,又因为数组默认值为0,所以前面2个单位是没有的
                    c=' ';
                else
                    // 这里是读到的字节
                    c=(char)b;
                System.out.print(c);

            }
            String newdata=new String(buffer,0,13);
            System.out.println(newdata);
            System.out.println(calculateMD5(newdata.getBytes(StandardCharsets.UTF_8)));
        }catch(Exception e){
            e.printStackTrace();
        }finally{
            if(is!=null)
                is.close();
        }

    }

    public static void testmd5() throws NoSuchAlgorithmException {
        String md5 = null;
        String data="sssssssssssss";
        byte[] input = data.getBytes(StandardCharsets.UTF_8);
        md5=calculateMD5(input);
        System.out.println(md5);
    }
    private static String calculateMD5(byte[] content) throws NoSuchAlgorithmException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(content);
        byte[] digest = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
