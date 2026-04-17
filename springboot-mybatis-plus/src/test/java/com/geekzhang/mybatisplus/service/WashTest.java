package com.geekzhang.mybatisplus.service;


import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.geekzhang.mybatisplus.entity.MeituanXiaodaiInfo;
import com.geekzhang.mybatisplus.mapper.MeituanXiaodaiInfoMapper;
import com.geekzhang.mybatisplus.util.HttpClientUtil;
import com.geekzhang.mybatisplus.vo.MeituanStoreInfoRegister;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author zwm
 * @desc WashTest
 * @date 2025年10月29日 09:40
 */
@Slf4j
@SpringBootTest
public class WashTest {
    @Autowired
    private HttpClientUtil httpClientUtil;

    @Test
    public void deviceStateLGTest() {
        statusLG("");
    }
    private void statusLG(String type) {
        String deviceId = "cecf89b0b51e08d391b2d76697a334c97091d3a8848e0a72f7dcb36bf6f7d6fe";

        if("HG".equals(type)){
            deviceId = "4ffa41665016eb21b4bbc62b90b722a0b2365c646c2cf1401d98d56987c75e73";
        }

        try {
            String url = String.format("https://kic-laundry-ext-qa.lgthinq.com.cn/devices/%s", deviceId);
            String resp = httpClientUtil.get(url, buildHeaders(), String.class).getBody();
            log.info("start startLG: type:{}, resp:{}", resp);
        } catch (Exception e) {
            log.error("fail to start startLG: {}", e.getMessage());
        }
    }
    @Test
    public void startLGTest() {
        startLG("HG");
    }

    private void startLG(String type) {
        String deviceId = "cecf89b0b51e08d391b2d76697a334c97091d3a8848e0a72f7dcb36bf6f7d6fe";

        JSONObject json = null;

        if("HG".equals(type)){
            deviceId = "4ffa41665016eb21b4bbc62b90b722a0b2365c646c2cf1401d98d56987c75e73";
            json = new JSONObject();
            JSONObject subJson = new JSONObject();
            // 30 68 HI_TEMP 30 60 MED_TEMP 30 52 LOW_TEMP 30 COOLING
            subJson.put("course", "COOLING");
            json.put("request", subJson);

        }else{
            json = new JSONObject();
            JSONObject subJson = new JSONObject();
            // 1：29 Hot 36 Warm 36 Cold
            subJson.put("course", "Cold");
            json.put("request", subJson);
        }

        try {
            String url = String.format("https://kic-laundry-ext-qa.lgthinq.com.cn/devices/%s/start", deviceId);
            String resp = httpClientUtil.postJson(url, json.toJSONString(),buildHeaders(), String.class).getBody();
            log.info("start startLG: type:{}, resp:{}", resp);
        } catch (Exception e) {
            log.error("fail to start startLG: {}", e.getMessage());
        }
    }

    @Test
    public void resetWashLGTest() {
        resetLG("");
    }
    private void resetLG(String type) {
        String deviceId = "cecf89b0b51e08d391b2d76697a334c97091d3a8848e0a72f7dcb36bf6f7d6fe";

        if("HG".equals(type)){
            deviceId = "4ffa41665016eb21b4bbc62b90b722a0b2365c646c2cf1401d98d56987c75e73";
        }

        try {
            String url = String.format("https://kic-laundry-ext-qa.lgthinq.com.cn/devices/%s/reset", deviceId);
            String resp = httpClientUtil.postJson(url, "",buildHeaders(), String.class).getBody();
            log.info("start resetWashLGTest:type:{}, resp:{}", resp);
        } catch (Exception e) {
            log.error("fail to start resetWashLGTest: {}", e.getMessage());
        }
    }
    @Test
    public void registerLGTest() {
        registerLG();
    }

    private void registerLG( ) {

        JSONObject json = new JSONObject();
        json.put("url", "http://172.16.15.84:8080/api/user/test");
        json.put("retryCount", 3);

        try {
            String url = "https://kic-laundry-ext-qa.lgthinq.com.cn/event/callback" ;
            String resp = httpClientUtil.put(url,json.toJSONString(), buildHeaders(), String.class).getBody();
            log.info("start registerLG:  resp:{}", resp);
        } catch (Exception e) {
            log.error("fail to start registerLG: {}", e.getMessage());
        }
    }
    @Test
    public void delRegisterLGTest() {
        delRegisterLG();
    }
    private void delRegisterLG( ) {

        try {
            String url = "https://kic-laundry-ext-qa.lgthinq.com.cn/event/callback" ;
            String resp = httpClientUtil.delete(url,  buildHeaders(), String.class).getBody();
            log.info("start delRegisterLG:  resp:{}", resp);
        } catch (Exception e) {
            log.error("fail to start delRegisterLG: {}", e.getMessage());
        }
    }
    private Map<String,String> buildHeaders() {
        Map<String,String> headers = new HashMap<>();
        headers.put("Content-Type", "application/x-www-form-urlencoded");
        headers.put("x-service-id", "44216bc7d937eed390ee2df0");
        headers.put("x-service-key", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzZXJ2aWNlSWQiOiI0NDIxNmJjN2Q5MzdlZWQzOTBlZTJkZjAiLCJ0b2tlblNlZWQiOiI1Zjc2MWQwMDU2YmE4NWJkIiwidGltZSI6MTc0NzI3MjY5NH0.kDBlisL3bqYfO_5FqMzeug-FSkheZTz61dshUG-nDug");
        headers.put("x-api-key", "vV6bStCpqr5Hqxbcr8Kmp9XkFh4VdlVp568YxBp5");
        headers.put("x-message-id", UUID.randomUUID().toString().replaceAll("-", ""));
        headers.put("x-country-code", "CN");
        headers.put("x-service-phase", "QA");
        headers.put("x-thinq-client-type", "SERVER");

        return headers;
    }

}
