package com.geekzhang.worktest.workutil;

import com.geekzhang.worktest.workutil.dto.HeartbeatMsg;
import com.google.common.collect.Lists;

import java.util.Comparator;

/**
 * @author zwm
 * @desc StreamTest
 * @date 2023年09月27日 13:51
 */
public class StreamTest {
    static final int MAXIMUM_CAPACITY = 1 << 30;
    public static void main(String[] args) {

        HeartbeatMsg paramProd = new HeartbeatMsg();
        paramProd.setCurrent(1);

        HeartbeatMsg heartbeatMsg1 = new HeartbeatMsg();
        heartbeatMsg1.setCurrent(1);
        HeartbeatMsg heartbeatMsg2 = new HeartbeatMsg();
        heartbeatMsg2.setCurrent(1);
        HeartbeatMsg heartbeatMsg3 = new HeartbeatMsg();
        heartbeatMsg3.setCurrent(1);
        Lists.newArrayList(heartbeatMsg1, heartbeatMsg2, heartbeatMsg3).stream().max(Comparator.comparing(HeartbeatMsg::getCurrent)).ifPresent(e -> {
            System.out.println("max:" + e.getCurrent());
        });

        HeartbeatMsg  repeatName = Lists.newArrayList(heartbeatMsg1, heartbeatMsg2, heartbeatMsg3).stream().filter(ele->ele.getCurrent()!=paramProd.getCurrent()).findFirst().orElse(null);



        int num = tableSizeFor(1);
        System.out.println("num:" + num);
    }

    public static  int tableSizeFor(int cap) {
        int n = cap - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
    }
}
