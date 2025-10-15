package com.geekzhang.mybatisplus.config;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * RestTemplate配置类
 * 提供HTTP客户端配置，包括连接池、超时设置等
 *
 * @author geekzhang
 * @since 2025-09-28
 */
@Configuration
public class RestTemplateConfig {

    /**
     * 创建默认的RestTemplate Bean
     * 使用RestTemplateBuilder进行配置
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(10))  // 连接超时10秒
                .setReadTimeout(Duration.ofSeconds(30))     // 读取超时30秒
                .build();
    }

    /**
     * 创建带连接池的RestTemplate Bean
     * 适用于高并发场景
     */
    @Bean("pooledRestTemplate")
    public RestTemplate pooledRestTemplate() {
        // 创建HttpClient连接池
        PoolingHttpClientConnectionManager connectionManager = createConnectionManager();
        
        // 创建RequestConfig
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(10))           // 连接超时10秒
                .setResponseTimeout(Timeout.ofSeconds(30))          // 响应超时30秒
                .setConnectionRequestTimeout(Timeout.ofSeconds(5))  // 从连接池获取连接超时5秒
                .build();

        // 创建HttpClient
        HttpClient httpClient = HttpClientBuilder.create()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build();

        // 创建RestTemplate
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory(httpClient));
        
        // 设置字符编码
        restTemplate.getMessageConverters()
                .stream()
                .filter(converter -> converter instanceof StringHttpMessageConverter)
                .forEach(converter -> ((StringHttpMessageConverter) converter)
                        .setDefaultCharset(StandardCharsets.UTF_8));

        return restTemplate;
    }

    /**
     * 创建连接池管理器
     */
    private PoolingHttpClientConnectionManager createConnectionManager() {
        Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", SSLConnectionSocketFactory.getSocketFactory())
                .build();

        PoolingHttpClientConnectionManager connectionManager = 
                new PoolingHttpClientConnectionManager(registry);
        
        // 设置连接池参数
        connectionManager.setMaxTotal(200);                // 最大连接数
        connectionManager.setDefaultMaxPerRoute(50);       // 每个路由的最大连接数
        connectionManager.setValidateAfterInactivity(Timeout.ofSeconds(2)); // 连接不活跃多久后验证

        return connectionManager;
    }

    /**
     * 创建用于文件上传的RestTemplate
     */
    @Bean("fileUploadRestTemplate")
    public RestTemplate fileUploadRestTemplate() {
        // 创建RequestConfig
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(30))     // 连接超时30秒
                .setResponseTimeout(Timeout.ofMinutes(5))     // 响应超时5分钟
                .build();

        // 创建HttpClient
        HttpClient httpClient = HttpClientBuilder.create()
                .setDefaultRequestConfig(requestConfig)
                .build();
        
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory(httpClient));
        
        return restTemplate;
    }
}
