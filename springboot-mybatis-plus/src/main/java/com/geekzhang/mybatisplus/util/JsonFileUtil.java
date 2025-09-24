package com.geekzhang.mybatisplus.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * JSON文件操作工具类
 *
 * @author geekzhang
 * @since 2025-09-24
 */
@Slf4j
public class JsonFileUtil {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();

    static {
        // 配置Jackson
        OBJECT_MAPPER.registerModule(new JavaTimeModule());
        OBJECT_MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        OBJECT_MAPPER.enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * 使用Jackson将List写入JSON文件
     *
     * @param list     要写入的集合
     * @param filePath 文件路径
     * @param <T>      集合元素类型
     * @return 是否成功
     */
    public static <T> boolean writeListToJsonWithJackson(List<T> list, String filePath) {
        try {
            File file = new File(filePath);
            // 确保父目录存在
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            OBJECT_MAPPER.writeValue(file, list);
            log.info("使用Jackson成功写入JSON文件：{}, 数据量：{}", filePath, list.size());
            return true;
        } catch (IOException e) {
            log.error("使用Jackson写入JSON文件失败：{}", filePath, e);
            return false;
        }
    }

    /**
     * 使用FastJSON2将List写入JSON文件
     *
     * @param list     要写入的集合
     * @param filePath 文件路径
     * @param <T>      集合元素类型
     * @return 是否成功
     */
    public static <T> boolean writeListToJsonWithFastJson(List<T> list, String filePath) {
        try {
            Path path = Paths.get(filePath);
            // 确保父目录存在
            Files.createDirectories(path.getParent());

            String jsonString = JSON.toJSONString(list, JSONWriter.Feature.PrettyFormat);
            Files.write(path, jsonString.getBytes(StandardCharsets.UTF_8));
            
            log.info("使用FastJSON成功写入JSON文件：{}, 数据量：{}", filePath, list.size());
            return true;
        } catch (IOException e) {
            log.error("使用FastJSON写入JSON文件失败：{}", filePath, e);
            return false;
        }
    }

    /**
     * 使用Gson将List写入JSON文件
     *
     * @param list     要写入的集合
     * @param filePath 文件路径
     * @param <T>      集合元素类型
     * @return 是否成功
     */
    public static <T> boolean writeListToJsonWithGson(List<T> list, String filePath) {
        try {
            File file = new File(filePath);
            // 确保父目录存在
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(list, writer);
            }
            
            log.info("使用Gson成功写入JSON文件：{}, 数据量：{}", filePath, list.size());
            return true;
        } catch (IOException e) {
            log.error("使用Gson写入JSON文件失败：{}", filePath, e);
            return false;
        }
    }

    /**
     * 生成带时间戳的文件名
     *
     * @param baseName 基础文件名
     * @param suffix   文件后缀
     * @return 带时间戳的文件名
     */
    public static String generateTimestampFileName(String baseName, String suffix) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return String.format("%s_%s.%s", baseName, timestamp, suffix);
    }

    /**
     * 从JSON文件读取List集合（Jackson）
     *
     * @param filePath 文件路径
     * @param clazz    集合元素类型
     * @param <T>      集合元素类型
     * @return 读取的集合
     */
    public static <T> List<T> readListFromJsonWithJackson(String filePath, Class<T> clazz) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                log.warn("JSON文件不存在：{}", filePath);
                return List.of();
            }

            return OBJECT_MAPPER.readValue(file, 
                OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (IOException e) {
            log.error("使用Jackson读取JSON文件失败：{}", filePath, e);
            return List.of();
        }
    }

    /**
     * 从JSON文件读取List集合（FastJSON）
     *
     * @param filePath 文件路径
     * @param clazz    集合元素类型
     * @param <T>      集合元素类型
     * @return 读取的集合
     */
    public static <T> List<T> readListFromJsonWithFastJson(String filePath, Class<T> clazz) {
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                log.warn("JSON文件不存在：{}", filePath);
                return List.of();
            }

            String jsonString = Files.readString(path, StandardCharsets.UTF_8);
            return JSON.parseArray(jsonString, clazz);
        } catch (IOException e) {
            log.error("使用FastJSON读取JSON文件失败：{}", filePath, e);
            return List.of();
        }
    }
}
