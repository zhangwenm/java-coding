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
import java.util.*;

/**
 * 数据对比工具类
 * 以JSON中的数据为主数据，检查是否在CSV中存在，并导出为Excel
 */
@Data
@ContentRowHeight(20)
@HeadRowHeight(30)
public class JsonCsvCompareTool {

    @ExcelProperty("_index")
    @ColumnWidth(25)
    private String index;

    @ExcelProperty("_type")
    @ColumnWidth(15)
    private String type;

    @ExcelProperty("_id")
    @ColumnWidth(30)
    private String id;

    @ExcelProperty("_score")
    @ColumnWidth(15)
    private String score;

    @ExcelProperty("productId")
    @ColumnWidth(30)
    private String productId;

    @ExcelProperty("provinceName")
    @ColumnWidth(15)
    private String provinceName;

    @ExcelProperty("cityName")
    @ColumnWidth(15)
    private String cityName;

    @ExcelProperty("type")
    @ColumnWidth(15)
    private String typeValue;

    @ExcelProperty("provinceId")
    @ColumnWidth(15)
    private String provinceId;

    @ExcelProperty("在CSV中存在")
    @ColumnWidth(15)
    private String existsInCsv;

    public static void main(String[] args) {
        // 文件路径
        String resourcesPath = "src/main/resources/";
        String csvPath = resourcesPath + "腾讯云DMC_数据导出_1769412599092.csv";
        String jsonPath = resourcesPath + "devicexiaodai.json";
        String outputPath = "JSON数据对比结果.xlsx";

        System.out.println("=".repeat(60));
        System.out.println("开始对比JSON和CSV数据（以JSON为主数据）...");
        System.out.println("=".repeat(60));

        try {
            // 1. 读取CSV文件，提取所有product_id和device_id
            System.out.println("\n📖 正在读取CSV文件...");
            Set<String> csvIds = loadCsvData(csvPath);
            System.out.println("   CSV文件共 " + csvIds.size() + " 个不同的product_id/device_id");

            // 2. 读取JSON文件并对比
            System.out.println("\n📖 正在读取JSON文件...");
            List<JsonCsvCompareTool> jsonData = loadJsonData(jsonPath, csvIds);
            System.out.println("   JSON文件共 " + jsonData.size() + " 条记录");

            // 3. 统计结果
            long existsCount = jsonData.stream()
                    .filter(item -> "是".equals(item.getExistsInCsv()))
                    .count();
            System.out.println("\n🔍 对比完成:");
            System.out.println("   JSON中在CSV中存在的记录: " + existsCount + " 条");
            System.out.println("   JSON中不在CSV中的记录: " + (jsonData.size() - existsCount) + " 条");

            // 4. 导出Excel
            System.out.println("\n📊 正在导出Excel...");
            File outputFile = new File(outputPath);
            EasyExcel.write(outputPath, JsonCsvCompareTool.class)
                    .sheet("JSON数据对比结果")
                    .doWrite(jsonData);
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
     * 加载CSV数据，提取所有product_id和device_id
     */
    private static Set<String> loadCsvData(String csvPath) throws Exception {
        Set<String> ids = new HashSet<>();
        File csvFile = new File(csvPath);

        if (!csvFile.exists()) {
            throw new RuntimeException("CSV文件不存在: " + csvPath);
        }

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(csvFile), StandardCharsets.UTF_8))) {

            String line;
            boolean isFirstLine = true;

            while ((line = br.readLine()) != null) {
                // 跳过BOM
                if (line.startsWith("\uFEFF")) {
                    line = line.substring(1);
                }

                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }

                // 解析CSV字段（处理带引号的字段）
                List<String> parsedFields = parseCsvFields(line);

                if (parsedFields.size() >= 5) {
                    // 提取product_id（第4列，索引3）和device_id（第5列，索引4）
                    String productId = parsedFields.get(3).replace("\"", "").trim();
                    String deviceId = parsedFields.get(4).replace("\"", "").trim();

                    if (!productId.isEmpty()) {
                        ids.add(productId);
                    }
                    if (!deviceId.isEmpty()) {
                        ids.add(deviceId);
                    }
                }
            }
        }

        return ids;
    }

    /**
     * 加载JSON数据并标记
     */
    private static List<JsonCsvCompareTool> loadJsonData(String jsonPath, Set<String> csvIds) throws Exception {
        List<JsonCsvCompareTool> dataList = new ArrayList<>();
        File jsonFile = new File(jsonPath);

        if (!jsonFile.exists()) {
            throw new RuntimeException("JSON文件不存在: " + jsonPath);
        }

        String content = org.apache.commons.io.FileUtils.readFileToString(jsonFile, StandardCharsets.UTF_8);
        JSONArray jsonArray = JSONArray.parseArray(content);

        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject item = jsonArray.getJSONObject(i);
            JsonCsvCompareTool data = new JsonCsvCompareTool();

            // 提取基本信息
            data.setIndex(item.getString("_index"));
            data.setType(item.getString("_type"));
            data.setId(item.getString("_id"));
            data.setScore(String.valueOf(item.get("_score")));

            // 提取source中的数据
            if (item.containsKey("_source")) {
                JSONObject source = item.getJSONObject("_source");
                data.setProductId(source.getString("productId"));
                data.setProvinceName(source.getString("provinceName"));
                data.setCityName(source.getString("cityName"));
                data.setTypeValue(source.getString("type"));
                data.setProvinceId(source.getString("provinceId") != null ?
                        String.valueOf(source.getInteger("provinceId")) : "");

                // 标记是否在CSV中存在
                String productId = data.getProductId();
                if (productId != null && csvIds.contains(productId)) {
                    data.setExistsInCsv("是");
                } else {
                    data.setExistsInCsv("否");
                }
            }

            dataList.add(data);
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
