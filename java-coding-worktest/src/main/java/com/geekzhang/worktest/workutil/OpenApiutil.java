package com.geekzhang.worktest.workutil;



import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.geekzhang.worktest.workutil.dto.MethodDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author zwm
 * @desc UserDataListener
 * @date 2023年12月22日 14:31
 */
public class OpenApiutil   {


    private static void validateOpenSign(String appname, Long ts,String secret) {
        System.out.println("ts:"+ts);
        Map<String, String> params = new HashMap(){{
            put("productId", "GGGI0321410807923");
            put("floor","0");

        }};;
        List<String> kvs = new ArrayList<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = StringUtils.trim(entry.getKey());
            String value = StringUtils.trim(entry.getValue());
            if ("appname".equalsIgnoreCase(key)
                    || "secret".equalsIgnoreCase(key)
                    || "ts".equalsIgnoreCase(key)
                    || "sign".equalsIgnoreCase(key)
                    || StringUtils.isBlank(value))
                continue;

            kvs.add(key + ":" + value);
        }
        Collections.sort(kvs);

        kvs.add("appname:" + appname);
        kvs.add("secret:" + secret);
        kvs.add("ts:" + ts);
        String signRes = DigestUtils.md5Hex(StringUtils.join(kvs, "|").getBytes());
        System.out.println(" signRes:"+signRes);
    }
    public static void main(String[] args) {
        validateOpenSign("test",new Date().getTime(),"df556ef607b8b583baa5e8b6afc5a205");
    }


}
