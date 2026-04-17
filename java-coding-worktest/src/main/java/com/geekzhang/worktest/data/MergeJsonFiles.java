package com.geekzhang.worktest.data;

import com.alibaba.fastjson2.JSONArray;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * JSON文件合并工具
 * 合并 devicexiaodaigege.json 和 devicexiaodainvwa.json 到 devicexiaodai.json
 */
public class MergeJsonFiles {

    public static void main(String[] args) {
        String resourcesPath = "src/main/resources/";
        String gegePath = resourcesPath + "devicexiaodaigege.json";
        String nvwaPath = resourcesPath + "devicexiaodainvwa.json";
        String outputPath = resourcesPath + "devicexiaodai.json";

        System.out.println("=".repeat(60));
        System.out.println("开始合并JSON文件...");
        System.out.println("=".repeat(60));

        try {
            // 1. 读取两个JSON文件
            System.out.println("\n📖 正在读取 devicexiaodaigege.json...");
            File gegeFile = new File(gegePath);
            if (!gegeFile.exists()) {
                throw new RuntimeException("文件不存在: " + gegePath);
            }
            String gegeContent = FileUtils.readFileToString(gegeFile, StandardCharsets.UTF_8);
            JSONArray gegeArray = JSONArray.parseArray(gegeContent);
            System.out.println("   devicexiaodaigege.json: " + gegeArray.size() + " 条记录");

            System.out.println("\n📖 正在读取 devicexiaodainvwa.json...");
            File nvwaFile = new File(nvwaPath);
            if (!nvwaFile.exists()) {
                throw new RuntimeException("文件不存在: " + nvwaPath);
            }
            String nvwaContent = FileUtils.readFileToString(nvwaFile, StandardCharsets.UTF_8);
            JSONArray nvwaArray = JSONArray.parseArray(nvwaContent);
            System.out.println("   devicexiaodainvwa.json: " + nvwaArray.size() + " 条记录");

            // 2. 合并两个数组
            System.out.println("\n🔗 正在合并数据...");
            JSONArray mergedArray = new JSONArray();
            mergedArray.addAll(gegeArray);
            mergedArray.addAll(nvwaArray);

            System.out.println("   合并后总记录数: " + mergedArray.size());

            // 3. 检查是否有重复的productId
            System.out.println("\n🔍 检查重复记录...");
            java.util.Set<String> productIds = new java.util.HashSet<>();
            java.util.Set<String> duplicateIds = new java.util.HashSet<>();
            for (int i = 0; i < mergedArray.size(); i++) {
                com.alibaba.fastjson2.JSONObject item = mergedArray.getJSONObject(i);
                if (item.containsKey("_source")) {
                    com.alibaba.fastjson2.JSONObject source = item.getJSONObject("_source");
                    if (source.containsKey("productId")) {
                        String productId = source.getString("productId");
                        if (productIds.contains(productId)) {
                            duplicateIds.add(productId);
                        } else {
                            productIds.add(productId);
                        }
                    }
                }
            }

            if (!duplicateIds.isEmpty()) {
                System.out.println("   ⚠️  发现重复的 productId: " + duplicateIds.size() + " 个");
            } else {
                System.out.println("   ✅ 未发现重复记录");
            }

            // 4. 写入合并后的文件
            System.out.println("\n💾 正在写入 devicexiaodai.json...");
            File outputFile = new File(outputPath);
            String outputJson = mergedArray.toJSONString();
            FileUtils.writeStringToFile(outputFile, outputJson, StandardCharsets.UTF_8);
            System.out.println("   ✅ 成功写入: " + outputFile.getAbsolutePath());

            System.out.println("\n" + "=".repeat(60));
            System.out.println("✨ 合并完成!");
            System.out.println("=".repeat(60));

            System.out.println("\n📊 统计信息:");
            System.out.println("   devicexiaodaigege.json: " + gegeArray.size() + " 条");
            System.out.println("   devicexiaodainvwa.json: " + nvwaArray.size() + " 条");
            System.out.println("   合并到 devicexiaodai.json: " + mergedArray.size() + " 条");
            if (!duplicateIds.isEmpty()) {
                System.out.println("   重复的 productId: " + duplicateIds.size() + " 个");
            }
            System.out.println("   不同的 productId: " + productIds.size() + " 个");

        } catch (Exception e) {
            System.err.println("❌ 合并失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
