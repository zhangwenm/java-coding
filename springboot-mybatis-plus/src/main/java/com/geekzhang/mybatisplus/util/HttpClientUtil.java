package com.geekzhang.mybatisplus.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.util.Map;

/**
 * HTTP客户端工具类
 * 封装RestTemplate的常用操作
 *
 * @author geekzhang
 * @since 2025-09-28
 */
@Slf4j
@Component
public class HttpClientUtil {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    @Qualifier("pooledRestTemplate")
    private RestTemplate pooledRestTemplate;

    @Autowired
    @Qualifier("fileUploadRestTemplate")
    private RestTemplate fileUploadRestTemplate;

    /**
     * GET请求
     */
    public <T> ResponseEntity<T> get(String url, Class<T> responseType) {
        return get(url, null, responseType);
    }

    /**
     * GET请求带参数
     */
    public <T> ResponseEntity<T> get(String url, Map<String, String> headers, Class<T> responseType) {
        try {
            HttpHeaders httpHeaders = createHeaders(headers);
            HttpEntity<?> entity = new HttpEntity<>(httpHeaders);
            
            log.info("发送GET请求: {}", url);
            ResponseEntity<T> response = restTemplate.exchange(url, HttpMethod.GET, entity, responseType);
            log.info("GET请求响应状态: {}", response.getStatusCode());
            
            return response;
        } catch (RestClientException e) {
            log.error("GET请求失败: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * POST请求发送JSON数据
     */
    public <T> ResponseEntity<T> postJson(String url, Object requestBody, Class<T> responseType) {
        return postJson(url, requestBody, null, responseType);
    }

    /**
     * POST请求发送JSON数据带自定义头部
     */
    public <T> ResponseEntity<T> postJson(String url, Object requestBody, Map<String, String> headers, Class<T> responseType) {
        try {
            HttpHeaders httpHeaders = createHeaders(headers);
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Object> entity = new HttpEntity<>(requestBody, httpHeaders);
            
            log.info("发送POST JSON请求: {}", url);
            ResponseEntity<T> response = pooledRestTemplate.postForEntity(url, entity, responseType);
            log.info("POST请求响应状态: {}", response.getStatusCode());
            
            return response;
        } catch (RestClientException e) {
            log.error("POST JSON请求失败: {}", e.getMessage());
            throw e;
        }
    }
    public <T> ResponseEntity<T> putJson(String url, String requestBody, Map<String, String> headers, Class<T> responseType) {
        try {
            HttpHeaders httpHeaders = createHeaders(headers);
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Object> entity = new HttpEntity<>(requestBody, httpHeaders);

            log.info("发送PUT JSON请求: {}", url);
            ResponseEntity<T> response = pooledRestTemplate.exchange(url, HttpMethod.PUT, entity, responseType);
            log.info("PUT请求响应状态: {}", response.getStatusCode());

            return response;
        } catch (RestClientException e) {
            log.error("PUT JSON请求失败: {}", e.getMessage());
            throw e;
        }
    }
    /**
     * POST请求发送表单数据
     */
    public <T> ResponseEntity<T> postForm(String url, MultiValueMap<String, Object> formData, Class<T> responseType) {
        return postForm(url, formData, null, responseType);
    }

    /**
     * POST请求发送表单数据带自定义头部
     */
    public <T> ResponseEntity<T> postForm(String url, MultiValueMap<String, Object> formData, Map<String, String> headers, Class<T> responseType) {
        try {
            HttpHeaders httpHeaders = createHeaders(headers);
            httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(formData, httpHeaders);
            
            log.info("发送POST表单请求: {}", url);
            ResponseEntity<T> response = restTemplate.postForEntity(url, entity, responseType);
            log.info("POST表单请求响应状态: {}", response.getStatusCode());
            
            return response;
        } catch (RestClientException e) {
            log.error("POST表单请求失败: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 文件上传
     */
    public <T> ResponseEntity<T> uploadFile(String url, File file, String fileParamName, Class<T> responseType) {
        return uploadFile(url, file, fileParamName, null, null, responseType);
    }

    /**
     * 文件上传带额外参数
     */
    public <T> ResponseEntity<T> uploadFile(String url, File file, String fileParamName, 
                                          Map<String, Object> additionalParams, Map<String, String> headers, Class<T> responseType) {
        try {
            HttpHeaders httpHeaders = createHeaders(headers);
            httpHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
            
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add(fileParamName, new FileSystemResource(file));
            
            // 添加额外参数
            if (additionalParams != null) {
                additionalParams.forEach(body::add);
            }
            
            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, httpHeaders);
            
            log.info("上传文件: {} 到 {}", file.getName(), url);
            ResponseEntity<T> response = fileUploadRestTemplate.postForEntity(url, entity, responseType);
            log.info("文件上传响应状态: {}", response.getStatusCode());
            
            return response;
        } catch (RestClientException e) {
            log.error("文件上传失败: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * PUT请求
     */
    public <T> ResponseEntity<T> put(String url, Object requestBody, Class<T> responseType) {
        return put(url, requestBody, null, responseType);
    }

    /**
     * PUT请求带自定义头部
     */
    public <T> ResponseEntity<T> put(String url, Object requestBody, Map<String, String> headers, Class<T> responseType) {
        try {
            HttpHeaders httpHeaders = createHeaders(headers);
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Object> entity = new HttpEntity<>(requestBody, httpHeaders);
            
            log.info("发送PUT请求: {}", url);
            ResponseEntity<T> response = restTemplate.exchange(url, HttpMethod.PUT, entity, responseType);
            log.info("PUT请求响应状态: {}", response.getStatusCode());
            
            return response;
        } catch (RestClientException e) {
            log.error("PUT请求失败: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * DELETE请求
     */
    public <T> ResponseEntity<T> delete(String url, Class<T> responseType) {
        return delete(url, null, responseType);
    }

    /**
     * DELETE请求带自定义头部
     */
    public <T> ResponseEntity<T> delete(String url, Map<String, String> headers, Class<T> responseType) {
        try {
            HttpHeaders httpHeaders = createHeaders(headers);
            HttpEntity<?> entity = new HttpEntity<>(httpHeaders);
            
            log.info("发送DELETE请求: {}", url);
            ResponseEntity<T> response = restTemplate.exchange(url, HttpMethod.DELETE, entity, responseType);
            log.info("DELETE请求响应状态: {}", response.getStatusCode());
            
            return response;
        } catch (RestClientException e) {
            log.error("DELETE请求失败: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 创建HTTP头部
     */
    private HttpHeaders createHeaders(Map<String, String> headers) {
        HttpHeaders httpHeaders = new HttpHeaders();
        
        if (headers != null) {
            headers.forEach(httpHeaders::set);
        }
        
        return httpHeaders;
    }
}
