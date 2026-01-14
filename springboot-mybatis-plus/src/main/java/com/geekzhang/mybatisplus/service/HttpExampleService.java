package com.geekzhang.mybatisplus.service;

import com.alibaba.fastjson2.JSONObject;
import com.geekzhang.mybatisplus.util.HttpClientUtil;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * HTTP请求示例服务
 * 演示如何使用HttpClientUtil进行各种HTTP请求
 *
 * @author geekzhang
 * @since 2025-09-28
 */
@Slf4j
@Service
public class HttpExampleService {

    @Autowired
    private HttpClientUtil httpClientUtil;
    /**
     * 使用HttpClientUtil实现的微信小程序登录 - 简化版本
     *
     * @param code 小程序端调用wx.login获取的code
     * @return Response结果
     */
    public Response decodeOpenidSimple(String code) {
        String appId = "wxf1c1ab450733c7ea";
        String secret = "190e861430df34673d29afc169de736c";

        try {
            // 构建GET请求URL参数
            String url = String.format(
                    "https://api.weixin.qq.com/sns/jscode2session?grant_type=authorization_code&js_code=%s&appid=%s&secret=%s",
                    code, appId, secret
            );

            log.info("发起微信小程序登录请求，code: {}", code);

            // 使用HttpClientUtil发送GET请求
            ResponseEntity<String> response = httpClientUtil.get(url, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("微信API调用失败，状态码: {}", response.getStatusCode());
                return Response.FAILED("微信API调用失败");
            }

            String res = response.getBody();
            log.info("微信API响应: {}", res);

            // 解析响应
            JSONObject json = JSONObject.parseObject(res);

            String openid = json.getString("openid");
            if (openid != null) {
                log.info("微信登录成功，openid: {}", openid);
                return Response.SUCCESS(openid);
            } else {
                // 检查是否有错误信息
                Integer errcode = json.getInteger("errcode");
                String errmsg = json.getString("errmsg");
                if (errcode != null) {
                    log.error("微信登录失败，errcode: {}, errmsg: {}", errcode, errmsg);
                    return Response.FAILED("微信登录失败: " + errmsg);
                }
                return Response.FAILED("无法获取openid");
            }

        } catch (Exception e) {
            log.error("微信登录异常: {}", e.getMessage(), e);
            return Response.FAILED("openId生成失败");
        }
    }

    /**
     * 通用响应类
     */
    public static class Response {
        private boolean success;
        private String message;
        private Object data;

        public Response() {}

        public Response(boolean success, String message, Object data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public static Response SUCCESS(Object data) {
            return new Response(true, "成功", data);
        }

        public static Response FAILED(String message) {
            return new Response(false, message, null);
        }

        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public Object getData() { return data; }
        public void setData(Object data) { this.data = data; }

        @Override
        public String toString() {
            return "Response{" +
                    "success=" + success +
                    ", message='" + message + '\'' +
                    ", data=" + data +
                    '}';
        }
    }
    /**
     * GET请求示例
     */
    public String getExample() {
        try {
            String url = "https://jsonplaceholder.typicode.com/posts/1";
            
            // 添加自定义头部
            Map<String, String> headers = new HashMap<>();
            headers.put("User-Agent", "MyApp/1.0");
            
            ResponseEntity<String> response = httpClientUtil.get(url, headers, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("GET请求成功，响应数据: {}", response.getBody());
                return response.getBody();
            } else {
                log.warn("GET请求失败，状态码: {}", response.getStatusCode());
                return null;
            }
        } catch (Exception e) {
            log.error("GET请求异常: {}", e.getMessage());
            return null;
        }
    }

    /**
     * POST JSON请求示例
     */
    public String postJsonExample() {
        try {
            String url = "https://jsonplaceholder.typicode.com/posts";
            
            // 创建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("title", "测试标题");
            requestBody.put("body", "测试内容");
            requestBody.put("userId", 1);
            
            // 添加自定义头部
            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Bearer your-token");
            
            ResponseEntity<String> response = httpClientUtil.postJson(url, requestBody, headers, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("POST JSON请求成功，响应数据: {}", response.getBody());
                return response.getBody();
            } else {
                log.warn("POST JSON请求失败，状态码: {}", response.getStatusCode());
                return null;
            }
        } catch (Exception e) {
            log.error("POST JSON请求异常: {}", e.getMessage());
            return null;
        }
    }

    /**
     * POST表单请求示例
     */
    public String postFormExample() {
        try {
            String url = "https://httpbin.org/post";
            
            // 创建表单数据
            MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
            formData.add("username", "testuser");
            formData.add("password", "testpass");
            formData.add("email", "test@example.com");
            
            ResponseEntity<String> response = httpClientUtil.postForm(url, formData, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("POST表单请求成功，响应数据: {}", response.getBody());
                return response.getBody();
            } else {
                log.warn("POST表单请求失败，状态码: {}", response.getStatusCode());
                return null;
            }
        } catch (Exception e) {
            log.error("POST表单请求异常: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 文件上传示例
     */
    public String uploadFileExample(String filePath) {
        try {
            String url = "https://httpbin.org/post";
            
            File file = new File(filePath);
            if (!file.exists()) {
                log.warn("文件不存在: {}", filePath);
                return null;
            }
            
            // 添加额外参数
            Map<String, Object> additionalParams = new HashMap<>();
            additionalParams.put("description", "测试文件上传");
            additionalParams.put("category", "document");
            
            // 添加自定义头部
            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Bearer your-token");
            
            ResponseEntity<String> response = httpClientUtil.uploadFile(
                url, file, "file", additionalParams, headers, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("文件上传成功，响应数据: {}", response.getBody());
                return response.getBody();
            } else {
                log.warn("文件上传失败，状态码: {}", response.getStatusCode());
                return null;
            }
        } catch (Exception e) {
            log.error("文件上传异常: {}", e.getMessage());
            return null;
        }
    }

    /**
     * PUT请求示例
     */
    public String putExample() {
        try {
            String url = "https://jsonplaceholder.typicode.com/posts/1";
            
            // 创建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("id", 1);
            requestBody.put("title", "更新的标题");
            requestBody.put("body", "更新的内容");
            requestBody.put("userId", 1);
            
            ResponseEntity<String> response = httpClientUtil.put(url, requestBody, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("PUT请求成功，响应数据: {}", response.getBody());
                return response.getBody();
            } else {
                log.warn("PUT请求失败，状态码: {}", response.getStatusCode());
                return null;
            }
        } catch (Exception e) {
            log.error("PUT请求异常: {}", e.getMessage());
            return null;
        }
    }

    /**
     * DELETE请求示例
     */
    public boolean deleteExample() {
        try {
            String url = "https://jsonplaceholder.typicode.com/posts/1";
            
            // 添加自定义头部
            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Bearer your-token");
            
            ResponseEntity<String> response = httpClientUtil.delete(url, headers, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("DELETE请求成功");
                return true;
            } else {
                log.warn("DELETE请求失败，状态码: {}", response.getStatusCode());
                return false;
            }
        } catch (Exception e) {
            log.error("DELETE请求异常: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 调用第三方API示例
     */
    public Map<String, Object> callThirdPartyApi(String apiUrl, Map<String, Object> params) {
        try {
            // 添加认证头部
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");
            headers.put("Accept", "application/json");
            
            ResponseEntity<Map> response = httpClientUtil.postJson(apiUrl, params, headers, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("第三方API调用成功");
                return response.getBody();
            } else {
                log.warn("第三方API调用失败，状态码: {}", response.getStatusCode());
                return null;
            }
        } catch (Exception e) {
            log.error("第三方API调用异常: {}", e.getMessage());
            return null;
        }
    }
}
