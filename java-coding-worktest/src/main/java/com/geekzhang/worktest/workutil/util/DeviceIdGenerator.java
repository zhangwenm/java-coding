package com.geekzhang.worktest.workutil.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.*;
@Slf4j
public class DeviceIdGenerator {

    // 设备类型映射
    private static final Map<Integer, String> typePrefix = new LinkedHashMap<>();
    static {
        typePrefix.put(1, "X1");
        typePrefix.put(2, "X1LTE");
        typePrefix.put(3, "X4");
        typePrefix.put(4, "K3");
        typePrefix.put(5, "X1S");
        typePrefix.put(6, "X1SLTE");
        typePrefix.put(7, "S1");
        typePrefix.put(8, "X2");
        typePrefix.put(9, "L4");
        typePrefix.put(10, "L4LTE");
        typePrefix.put(9, "L4P");
        typePrefix.put(10, "L4PLTE");
        typePrefix.put(11, "X5");
        typePrefix.put(12, "N1");
        typePrefix.put(13, "N1LTE");
        typePrefix.put(14, "N4");
        typePrefix.put(15, "N4LTE");
        typePrefix.put(16, "R1");
        typePrefix.put(17, "M1");
        typePrefix.put(18, "L7");
        typePrefix.put(19, "L7LTE");
        typePrefix.put(20, "X3");
        typePrefix.put(21, "M1LTE");
        typePrefix.put(22, "L2");
        typePrefix.put(23, "L2LTE");
        typePrefix.put(24, "X2LTE");
        typePrefix.put(25, "L2P");
        typePrefix.put(26, "L7P");
        typePrefix.put(27, "L2PLTE");
        typePrefix.put(28, "L5");
    }

    /** 反向映射：前缀 -> 类型值 */
    private static final Map<String, Integer> prefixToType = new HashMap<>();
    static {
        for (Map.Entry<Integer, String> entry : typePrefix.entrySet()) {
            prefixToType.put(entry.getValue(), entry.getKey());
        }
    }

    /** 主要算法：生成设备 ID */
    public static long getHCAYIdV3(int typeVal, int series, int batch, int number) {
        return ((1L << 25) ^ ((long) typeVal << 20))
                | ((long) series << 15)
                | ((long) batch << 10)
                | number;
    }

    /** 批量生成设备 ID */
    public static List<Long> generateDeviceIds(String typeName, int series, int batch, int startNum, int numDevices) {
        if (!prefixToType.containsKey(typeName)) {
            throw new IllegalArgumentException("未知设备类型: " + typeName);
        }
        int typeVal = prefixToType.get(typeName);

        List<Long> result = new ArrayList<>();
        for (int i = 0; i < numDevices; i++) {
            int number = startNum + i;
            result.add(getHCAYIdV3(typeVal, series, batch, number));
        }
        return result;
    }

    /** 批量生成设备昵称 */
    public static List<String> generateNicknames(String typeName, int series, int batch, int startNum, int numDevices) {
        List<String> names = new ArrayList<>();
        for (int i = 0; i < numDevices; i++) {
            int number = startNum + i;
            String nickname = String.format(
                    "%s_S%02dB%02dN%03d",
                    typeName, series, batch, number
            );
            names.add(nickname);
        }
        return names;
    }

    public static void main(String[] args) {
        // 示例
        String type = "X1";
        int series = 1;//“S”后面的数字，如01写1
        int batch = 5;//“B”后面的数字，如01写1
        int start = 734;//“N”后面的数字，如005写5
        int num = 1;//不用动，控制生成的个数

        System.out.println("设备ID:");
        generateDeviceIds(type, series, batch, start, num).forEach(System.out::println);

        System.out.println("\n设备昵称:");
        generateNicknames(type, series, batch, start, num).forEach(System.out::println);
    }


    public static String generateSign(Map<String, String> args) {
        long timestamp = System.currentTimeMillis();
        args.put("timestamp", String.valueOf(timestamp));
        log.info("Timestamp should be {}", timestamp);
        String secret = "9f9781f1eaabf364dbcd0b0630d90ab6da14e38ed92e5230";
        // 按 key 排序，拼接 key=value
        List<String> strPart = new ArrayList<>();
        List<String> sortedKeys = new ArrayList<>(args.keySet());
        Collections.sort(sortedKeys);
        for (String key : sortedKeys) {
            strPart.add(key + "=" + args.get(key));
        }
        strPart.add("secret=" + secret);
        String reqString = String.join("&", strPart);
        String rightSign = DigestUtils.md5Hex(reqString.getBytes(StandardCharsets.UTF_8)).toUpperCase();
        log.info("Sign should be {}", rightSign);
        return rightSign;
    }
}

