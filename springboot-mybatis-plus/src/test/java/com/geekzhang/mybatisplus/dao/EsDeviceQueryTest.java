package com.geekzhang.mybatisplus.dao;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.geekzhang.mybatisplus.entity.MeituanXiaodaiDevice;
import com.geekzhang.mybatisplus.entity.Place;
import com.geekzhang.mybatisplus.mapper.MeituanXiaodaiDeviceMapper;
import com.geekzhang.mybatisplus.mapper.PlaceMapper;
import com.geekzhang.mybatisplus.util.HttpClientUtil;
import com.geekzhang.mybatisplus.util.JsonFileUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ES 设备分页查询测试
 * 排除指定城市（深圳市/北京市/广州市/上海市/武汉市/成都市/杭州市/西安市/南京市）
 * 筛选类型：GEGE / NVWA
 */
@Slf4j
@SpringBootTest
public class EsDeviceQueryTest {

    private static final String ES_URL =
            "https://esnew.yunjichina.com.cn/device.info.*,device.info.up/_search";

    // 替换为实际 Authorization 值（Basic xxx 或 Bearer xxx）
    private static final String ES_AUTH = "Basic yun17ji18";

    private static final int PAGE_SIZE = 1000;

    // ES 默认 max_result_window=10000，from+size 不能超过该值
    private static final int MAX_FROM = 90000;

    private static final List<String> EXCLUDE_CITIES = Arrays.asList(
            "深圳市", "北京市", "广州市", "上海市", "武汉市", "成都市", "杭州市", "西安市", "南京市"
    );

    // 输出目录：模块根目录下的 output 文件夹（绝对路径，不受 IDEA 工作目录影响）
    private static final String OUTPUT_DIR =
            Paths.get("").toAbsolutePath().toString().replaceAll("springboot-mybatis-plus.*", "springboot-mybatis-plus")
            + "/output";

    @Autowired
    private HttpClientUtil httpClientUtil;

    @Autowired
    private PlaceMapper placeMapper;

    @Autowired
    private MeituanXiaodaiDeviceMapper meituanXiaodaiDeviceMapper;

    /**
     * 分页查询排除指定城市的设备，按城市统计结果
     */
    @Test
    public void testQueryEsExcludeCities() {
        int from = 0;
        int total = Integer.MAX_VALUE;
        int pageNum = 1;
        List<JSONObject> allSources = new ArrayList<>();

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", ES_AUTH);

        while (from < total && from <= MAX_FROM) {
            String body = buildExcludeCitiesQuery(from, PAGE_SIZE);
            ResponseEntity<String> response =
                    httpClientUtil.postJson(ES_URL, body, headers, String.class);

            JSONObject result = JSON.parseObject(response.getBody());
            JSONObject hits = result.getJSONObject("hits");

            // 首次拿到总数（ES 7.x: total={value,relation}；ES 6.x: total=数字）
            if (from == 0) {
                Object totalObj = hits.get("total");
                if (totalObj instanceof Number) {
                    total = ((Number) totalObj).intValue();
                } else {
                    total = hits.getJSONObject("total").getIntValue("value");
                }
                log.info("ES 总命中: {}", total);
                if (total > MAX_FROM + PAGE_SIZE) {
                    log.warn("总数 {} 超过 max_result_window，仅获取前 {} 条，如需全量请改用 scroll API",
                            total, MAX_FROM + PAGE_SIZE);
                }
            }

            JSONArray list = hits.getJSONArray("hits");
            if (list == null || list.isEmpty()) {
                break;
            }

            int beforeSize = allSources.size();
            for (int i = 0; i < list.size(); i++) {
                JSONObject source = list.getJSONObject(i).getJSONObject("_source");
                String placeId = source.getString("placeId");
                if (placeId != null && !placeId.isEmpty()) {
                    allSources.add(source);
                }
            }

            log.info("第 {} 页: from={} 本页 {} 条（过滤后 +{}），累计 {} 条",
                    pageNum, from, list.size(), allSources.size() - beforeSize, allSources.size());

            from += PAGE_SIZE;
            pageNum++;
        }

        log.info("查询完成，共获取 {} 条（排除指定城市）", allSources.size());

        // 输出 JSON 文件
        String fileName = JsonFileUtil.generateTimestampFileName("es_device_exclude_cities", "json");
        String filePath = OUTPUT_DIR + "/" + fileName;
        JsonFileUtil.writeListToJsonWithFastJson(allSources, filePath);
        log.info("结果已写入文件: {}", filePath);

        // 按城市统计 Top 20
        Map<String, Long> cityStats = allSources.stream()
                .collect(Collectors.groupingBy(
                        d -> d.getString("cityName") != null ? d.getString("cityName") : "未知",
                        Collectors.counting()));

        log.info("=== 城市分布 Top 20 ===");
        cityStats.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(20)
                .forEach(e -> log.info("  城市: {} \t数量: {}", e.getKey(), e.getValue()));

        // 按 type 统计
        Map<String, Long> typeStats = allSources.stream()
                .collect(Collectors.groupingBy(
                        d -> d.getString("type") != null ? d.getString("type") : "未知",
                        Collectors.counting()));
        log.info("=== 类型分布 ===");
        typeStats.forEach((t, c) -> log.info("  type: {} \t数量: {}", t, c));
    }

    /**
     * 读取 ES 查询输出的 JSON 文件，根据 placeId 批量查询 t_place 填充 storeId，
     * 过滤掉 storeId 为空的数据，输出新 JSON 文件
     *
     * 前置条件：先执行 testQueryEsExcludeCities，生成 output/es_device_exclude_cities_*.json
     */
    @Test
    public void testFillStoreId() throws IOException {
        // 读取最新的 ES 输出文件
        String inputFile = findLatestOutputFile("es_device_exclude_cities");
        log.info("读取文件: {}", inputFile);

        String json = Files.readString(Paths.get(inputFile), StandardCharsets.UTF_8);
        List<JSONObject> devices = JSON.parseArray(json, JSONObject.class);
        log.info("原始数据: {} 条", devices.size());

        // 收集所有 placeId（去重）
        List<String> placeIds = devices.stream()
                .map(d -> d.getString("placeId"))
                .filter(id -> id != null && !id.isEmpty())
                .distinct()
                .collect(Collectors.toList());
        log.info("去重 placeId: {} 个", placeIds.size());

        // 批量查询 t_place，每批 500 个
        Map<String, String> placeIdToStoreId = new HashMap<>();
        int batchSize = 500;
        for (int i = 0; i < placeIds.size(); i += batchSize) {
            List<String> batch = placeIds.subList(i, Math.min(i + batchSize, placeIds.size()));
            List<Place> places = placeMapper.selectList(
                    new LambdaQueryWrapper<Place>()
                            .select(Place::getPlaceId, Place::getStoreId)
                            .in(Place::getPlaceId, batch)
            );
            places.forEach(p -> placeIdToStoreId.put(p.getPlaceId(), p.getStoreId()));
        }
        log.info("查到 t_place 记录: {} 条", placeIdToStoreId.size());

        // 填充 storeId，过滤掉 storeId 为空的记录
        List<JSONObject> result = new ArrayList<>();
        for (JSONObject device : devices) {
            String placeId = device.getString("placeId");
            String storeId = placeIdToStoreId.get(placeId);
            if (storeId == null || storeId.isEmpty()) {
                continue;
            }
            device.put("storeId", storeId);
            result.add(device);
        }
        log.info("填充后有效数据: {} 条（过滤掉 {} 条 storeId 为空）",
                result.size(), devices.size() - result.size());

        // 写出新文件
        String outFile = OUTPUT_DIR + "/" + JsonFileUtil.generateTimestampFileName("es_device_with_store_id", "json");
        JsonFileUtil.writeListToJsonWithFastJson(result, outFile);
        log.info("结果已写入: {}", outFile);
    }

    /**
     * 读取 testFillStoreId 输出的 JSON，查询 t_meituan_xiaodai_info，
     * productId 存在则 meituan=1，否则 meituan=0，输出新 JSON 文件
     *
     * 前置条件：先执行 testFillStoreId，生成 output/es_device_with_store_id_*.json
     */
    @Test
    public void testFillMeituanFlag() throws IOException {
        // 读取最新的 storeId 填充文件
        String inputFile = findLatestOutputFile("es_device_with_store_id");
        log.info("读取文件: {}", inputFile);

        String json = Files.readString(Paths.get(inputFile), StandardCharsets.UTF_8);
        List<JSONObject> devices = JSON.parseArray(json, JSONObject.class);
        log.info("原始数据: {} 条", devices.size());

        // 收集所有 productId（去重）
        List<String> productIds = devices.stream()
                .map(d -> d.getString("productId"))
                .filter(id -> id != null && !id.isEmpty())
                .distinct()
                .collect(Collectors.toList());
        log.info("去重 productId: {} 个", productIds.size());

        // 批量查询 t_meituan_xiaodai_info，每批 500 个，只取 productId 列
        java.util.Set<String> meituanProductIds = new java.util.HashSet<>();
        int batchSize = 500;
        for (int i = 0; i < productIds.size(); i += batchSize) {
            List<String> batch = productIds.subList(i, Math.min(i + batchSize, productIds.size()));
            List<MeituanXiaodaiDevice> records = meituanXiaodaiDeviceMapper.selectList(
                    new LambdaQueryWrapper<MeituanXiaodaiDevice>()
                            .select(MeituanXiaodaiDevice::getProductId)
                            .in(MeituanXiaodaiDevice::getProductId, batch)
                            .eq(MeituanXiaodaiDevice::getStatus, 1)
            );
            records.forEach(r -> meituanProductIds.add(r.getProductId()));
        }
        log.info("t_meituan_xiaodai_info 命中 productId: {} 个", meituanProductIds.size());

        // 填充 meituan 标记
        int meituanCount = 0;
        for (JSONObject device : devices) {
            String productId = device.getString("productId");
            int flag = meituanProductIds.contains(productId) ? 1 : 0;
            device.put("meituan", flag);
            if (flag == 1) meituanCount++;
        }
        log.info("meituan=1: {} 条，meituan=0: {} 条", meituanCount, devices.size() - meituanCount);

        // 写出新文件
        String outFile = OUTPUT_DIR + "/" + JsonFileUtil.generateTimestampFileName("es_device_with_meituan", "json");
        JsonFileUtil.writeListToJsonWithFastJson(devices, outFile);
        log.info("结果已写入: {}", outFile);
    }

    /**
     * 读取 testFillMeituanFlag 输出的 JSON，
     * 过滤 meituan=1 的数据，对 storeId 去重，输出新 JSON 文件
     *
     * 前置条件：先执行 testFillMeituanFlag，生成 output/es_device_with_meituan_*.json
     */
    @Test
    public void testFilterMeituanAndDeduplicateByStoreId() throws IOException {
        String inputFile = findLatestOutputFile("es_device_with_meituan");
        log.info("读取文件: {}", inputFile);

        String json = Files.readString(Paths.get(inputFile), StandardCharsets.UTF_8);
        List<JSONObject> devices = JSON.parseArray(json, JSONObject.class);
        log.info("原始数据: {} 条", devices.size());

        // 过滤 meituan=1，再按 storeId 去重（保留第一条）
        Map<String, JSONObject> storeIdMap = new java.util.LinkedHashMap<>();
        for (JSONObject device : devices) {
            if (device.getIntValue("meituan") != 1) continue;
            String storeId = device.getString("storeId");
            if (storeId == null || storeId.isEmpty()) continue;
            storeIdMap.putIfAbsent(storeId, device);
        }

        List<JSONObject> result = new ArrayList<>(storeIdMap.values());
        log.info("meituan=1 且 storeId 去重后: {} 条", result.size());

        String outFile = OUTPUT_DIR + "/" + JsonFileUtil.generateTimestampFileName("es_device_meituan_unique_store", "json");
        JsonFileUtil.writeListToJsonWithFastJson(result, outFile);
        log.info("结果已写入: {}", outFile);
    }

    /**
     * 读取 testFilterMeituanAndDeduplicateByStoreId 输出的 JSON，
     * 整理成 Excel 文件（列：storeId / placeId / productId / type / provinceName / cityName）
     *
     * 前置条件：先执行 testFilterMeituanAndDeduplicateByStoreId，
     *           生成 output/es_device_meituan_unique_store_*.json
     */
    @Test
    public void testExportToExcel() throws IOException {
        String inputFile = findLatestOutputFile("es_device_meituan_unique_store");
        log.info("读取文件: {}", inputFile);

        String json = Files.readString(Paths.get(inputFile), StandardCharsets.UTF_8);
        List<JSONObject> devices = JSON.parseArray(json, JSONObject.class);
        log.info("待导出数据: {} 条", devices.size());

        // 表头
        List<String> headers = Arrays.asList(
                "storeId", "placeId", "productId", "type", "provinceName", "cityName"
        );

        // 数据行
        List<List<Object>> rows = new ArrayList<>();
        for (JSONObject d : devices) {
            List<Object> row = new ArrayList<>();
            row.add(d.getString("storeId"));
            row.add(d.getString("placeId"));
            row.add(d.getString("productId"));
            row.add(d.getString("type"));
            row.add(d.getString("provinceName"));
            row.add(d.getString("cityName"));
            rows.add(row);
        }

        String outFile = OUTPUT_DIR + "/" + JsonFileUtil.generateTimestampFileName("es_device_meituan_unique_store", "xlsx");
        Files.createDirectories(Paths.get(OUTPUT_DIR));

        cn.hutool.poi.excel.ExcelWriter writer = cn.hutool.poi.excel.ExcelUtil.getWriter(outFile);
        writer.writeHeadRow(headers);
        writer.write(rows);
        writer.close();

        log.info("Excel 已写入: {} 共 {} 行", outFile, rows.size());
    }

    /** 查找 output 目录下最新的匹配文件 */
    private String findLatestOutputFile(String prefix) throws IOException {
        return Files.list(Paths.get(OUTPUT_DIR))
                .filter(p -> p.getFileName().toString().startsWith(prefix))
                .max(java.util.Comparator.comparing(p -> p.getFileName().toString()))
                .orElseThrow(() -> new IOException("未找到文件: " + OUTPUT_DIR + "/" + prefix + "_*"))
                .toString();
    }

    /**
     * 构建 ES 查询 DSL：
     * - must:     type 为 GEGE 或 NVWA
     * - must_not: cityName 或 provinceName 在排除城市列表中
     */
    private String buildExcludeCitiesQuery(int from, int size) {
        JSONObject query = new JSONObject();

        // _source 字段
        query.put("_source", Arrays.asList("productId", "type", "provinceId", "provinceName", "cityName", "placeId"));
        query.put("from", from);
        query.put("size", size);

        // must: type = GEGE 或 NVWA
        JSONObject typeShould = new JSONObject();
        typeShould.put("should", Arrays.asList(
                matchClause("type", "GEGE"),
                matchClause("type", "NVWA")
        ));
        typeShould.put("minimum_should_match", 1);
        JSONObject typeBool = new JSONObject();
        typeBool.put("bool", typeShould);

        // must_not: cityName 或 provinceName 在排除城市列表中
        JSONObject cityShould = new JSONObject();
        cityShould.put("should", Arrays.asList(
                termsClause("cityName.keyword", EXCLUDE_CITIES),
                termsClause("provinceName.keyword", EXCLUDE_CITIES)
        ));
        cityShould.put("minimum_should_match", 1);
        JSONObject cityBool = new JSONObject();
        cityBool.put("bool", cityShould);

        JSONObject bool = new JSONObject();
        bool.put("must", Arrays.asList(typeBool));
        bool.put("must_not", Arrays.asList(cityBool));

        JSONObject queryWrapper = new JSONObject();
        queryWrapper.put("bool", bool);
        query.put("query", queryWrapper);

        return JSON.toJSONString(query);
    }

    private JSONObject matchClause(String field, String value) {
        JSONObject match = new JSONObject();
        match.put(field, value);
        JSONObject clause = new JSONObject();
        clause.put("match", match);
        return clause;
    }

    private JSONObject termsClause(String field, List<String> values) {
        JSONObject terms = new JSONObject();
        terms.put(field, values);
        JSONObject clause = new JSONObject();
        clause.put("terms", terms);
        return clause;
    }
}
