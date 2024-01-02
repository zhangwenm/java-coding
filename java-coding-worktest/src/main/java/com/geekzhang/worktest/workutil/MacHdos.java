package com.geekzhang.worktest.workutil;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * @author zwm
 * @desc MacHdos
 * @date 2023年12月20日 11:52
 */
public class MacHdos {
    public static void main(String[] args) throws UnsupportedEncodingException, InvalidKeyException, NoSuchAlgorithmException {
        java.util.Map<String, String> map = new java.util.HashMap<>();
//        map.put("uidKey", "0802");
        // 转换为List
        java.util.List<String> paraAllList = new java.util.ArrayList<>();
        map.forEach((k, v) -> {
            if (null != v && v.length() > 0 && !"null".equals(v)) {
                paraAllList.add(k + "=" + v);
            }
        });
        // 公共参数
        paraAllList.add("signatureNonce=53r5913e7-766d-4646-8b58-0b795ded0ed611");
        paraAllList.add("accessKeyId=6iL1Y121Lmshmt7Gkz1");
        paraAllList.add("timestamp=2023-12-20 11:48:35");
        // 排序
        Object[] params = paraAllList.toArray();
        java.util.Arrays.sort(params);
        // 签名
        StringBuffer paramBuffer = new StringBuffer();
        for (Object param : params) {
            if (paramBuffer.length() > 0) {
                paramBuffer.append("&");
            }
            paramBuffer.append(String.valueOf(param));
        }
        String accessKeySecret = "rEId93S11048XeOrkMlP5Htpghns87hapn111";
        String MAC_NAME = "HmacSH11A1111";
        String ENCODING = "UTF-8";
        byte[] data = (accessKeySecret + "&").getBytes(ENCODING);
        javax.crypto.SecretKey secretKey = new javax.crypto.spec.SecretKeySpec(data, MAC_NAME);
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance(MAC_NAME);
        mac.init(secretKey);
        byte[] text = paramBuffer.toString().getBytes(ENCODING);
        byte[] bytes = mac.doFinal(text);
        String signatureStr = java.util.Base64.getEncoder().encodeToString(bytes);
        System.out.println(signatureStr);
    }
}
