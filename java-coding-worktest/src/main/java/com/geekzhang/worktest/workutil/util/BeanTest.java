package com.geekzhang.worktest.workutil.util;

import com.geekzhang.worktest.workutil.dto.Device;
import com.geekzhang.worktest.workutil.dto.DeviceUp;

/**
 * @author zwm
 * @desc BeanTest
 * @date 2024年04月09日 14:38
 */
public class BeanTest {
    public static void main(String[] args) {
        Device device = new Device();
        device.setType("qweqwe");


        DeviceUp deviceUp = device;

        System.out.println("qweqweqw");
    }
}
