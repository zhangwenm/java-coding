package com.geekzhang.worktest.workutil;

import com.alibaba.excel.EasyExcel;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.geekzhang.worktest.workutil.dto.PlaceInfo;
import com.google.common.collect.Sets;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

/**
 * @author zwm
 * @desc ExcelTest
 * @date 2024年01月18日 10:40
 */
public class DataTest {

    public static void main(String[] args) throws IOException {
        InputStream resourceAsStream0 = Thread.currentThread().getContextClassLoader().getResourceAsStream("Store.json");
        JSONObject deviceList = JSON.parseObject(resourceAsStream0 , JSONObject.class);

        JSONArray store = deviceList.getJSONArray("data");

        JSONObject jsonObject = store.getJSONObject(0);

        JSONArray stores = jsonObject.getJSONArray("storeList");

        String res = jsonObject.getString("storeId");

        String[] split = res.split(",");

        Set<String> set = Sets.newHashSet(split);
        StringBuffer sb = new StringBuffer();
        for (String s : set) {
            sb.append("'"+s+"'").append(",");
        }
        System.out.println();



    }
}
