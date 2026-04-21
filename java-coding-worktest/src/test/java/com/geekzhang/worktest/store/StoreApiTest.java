package com.geekzhang.worktest.store;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.junit.jupiter.api.*;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 门店列表分页查询接口集成测试
 * <p>
 * 接口：GET /v3/store/listByPage
 * 特点：免鉴权（白名单部署后）、所有参数可选、必须分页
 * <p>
 * 鉴权说明：
 *   白名单部署前，dev01 环境需要鉴权。支持两种方式（二选一）：
 *   1. 环境变量 HDOS_ACCESS_TOKEN：接口支持 ?accessToken=xxx 查询参数鉴权
 *   2. 无需任何凭证：白名单部署后或 testing=true 时自动免鉴权
 * <p>
 * 默认值：current=1, pageSize=50
 * 响应结构：{ code, message, data: [{storeId,storeName,...}], current, pageSize, total }
 */
@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class StoreApiTest {

    private static final String BASE_URL = "https://dev01-open-api.yunjiai.cn";
    private static final String LIST_BY_PAGE_URL = BASE_URL + "/v3/store/listByPage";

    private static final String ACCESS_TOKEN = System.getenv().getOrDefault("HDOS_ACCESS_TOKEN", "");

    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    // ==================== TC-01 默认分页查询 ====================

    @Test
    @Order(1)
    @DisplayName("TC-STORE-01 默认分页-不传参数 [预期:code=200,默认current=1,pageSize=50]")
    void listByPage_defaultParams() {
        JSONObject resp = apiGet("");
        assertSuccess(resp);

        assertEquals(1, resp.getIntValue("current"), "默认current应为1");
        assertEquals(50, resp.getIntValue("pageSize"), "默认pageSize应为50");

        JSONArray data = resp.getJSONArray("data");
        assertNotNull(data, "data不能为null");
        log.info("TC-STORE-01 total={} dataCount={}", resp.getLongValue("total"), data.size());
    }

    // ==================== TC-02 指定分页参数 ====================

    @Test
    @Order(2)
    @DisplayName("TC-STORE-02 指定分页-current=1&pageSize=2 [预期:data.length<=2]")
    void listByPage_withPagination() {
        JSONObject resp = apiGet("current=1&pageSize=2");
        assertSuccess(resp);

        assertEquals(1, resp.getIntValue("current"));
        assertEquals(2, resp.getIntValue("pageSize"));

        JSONArray data = resp.getJSONArray("data");
        assertNotNull(data);
        assertTrue(data.size() <= 2, "data条数应<=pageSize");
        log.info("TC-STORE-02 dataCount={}", data.size());
    }

    // ==================== TC-03 第二页查询 ====================

    @Test
    @Order(3)
    @DisplayName("TC-STORE-03 翻页查询-current=2&pageSize=2 [预期:data与第一页不重复]")
    void listByPage_secondPage() {
        JSONObject resp1 = apiGet("current=1&pageSize=2");
        assertSuccess(resp1);
        JSONArray data1 = resp1.getJSONArray("data");

        JSONObject resp2 = apiGet("current=2&pageSize=2");
        assertSuccess(resp2);
        JSONArray data2 = resp2.getJSONArray("data");

        if (data1.size() == 2 && data2 != null && !data2.isEmpty()) {
            String id1 = data1.getJSONObject(0).getString("storeId");
            String id2 = data2.getJSONObject(0).getString("storeId");
            assertNotEquals(id1, id2, "第二页第一条不应与第一页第一条重复");
        }
        log.info("TC-STORE-03 page1={}条 page2={}条", data1.size(), data2 == null ? 0 : data2.size());
    }

    // ==================== TC-04 按门店名称模糊搜索 ====================

    @Test
    @Order(4)
    @DisplayName("TC-STORE-04 按名称搜索-storeName=酒店 [预期:有结果时名称非空]")
    void listByPage_byStoreName() {
        JSONObject resp = apiGet("storeName=酒店&pageSize=100");
        assertSuccess(resp);

        JSONArray data = resp.getJSONArray("data");
        assertNotNull(data);
        if (!data.isEmpty()) {
            JSONObject first = data.getJSONObject(0);
            assertNotNull(first.getString("storeId"), "storeId不能为空");
            assertNotNull(first.getString("storeName"), "storeName不能为空");
            log.info("TC-STORE-04 total={} firstStore={}", resp.getLongValue("total"), first.getString("storeName"));
        } else {
            log.info("TC-STORE-04 名称含'酒店'的门店为空（数据问题，非接口问题）");
        }
    }

    // ==================== TC-05 按storeId精确查询 ====================

    @Test
    @Order(5)
    @DisplayName("TC-STORE-05 按storeId查询 [预期:精确返回1条]")
    void listByPage_byStoreId() {
        JSONObject defaultResp = apiGet("pageSize=1");
        assertSuccess(defaultResp);
        JSONArray defaultData = defaultResp.getJSONArray("data");

        if (defaultData == null || defaultData.isEmpty()) {
            log.info("TC-STORE-05 环境无门店数据，跳过");
            return;
        }

        String targetStoreId = defaultData.getJSONObject(0).getString("storeId");
        assertNotNull(targetStoreId);

        JSONObject resp = apiGet("storeId=" + targetStoreId);
        assertSuccess(resp);

        JSONArray data = resp.getJSONArray("data");
        assertNotNull(data);
        assertFalse(data.isEmpty(), "按storeId查询应有结果");
        assertEquals(targetStoreId, data.getJSONObject(0).getString("storeId"));
        log.info("TC-STORE-05 storeId={} storeName={}", targetStoreId, data.getJSONObject(0).getString("storeName"));
    }

    // ==================== TC-06 按tenantId查询 ====================

    @Test
    @Order(6)
    @DisplayName("TC-STORE-06 按不存在tenantId查询 [预期:返回空列表]")
    void listByPage_byTenantId() {
        JSONObject emptyResp = apiGet("tenantId=NOT_EXIST_TENANT_99999");
        assertSuccess(emptyResp);
        JSONArray emptyData = emptyResp.getJSONArray("data");
        assertTrue(emptyData == null || emptyData.isEmpty(), "不存在的tenantId应返回空列表");
        log.info("TC-STORE-06 不存在tenantId返回空列表 ✓");
    }

    // ==================== TC-07 大pageSize查询 ====================

    @Test
    @Order(7)
    @DisplayName("TC-STORE-07 大pageSize=500 [预期:正常返回,不报错]")
    void listByPage_largePageSize() {
        JSONObject resp = apiGet("pageSize=500");
        assertSuccess(resp);

        JSONArray data = resp.getJSONArray("data");
        assertNotNull(data);
        log.info("TC-STORE-07 total={} dataCount={}", resp.getLongValue("total"), data.size());
    }

    // ==================== TC-08 无效页码 ====================

    @Test
    @Order(8)
    @DisplayName("TC-STORE-08 超大页码-current=99999 [预期:返回空列表,不报错]")
    void listByPage_invalidPage() {
        JSONObject resp = apiGet("current=99999&pageSize=10");
        assertSuccess(resp);

        JSONArray data = resp.getJSONArray("data");
        assertTrue(data == null || data.isEmpty(), "超大页码应返回空列表");
        log.info("TC-STORE-08 current=99999 返回空列表 ✓");
    }

    // ==================== TC-09 验证data字段结构 ====================

    @Test
    @Order(9)
    @DisplayName("TC-STORE-09 验证门店字段结构 [预期:含storeId/storeName/addressText/province/city/outStoreId]")
    void listByPage_verifyFields() {
        JSONObject resp = apiGet("pageSize=5");
        assertSuccess(resp);

        JSONArray data = resp.getJSONArray("data");
        if (data == null || data.isEmpty()) {
            log.info("TC-STORE-09 无数据，跳过字段校验");
            return;
        }

        JSONObject first = data.getJSONObject(0);
        assertTrue(first.containsKey("storeId"), "应包含storeId字段");
        assertTrue(first.containsKey("storeName"), "应包含storeName字段");
        assertTrue(first.containsKey("addressText"), "应包含addressText字段");
        assertTrue(first.containsKey("province"), "应包含province字段");
        assertTrue(first.containsKey("city"), "应包含city字段");
        assertTrue(first.containsKey("outStoreId"), "应包含outStoreId字段");

        log.info("TC-STORE-09 sample: storeId={} storeName={} province={} city={}",
                first.getString("storeId"), first.getString("storeName"),
                first.getString("province"), first.getString("city"));
    }

    // ==================== TC-10 组合查询 ====================

    @Test
    @Order(10)
    @DisplayName("TC-STORE-10 组合查询-storeName+pageSize [预期:同时生效]")
    void listByPage_combinedQuery() {
        JSONObject resp = apiGet("storeName=酒店&current=1&pageSize=3");
        assertSuccess(resp);

        assertEquals(1, resp.getIntValue("current"));
        assertEquals(3, resp.getIntValue("pageSize"));

        JSONArray data = resp.getJSONArray("data");
        assertNotNull(data);
        assertTrue(data.size() <= 3);
        log.info("TC-STORE-10 total={} dataCount={}", resp.getLongValue("total"), data.size());
    }

    // ==================== HTTP 工具 ====================

    /**
     * 发起 GET 请求，自动拼接鉴权参数。
     * 优先使用环境变量 HDOS_ACCESS_TOKEN（?accessToken=xxx），
     * 未设置时走免鉴权路径（依赖服务端白名单或 testing=true）。
     */
    private static JSONObject apiGet(String queryParams) {
        String url = LIST_BY_PAGE_URL;
        String separator = "?";

        if (!ACCESS_TOKEN.isEmpty()) {
            url += separator + "accessToken=" + ACCESS_TOKEN;
            separator = "&";
        }

        if (queryParams != null && !queryParams.isEmpty()) {
            url += separator + queryParams;
        }

        return doGet(url);
    }

    private static JSONObject doGet(String url) {
        try {
            Request req = new Request.Builder().url(url).get().build();
            try (Response resp = CLIENT.newCall(req).execute()) {
                String body = resp.body() != null ? resp.body().string() : "{}";
                log.info("GET {} | HTTP {} | {}", url, resp.code(),
                        body.length() > 500 ? body.substring(0, 500) + "..." : body);
                return JSON.parseObject(body);
            }
        } catch (Exception e) {
            log.error("GET请求异常 url={}: {}", url, e.getMessage());
            JSONObject err = new JSONObject();
            err.put("code", -1);
            err.put("message", e.getMessage());
            return err;
        }
    }

    // ==================== 断言工具 ====================

    private static void assertSuccess(JSONObject resp) {
        int code = resp.getIntValue("code");
        // FrontResponse 成功码：code=200 或 code 字段值为字符串 "SUCCESS"
        assertTrue(code == 200 || "SUCCESS".equals(resp.getString("code")),
                String.format("接口调用失败 code=%s message=%s", code, resp.getString("message")));
    }
}
