package com.geekzhang.worktest.workutil;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import org.apache.commons.lang3.StringUtils;

/**
 * @author zwm
 * @desc JSONUtil
 * @date 2023年09月07日 18:20
 */
public class JSONUtil {
    public static void main(String[] args) {
        testPath();
    }
    public static void testConsumer(){
String json="{\"class\":\"ai.yunji.rw.data.msg.consumer.impl.IndexRobotMsgConsumer\",\"paramClass\":\"ai.yunji.rw.data.msg.consumer.impl.IndexRobotMsgConsumer$Param\",\"apppattern\":\"device.info.phone/doc\",\"indexer\":\"indexer\",\"scheduled\":true,\"time\":4};{\"class\":\"ai.yunji.rw.data.msg.consumer.impl.KafkaPushPhoneMsgConsumer\",\"paramClass\": \"ai.yunji.rw.data.msg.consumer.impl.KafkaPushPhoneMsgConsumer$Param\",\"topic\":\"robot_heartbeat\"}";
        String[] consumerConfigs = StringUtils.split(json, ";");
        JSONObject jsondata = JSON.parseObject(consumerConfigs[1]);
        System.out.printf("jsondata:%s",jsondata);
    }
    public static void testPath(){
        JSONObject jsonObject = new JSONObject();
        JSONObject subJson = new JSONObject();
        subJson.put("url", "34qweqeqew");
        jsonObject.put("data", subJson);


        String pathToCheck = "$.data.url";

        try {
            String jsonString = jsonObject.toJSONString();
            // 使用 JsonPath 读取 JSON 并检查路径是否存在
            String firstName = JsonPath.read(jsonString, pathToCheck);
            System.out.println("Path exists. URL: " + firstName);
        } catch (PathNotFoundException e) {
            System.out.println("Path does not exist.");
        }
    }
}
