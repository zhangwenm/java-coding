package com.geekzhang.worktest.shop;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 新商城 OpenAPI 集成测试
 * <p>
 * 接口文档：新商城 OpenAPI 接口文档
 * 测试域：https://api-shop-test.yunjiai.cn
 * <p>
 * 【响应结构约定】
 *   实测外层 key 为 result（文档示例写的 data，实际不同）
 *   3.1 商品查询：成功 code=200
 *   4.1 订单创建：成功码兼容 0 / 200
 *   4.3 订单查询：成功码兼容 0 / 200
 *   4.4 批量订单查询：实测成功 code=200
 *   5. 仓储位置查询：成功 code=200，data 为数组，包含 goodsId 与 locations（type/sn/roads[id/count]）
 * <p>
 * 【执行顺序】
 *   TC-GOODS-01 先拉真实商品，提取 goodsId/skuId/price
 *   TC-ORDER-CREATE-01 下单，保存 orderSn
 *   TC-ORDER-QUERY-01  用 orderSn 查单（依赖 CREATE-01）
 *   TC-STORAGE-01 仓储位置查询（只传 storeId）
 * <p>
 * 【4.4 说明】
 *   /open/order/query/orders/{userId} 需要登录态，返回 401 时自动跳过正向用例
 */
@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ShopApiTest {

    // ==================== 配置 ====================

    private static final String BASE_URL = "https://api-shop-test.yunjiai.cn";
    private static final String STORE_ID = "202614384191436064731650500608";
    private static final String TEST_MOBILE = "13800138001";
    /** 4.4 文档示例路径含 {userId}，实测 404；改为不带 userId 的路径，userId 放 body */
    private static final String ORDER_LIST_URL = BASE_URL + "/open/order/query/orders";

    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    private static final MediaType JSON_TYPE = MediaType.get("application/json; charset=utf-8");

    // ==================== 跨用例上下文 ====================

    private static String firstGoodsId;
    private static String firstSkuId;
    private static double firstGoodsPrice;
    private static String firstGoodsName;
    private static String createdOrderSn;
    private static final List<GoodsCandidate> GOODS_CANDIDATES = new ArrayList<>();
    private static final List<AddressCandidate> ADDRESS_CANDIDATES = List.of(
            new AddressCandidate("自动化测试", TEST_MOBILE, "1001"),
            new AddressCandidate("自动化测试", TEST_MOBILE, "上海市青浦区崧泽大道333号"),
            new AddressCandidate("自动化测试", TEST_MOBILE, "上海市青浦区诸光路1888号"),
            new AddressCandidate("自动化测试", TEST_MOBILE, "上海市徐汇区漕溪北路398号"),
            new AddressCandidate("自动化测试", TEST_MOBILE, "北京市朝阳区建国路88号")
    );

    @BeforeAll
    static void setUpClass() {
        loadGoodsCandidates();
    }

    // ==================== 3.1 商品查询 ====================

    @Test
    @Order(1)
    @DisplayName("TC-GOODS-01 商品菜单-正常查询 [预期:code=200,categories不为空,提取第一个有价格的商品]")
    void goods_menu_normal() {
        loadGoodsCandidates();
        assertNotNull(firstGoodsId, "未找到任何 price>0 的商品，请检查门店商品数据");
        assertFalse(GOODS_CANDIDATES.isEmpty(), "候选商品列表不能为空");
    }

    private static void loadGoodsCandidates() {
        JSONObject resp = get(BASE_URL + "/open/goods/menu?storeId=" + STORE_ID);
        assertCode200(resp);

        // 实测响应外层 key 为 result
        JSONObject result = getResult(resp);
        assertNotNull(result, "result/data不能为null");
        JSONArray categories = result.getJSONArray("categories");
        assertNotNull(categories, "categories不能为null");
        assertFalse(categories.isEmpty(), "categories不应为空列表");

        log.info("TC-GOODS-01 一级分类数={}", categories.size());

        // 收集候选商品，按价格升序排列，供下单阶段依次尝试
        firstGoodsId = null;
        firstSkuId = null;
        firstGoodsPrice = 0;
        firstGoodsName = null;
        GOODS_CANDIDATES.clear();
        collectGoodsCandidates(categories);
        GOODS_CANDIDATES.sort(Comparator.comparingDouble(GoodsCandidate::price));

        if (!GOODS_CANDIDATES.isEmpty()) {
            GoodsCandidate firstCandidate = GOODS_CANDIDATES.get(0);
            firstGoodsId = firstCandidate.goodsId();
            firstSkuId = firstCandidate.skuId();
            firstGoodsPrice = firstCandidate.price();
            firstGoodsName = firstCandidate.goodsName();
        }

        assertNotNull(firstGoodsId, "未找到任何 price>0 的商品，请检查门店商品数据");
        log.info("TC-GOODS-01 收集候选商品数={}，首个候选 goodsId={} skuId={} price={} name={}",
                GOODS_CANDIDATES.size(), firstGoodsId, firstSkuId, firstGoodsPrice, firstGoodsName);
    }

    // ==================== 4.1 订单创建 ====================

    @Test
    @Order(10)
    @DisplayName("TC-ORDER-CREATE-01 创建订单-正常下单 [预期:code=0或200,orderSn有值]")
    void order_create_normal() {
        ensureOrderCreated();
        assertNotNull(createdOrderSn, "orderSn不能为空");
    }

    // ==================== 4.3 订单查询 ====================

    @Test
    @Order(20)
    @DisplayName("TC-ORDER-QUERY-01 订单查询-用orderSn查询 [预期:code=0,sn与入参一致]")
    void order_query_by_order_sn() {
        ensureOrderCreated();
        JSONObject resp = get(BASE_URL + "/open/order/query/byOrderSn?orderSn=" + createdOrderSn);
        assertCode0Or200(resp);

        JSONObject result = getResult(resp);
        assertNotNull(result, "result/data不能为null");
        JSONObject order = result.getJSONObject("order");
        assertNotNull(order, "result.order不能为null");
        assertEquals(createdOrderSn, order.getString("sn"),
                "返回的sn应与查询入参orderSn一致");
        log.info("TC-ORDER-QUERY-01 orderStatus={} payStatus={}",
                order.getString("orderStatus"), order.getString("payStatus"));
    }

    // ==================== 4.4 批量订单查询 ====================

    @Test
    @Order(30)
    @DisplayName("TC-ORDER-LIST-01 批量订单查询-正常分页 [预期:code=0,records字段存在]")
    void order_list_normal() {
        JSONObject body = buildOrderListBody(1, 10);
        JSONObject resp = post(ORDER_LIST_URL, body);
        log.info("TC-ORDER-LIST-01 resp={}", resp.toJSONString());
        assertCode200(resp);
        JSONObject result = getResult(resp);
        assertNotNull(result, "result/data不能为null");
        assertNotNull(result.getJSONArray("records"), "records不能为null");
        log.info("TC-ORDER-LIST-01 total={} current={}",
                result.getLongValue("total"), result.getLongValue("current"));
    }

    private static void ensureOrderCreated() {
        loadGoodsCandidates();
        if (createdOrderSn != null) {
            return;
        }

        JSONObject lastResp = null;
        String lastFailureReason = null;
        GoodsCandidate lastGoodsCandidate = null;
        AddressCandidate lastAddressCandidate = null;
        for (AddressCandidate addressCandidate : ADDRESS_CANDIDATES) {
            for (GoodsCandidate goodsCandidate : GOODS_CANDIDATES) {
                JSONObject body = buildOrderBody(goodsCandidate, addressCandidate);
                JSONObject resp = post(BASE_URL + "/open/order/create", body);
                lastResp = resp;
                lastGoodsCandidate = goodsCandidate;
                lastAddressCandidate = addressCandidate;

                if (isOrderCreateSuccess(resp)) {
                    firstGoodsId = goodsCandidate.goodsId();
                    firstSkuId = goodsCandidate.skuId();
                    firstGoodsPrice = goodsCandidate.price();
                    firstGoodsName = goodsCandidate.goodsName();

                    JSONObject result = getResult(resp);
                    assertNotNull(result, "result/data不能为null");
                    createdOrderSn = result.getString("orderSn");
                    assertNotNull(createdOrderSn, "orderSn不能为null");
                    log.info("TC-ORDER-CREATE-01 下单成功 goodsId={} skuId={} price={} name={} address={} orderSn={} outOrderId={}",
                            firstGoodsId, firstSkuId, firstGoodsPrice, firstGoodsName,
                            addressCandidate.consigneeDetail(), createdOrderSn, result.getString("outOrderId"));
                    return;
                }

                String message = resp.getString("message");
                lastFailureReason = message;

                if (message != null && message.contains("商品不足")) {
                    log.info("TC-ORDER-CREATE-01 商品库存不足，切换候选 goodsId={} skuId={} price={} name={}",
                            goodsCandidate.goodsId(), goodsCandidate.skuId(), goodsCandidate.price(), goodsCandidate.goodsName());
                    continue;
                }

                if (message != null && (message.contains("地址已失效") || message.contains("配送") || message.contains("地址"))) {
                    log.info("TC-ORDER-CREATE-01 地址不可用，切换地址 consigneeDetail={} message={}",
                            addressCandidate.consigneeDetail(), message);
                    break;
                }

                fail("创建订单失败，无法自动恢复，resp=" + resp.toJSONString());
            }
        }

        fail(String.format(
                "创建订单失败。最后失败原因=%s；最后商品={goodsId=%s, skuId=%s, goodsName=%s, price=%s}；最后地址={consigneeName=%s, consigneeMobile=%s, consigneeDetail=%s}；最后响应=%s",
                lastFailureReason,
                lastGoodsCandidate == null ? null : lastGoodsCandidate.goodsId(),
                lastGoodsCandidate == null ? null : lastGoodsCandidate.skuId(),
                lastGoodsCandidate == null ? null : lastGoodsCandidate.goodsName(),
                lastGoodsCandidate == null ? null : lastGoodsCandidate.price(),
                lastAddressCandidate == null ? null : lastAddressCandidate.consigneeName(),
                lastAddressCandidate == null ? null : lastAddressCandidate.consigneeMobile(),
                lastAddressCandidate == null ? null : lastAddressCandidate.consigneeDetail(),
                lastResp == null ? "null" : lastResp.toJSONString()
        ));
    }

    // ==================== HTTP 执行 ====================

    private static JSONObject get(String url) {
        try {
            Request req = new Request.Builder().url(url).get().build();
            try (Response resp = CLIENT.newCall(req).execute()) {
                String body = resp.body() != null ? resp.body().string() : "{}";
                log.info("GET {} | HTTP {} | {}", url, resp.code(), body);
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

    private static JSONObject post(String url, JSONObject body) {
        String bodyStr = body.toJSONString();
        try {
            Request req = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create(bodyStr, JSON_TYPE))
                    .header("Content-Type", "application/json")
                    .build();
            try (Response resp = CLIENT.newCall(req).execute()) {
                String respBody = resp.body() != null ? resp.body().string() : "{}";
                log.info("POST {} | HTTP {} | {}", url, resp.code(), respBody);
                return JSON.parseObject(respBody);
            }
        } catch (Exception e) {
            log.error("POST请求异常 url={}: {}", url, e.getMessage());
            JSONObject err = new JSONObject();
            err.put("code", -1);
            err.put("message", e.getMessage());
            return err;
        }
    }

    // ==================== 断言工具 ====================

    /** 3.1 商品查询成功码 */
    private static void assertCode200(JSONObject resp) {
        int actual = resp.getIntValue("code");
        String message = resp.getString("message");
        assertEquals(200, actual,
                String.format("code期望=200 实际=%d message=%s", actual, message));
    }

    /** 4.x 订单接口成功码 */
    private static void assertCode0(JSONObject resp) {
        int actual = resp.getIntValue("code");
        String message = resp.getString("message");
        assertEquals(0, actual,
                String.format("code期望=0 实际=%d message=%s", actual, message));
    }

    private static void assertCode0Or200(JSONObject resp) {
        int actual = resp.getIntValue("code");
        String message = resp.getString("message");
        assertTrue(actual == 0 || actual == 200,
                String.format("code期望=0或200 实际=%d message=%s", actual, message));
    }

    private static boolean isOrderCreateSuccess(JSONObject resp) {
        int code = resp.getIntValue("code");
        boolean success = resp.getBooleanValue("success");
        return success && (code == 0 || code == 200);
    }

    /**
     * 兼容 result / data 两种响应外层 key
     * 实测 3.1 返回 result，4.x 待验证
     */
    private static JSONObject getResult(JSONObject resp) {
        JSONObject r = resp.getJSONObject("result");
        return r != null ? r : resp.getJSONObject("data");
    }

    // ==================== 构造工具 ====================

    private static JSONObject buildOrderBody(GoodsCandidate goodsCandidate, AddressCandidate addressCandidate) {
        JSONObject addressInfo = new JSONObject();
        addressInfo.put("consigneeDetail", addressCandidate.consigneeDetail());
        addressInfo.put("consigneeMobile", addressCandidate.consigneeMobile());
        addressInfo.put("consigneeName", addressCandidate.consigneeName());

        JSONObject item = new JSONObject();
        item.put("goodsId", goodsCandidate.goodsId());
        item.put("skuId", goodsCandidate.skuId());
        item.put("quantity", 1);
        item.put("price", goodsCandidate.price() > 0 ? goodsCandidate.price() : 0.01);
        item.put("goodsName", goodsCandidate.goodsName());

        JSONArray orderItems = new JSONArray();
        orderItems.add(item);

        JSONObject body = new JSONObject();
        body.put("storeId", STORE_ID);
        body.put("mobile", TEST_MOBILE);
        body.put("addressInfo", addressInfo);
        body.put("orderItems", orderItems);
        body.put("payType", 1);
        body.put("remark", "自动化测试订单-请忽略");
        body.put("nickname", "自动化测试");
        return body;
    }

    private static JSONObject buildOrderListBody(int pageNumber, int pageSize) {
        JSONObject body = new JSONObject();
        body.put("pageNumber", pageNumber);
        body.put("pageSize", pageSize);
        body.put("storeId", STORE_ID);
        body.put("mobile", TEST_MOBILE);   // 实测字段名为 mobile，非 userId
        return body;
    }

    /**
     * 递归遍历分类树（最多3级），收集 serviceType=GOODS 且 price>0 的商品。
     * CONTAINER_FETCH / CATERING 类商品下单方式特殊，跳过。
     */
    private static void collectGoodsCandidates(JSONArray categories) {
        if (categories == null) {
            return;
        }
        for (int i = 0; i < categories.size(); i++) {
            JSONObject node = categories.getJSONObject(i);

            JSONArray goodsList = node.getJSONArray("goodsList");
            if (goodsList != null) {
                for (int j = 0; j < goodsList.size(); j++) {
                    JSONObject g = goodsList.getJSONObject(j);
                    double price = g.getDoubleValue("price");
                    String serviceType = g.getString("serviceType");
                    if (price > 0 && "GOODS".equals(serviceType)) {
                        GOODS_CANDIDATES.add(new GoodsCandidate(
                                g.getString("goodsId"),
                                g.getString("skuId"),
                                price,
                                g.getString("goodsName")
                        ));
                    }
                }
            }

            collectGoodsCandidates(node.getJSONArray("children"));
        }
    }

    private record GoodsCandidate(String goodsId, String skuId, double price, String goodsName) {
    }

    private record AddressCandidate(String consigneeName, String consigneeMobile, String consigneeDetail) {
    }
    // ==================== 5. 仓储相关接口 ====================

    @Test
    @Order(40)
    @DisplayName("TC-STORAGE-01 仓储位置查询-只传storeId [预期:code=200,返回全量仓储信息]")
    void storage_location_normal() {
        JSONObject body = new JSONObject();
        body.put("storeId", STORE_ID);
        JSONObject resp = post(BASE_URL + "/open/storage/location", body);
        log.info("TC-STORAGE-01 resp={}", resp.toJSONString());
        assertCode200(resp);
        // 实测响应外层 key 为 result（数组），兼容 result / data
        JSONArray data = resp.getJSONArray("result");
        if (data == null) {
            data = resp.getJSONArray("data");
        }
        assertNotNull(data, "result/data 不能为 null");
        assertFalse(data.isEmpty(), "result/data 不能为空");
        log.info("TC-STORAGE-01 返回仓储记录数={}", data.size());
        for (int i = 0; i < data.size(); i++) {
            JSONObject item = data.getJSONObject(i);
            assertNotNull(item.getString("goodsId"), "goodsId 不能为 null");
            JSONArray locations = item.getJSONArray("locations");
            assertNotNull(locations, "locations 不能为 null");
            for (int li = 0; li < locations.size(); li++) {
                JSONObject loc = locations.getJSONObject(li);
                assertNotNull(loc.getString("type"), "location.type 不能为 null");
                assertNotNull(loc.getString("sn"), "location.sn 不能为 null");
                JSONArray roads = loc.getJSONArray("roads");
                if (roads != null) {
                    for (int ri = 0; ri < roads.size(); ri++) {
                        JSONObject road = roads.getJSONObject(ri);
                        assertNotNull(road.getString("id"), "road.id 不能为 null");
                        assertTrue(road.getIntValue("count") >= 0, "road.count 应为非负数");
                    }
                }
            }
            log.info("TC-STORAGE-01 goodsId={} locations数={}", item.getString("goodsId"), locations.size());
        }
    }


}
