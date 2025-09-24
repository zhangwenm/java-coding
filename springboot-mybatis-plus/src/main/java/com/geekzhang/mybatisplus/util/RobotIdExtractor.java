package com.geekzhang.mybatisplus.util;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author zwm
 * @desc RobotIdExtractor
 * @date 2025年09月24日 15:12
 */
public class RobotIdExtractor {
    public static String extractRobotId(String logMessage) {
        // 正则表达式匹配 robotId: 后面的值
        Pattern pattern = Pattern.compile("robotId:([^,\\s]+)");
        Matcher matcher = pattern.matcher(logMessage);

        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    public static void main(String[] args) {


        InputStream resourceAsStream2 = Thread.currentThread().getContextClassLoader().getResourceAsStream("robot.json");
        JSONArray info = JSON.parseObject(resourceAsStream2 , JSONArray.class);
        System.out.println(info.size());
        List<String> robotIdhas = new ArrayList<>();
        List<String> robotIderror = new ArrayList<>();

        info.forEach(item -> {
            JSONObject jsonObject = (JSONObject) item;

            JSONObject data = jsonObject.getJSONObject("_source");

             String message = data.getString("message");


            String robotId = extractRobotId(message);

            if(message.contains("市行政区域信息有误")){
                robotIderror.add(robotId);
            }
            if(message.contains("行政区域不存在")){
                robotIdhas.add(robotId);
            }
        });

        System.out.println(robotIdhas.size());
        System.out.println(JSON.toJSONString(robotIdhas));
        System.out.println("===================================");
        System.out.println(robotIderror.size());
        System.out.println(JSON.toJSONString(robotIderror));

    }
}
