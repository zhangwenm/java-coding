package com.geekzhang.worktest.workutil;

import org.apache.commons.lang3.StringUtils;

/**
 * @author zwm
 * @desc MapUtil
 * @date 2023年09月20日 20:23
 */
public class StringTestUtil {

    public static void main(String[] args) {
        String str = "WTHT1234";
        System.out.println("res:"+ StringUtils.substring(str,  str.length()-4, str.length()));
    }
}
