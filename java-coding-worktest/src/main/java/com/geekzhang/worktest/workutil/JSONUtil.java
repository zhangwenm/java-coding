package com.geekzhang.worktest.workutil;

import com.alibaba.fastjson.JSONObject;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

/**
 * @author zwm
 * @desc JSONUtil
 * @date 2023年09月07日 18:20
 */
public class JSONUtil {
    public static void main(String[] args) {
        JSONObject jsonObject = new JSONObject();
        JSONObject subJson = new JSONObject();
        subJson.put("keySub", 1);
        jsonObject.put("low", 1);
        jsonObject.put("high", 2);
        System.out.println(jsonObject.toJSONString());
        jsonObject.put("command", "phoneStatus");
        jsonObject.put("sub", subJson);


        String pathToCheck = "$.sub.keySub";

        try {
            String jsonString = jsonObject.toJSONString();
            // 使用 JsonPath 读取 JSON 并检查路径是否存在
            Integer firstName = JsonPath.read(jsonString, pathToCheck);
            System.out.println("Path exists. Value: " + firstName);
        } catch (PathNotFoundException e) {
            System.out.println("Path does not exist.");
        }
    }
}
