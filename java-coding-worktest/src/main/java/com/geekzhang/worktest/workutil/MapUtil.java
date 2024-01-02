package com.geekzhang.worktest.workutil;


import com.alibaba.fastjson2.JSON;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author zwm
 * @desc MapUtil
 * @date 2023年09月20日 20:23
 */
public class MapUtil {
    private static final Set<String> modules = new HashSet(8) {{
        add("HC");
        add("HR");
        add("HT");
    }};
    public static void main(String[] args) throws ParseException {
        Map<String, String> map1 = new HashMap<>();
        map1.put("productId", "1");
        map1.put("vpn", "3");
        map1.put("type", "2");


        List<String> kvs = new ArrayList<>();
        for (Map.Entry<String, String> entry : map1.entrySet()) {
            String key = StringUtils.trim(entry.getKey());
            String value = StringUtils.trim(entry.getValue());
            if ("ts".equalsIgnoreCase(key)
                    || "sn".equalsIgnoreCase(key) // 理论上sn是禁止在接口参数中传过来的
                    || "sign".equalsIgnoreCase(key)
                    || StringUtils.isBlank(key)
                    || StringUtils.isBlank(value)) continue;

            kvs.add(key + ":" + value);
        }
        Collections.sort(kvs);
        kvs.add("ts:" + "123123");
        kvs.add("sn:" + StringUtils.trim("123122123123123"));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        List<String> trackItems = Lists.newArrayList("2023-11-30T04:55:55Z", "2023-11-30T04:55:56Z", "2023-11-30T04:55:57Z");
        Collections.sort(trackItems, (r1, r2) ->  r2.compareTo(r1));


        System.out.printf(JSON.toJSONString(trackItems));

    }
}
