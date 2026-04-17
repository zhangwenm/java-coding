package com.geekzhang.worktest.data;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.ContentRowHeight;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.Data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 数据对比工具类
 * 对比CSV和JSON数据，将CSV中在JSON中存在的数据标记并导出为Excel
 */
@Data
@ContentRowHeight(20)
@HeadRowHeight(30)
public class DataCompareTool {

    @ExcelProperty("id")
    @ColumnWidth(15)
    private String id;

    @ExcelProperty("place_id")
    @ColumnWidth(30)
    private String placeId;

    @ExcelProperty("type")
    @ColumnWidth(10)
    private String type;

    @ExcelProperty("product_id")
    @ColumnWidth(30)
    private String productId;

    @ExcelProperty("device_id")
    @ColumnWidth(30)
    private String deviceId;

    @ExcelProperty("keywords")
    @ColumnWidth(50)
    private String keywords;

    @ExcelProperty("lat")
    @ColumnWidth(15)
    private String lat;

    @ExcelProperty("lng")
    @ColumnWidth(15)
    private String lng;

    @ExcelProperty("status")
    @ColumnWidth(10)
    private String status;

    @ExcelProperty("create_time")
    @ColumnWidth(20)
    private String createTime;

    @ExcelProperty("update_time")
    @ColumnWidth(20)
    private String updateTime;

    @ExcelProperty("create_user")
    @ColumnWidth(25)
    private String createUser;

    @ExcelProperty("update_user")
    @ColumnWidth(25)
    private String updateUser;

    @ExcelProperty("在JSON中存在")
    @ColumnWidth(15)
    private String existsInJson;

    public static void main(String[] args) {
        // 文件路径
        String resourcesPath = "src/main/resources/";
        String csvPath = resourcesPath + "腾讯云DMC_数据导出_1769412599092.csv";
        String jsonPath = resourcesPath + "devicexiaodai.json";
        String outputPath = "数据对比结果.xlsx";

        System.out.println("=".repeat(60));
        System.out.println("开始对比CSV和JSON数据...");
        System.out.println("=".repeat(60));

        try {
            // 1. 读取JSON文件，提取所有productId
            System.out.println("\n📖 正在读取JSON文件...");
            Set<String> jsonProductIds = loadJsonData(jsonPath);
            System.out.println("   JSON文件共 " + jsonProductIds.size() + " 个不同的product_id");

            // 2. 读取CSV文件
            System.out.println("\n📖 正在读取CSV文件...");
            List<DataCompareTool> csvData = loadCsvData(csvPath, jsonProductIds);
            System.out.println("   CSV文件共 " + csvData.size() + " 条记录");

            // 3. 统计结果
            long existsCount = csvData.stream()
                    .filter(item -> "是".equals(item.getExistsInJson()))
                    .count();
            System.out.println("\n🔍 对比完成:");
            System.out.println("   CSV中在JSON中存在的记录: " + existsCount + " 条");
            System.out.println("   CSV中不在JSON中的记录: " + (csvData.size() - existsCount) + " 条");

            // 4. 导出Excel
            System.out.println("\n📊 正在导出Excel...");
            File outputFile = new File(outputPath);
            EasyExcel.write(outputPath, DataCompareTool.class)
                    .sheet("数据对比结果")
                    .doWrite(csvData);
            System.out.println("✅ 成功导出到: " + outputFile.getAbsolutePath());

            System.out.println("\n" + "=".repeat(60));
            System.out.println("✨ 处理完成!");
            System.out.println("=".repeat(60));

        } catch (Exception e) {
            System.err.println("❌ 处理失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 加载JSON数据，提取所有productId
     */
    private static Set<String> loadJsonData(String jsonPath) throws Exception {
        Set<String> productIds = new HashSet<>();
        File jsonFile = new File(jsonPath);

        if (!jsonFile.exists()) {
            throw new RuntimeException("JSON文件不存在: " + jsonPath);
        }

        String content = org.apache.commons.io.FileUtils.readFileToString(jsonFile, StandardCharsets.UTF_8);
        JSONArray jsonArray = JSONArray.parseArray(content);

        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject item = jsonArray.getJSONObject(i);
            if (item.containsKey("_source")) {
                JSONObject source = item.getJSONObject("_source");
                if (source.containsKey("productId")) {
                    productIds.add(source.getString("productId"));
                }
            }
        }

        return productIds;
    }

    /**
     * 加载CSV数据并标记
     */
    private static List<DataCompareTool> loadCsvData(String csvPath, Set<String> jsonProductIds) throws Exception {
        List<DataCompareTool> dataList = new ArrayList<>();
        File csvFile = new File(csvPath);

        if (!csvFile.exists()) {
            throw new RuntimeException("CSV文件不存在: " + csvPath);
        }

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(csvFile), StandardCharsets.UTF_8))) {

            String line;
            boolean isFirstLine = true;
            String[] headers = null;

            while ((line = br.readLine()) != null) {
                // 跳过BOM
                if (line.startsWith("\uFEFF")) {
                    line = line.substring(1);
                }

                String[] fields = line.split(",");

                if (isFirstLine) {
                    headers = fields;
                    isFirstLine = false;
                    continue;
                }

                // 解析CSV字段（处理带引号的字段）
                List<String> parsedFields = parseCsvFields(line);

                if (parsedFields.size() >= headers.length) {
                    DataCompareTool item = new DataCompareTool();
                    item.setId(parsedFields.get(0).replace("\"", "").trim());
                    item.setPlaceId(parsedFields.get(1).replace("\"", "").trim());
                    item.setType(parsedFields.get(2).replace("\"", "").trim());
                    item.setProductId(parsedFields.get(3).replace("\"", "").trim());
                    item.setDeviceId(parsedFields.get(4).replace("\"", "").trim());
                    item.setKeywords(parsedFields.get(5).replace("\"", "").trim());
                    item.setLat(parsedFields.get(6).replace("\"", "").trim());
                    item.setLng(parsedFields.get(7).replace("\"", "").trim());
                    item.setStatus(parsedFields.get(8).replace("\"", "").trim());
                    item.setCreateTime(parsedFields.get(9).replace("\"", "").trim());
                    item.setUpdateTime(parsedFields.get(10).replace("\"", "").trim());
                    item.setCreateUser(parsedFields.get(11).replace("\"", "").trim());
                    item.setUpdateUser(parsedFields.get(12).replace("\"", "").trim());

                    // 标记是否在JSON中存在
                    String deviceId = item.getDeviceId();
                    String productId = item.getProductId();
                    if (jsonProductIds.contains(deviceId) || jsonProductIds.contains(productId)) {
                        item.setExistsInJson("是");
                    } else {
                        item.setExistsInJson("否");
                    }

                    dataList.add(item);
                }
            }
        }

        return dataList;
    }

    /**
     * 解析CSV字段（处理带引号和逗号的字段）
     */
    private static List<String> parseCsvFields(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    // 转义的引号
                    currentField.append('"');
                    i++;
                } else {
                    // 切换引号状态
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                // 字段分隔符
                fields.add(currentField.toString());
                currentField = new StringBuilder();
            } else {
                currentField.append(c);
            }
        }

        // 添加最后一个字段
        fields.add(currentField.toString());

        return fields;
    }
}
