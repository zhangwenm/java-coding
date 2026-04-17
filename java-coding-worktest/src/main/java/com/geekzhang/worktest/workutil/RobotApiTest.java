package com.geekzhang.worktest.workutil;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 机器人接口测试
 * <p>
 * 涵盖：召唤/送物下单接口（创建任务）+ 取消任务接口
 * 调用规范参照：机器人设备接口标准化文档 v1.0
 * <p>
 * 认证方式：HMAC Auth（高性能端iPaaS默认）
 * Header规范：ReqId / Date(GMT) / ReqFrom / Digest(SHA-256) / Authorization(hmac) / syspipe / Content-Type
 * 响应errcode：0000=成功，0001=业务失败，0006=鉴权失败
 */
@Slf4j
public class RobotApiTest {

    // ==================== 基础配置 ====================

    private static final String BASE_URL = "https://ipaas-gw-test.jinjianghotels.com.cn";

    /** 创建任务（召唤/送物下单）*/
    private static final String CREATE_TASK_URL = BASE_URL + "/robot-svc/ipaas/robot/call";

    /** 取消任务 */
    private static final String CANCEL_TASK_URL = BASE_URL + "/robot-svc/ipaas/robot/task/cancel";

    /** 系统管道编码（31=云迹，32=擎朗，33=普渡，34=优地）*/
    private static final String SYSPIPE = "31";

    /** 应用系统编码（见文档3.1节）*/
    private static final String REQ_FROM = "JINTELL";

    /**
     * HMAC用户名与密钥（由iPaaS平台提供）
     * 注意：secret禁止明文写死代码，应从配置文件或环境变量读取
     */
    private static final String HMAC_USERNAME = "JINTELL-R6VIS50F";
    private static final String HMAC_SECRET   = System.getenv().getOrDefault("ROBOT_HMAC_SECRET", "fu58965W0k579qyWZ35uA2I3E1dIVikh");

    /** 测试数据 */
    private static final String ROBOT_ID     = "GG2L03211C0300439";
    private static final String HOTEL_ID     = "88888888";
    private static final String TARGET       = "101";
    private static final String CALLBACK_URL = "http://your-callback-server/robot/callback";

    /** readTimeout > 60s（文档要求网关响应时间上限60秒）*/
    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(65, TimeUnit.SECONDS)
            .build();

    private static final MediaType JSON_TYPE = MediaType.get("application/json; charset=utf-8");

    /** GMT时间格式（Date Header）*/
    private static final DateTimeFormatter GMT_FMT =
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.ENGLISH);

    // ==================== main 入口 ====================

    public static void main(String[] args) {
        log.info("========== 创建任务：简单召唤 ==========");
        testCreate_simpleCall_byHotelId();
        testCreate_simpleCall_byRobotId();
        testCreate_simpleCall_withTaskType();
        testCreate_simpleCall_missingTarget();
        testCreate_simpleCall_missingCallbackUrl();
        testCreate_simpleCall_missingHotelIdAndRobotId();
        testCreate_simpleCall_missingRequestId();
        testCreate_simpleCall_missingSyspipe();
        testCreate_simpleCall_wrongSignature();

        log.info("========== 创建任务：召唤送物（itemParams模式）==========");
        testCreate_deliveryCall_singleItem();
        testCreate_deliveryCall_multiItems();
        testCreate_deliveryCall_backToCaller();
        testCreate_deliveryCall_missingItemParams();
        testCreate_deliveryCall_itemMissingDoorId();
        testCreate_deliveryCall_itemMissingTo();

        log.info("========== 创建任务：货柜送物/接力送（goodsInfo模式）==========");
        testCreate_relayDelivery_basic();
        testCreate_relayDelivery_withOutTaskId();
        testCreate_relayDelivery_missingHotelId();
        testCreate_relayDelivery_missingGoodsInfo();
        testCreate_relayDelivery_goodsMissingName();

        log.info("========== 取消任务 ==========");
        testCancel_normal_autoBackTrue();
        testCancel_autoBackFalse();
        testCancel_withBoxId();
        testCancel_withTarget();
        testCancel_withBoxIdAndTarget();
        testCancel_missingRequestId();
        testCancel_missingRobotId();
        testCancel_robotIdNotExist();
        testCancel_missingSyspipe();
        testCancel_wrongSignature();
    }

    // ==================== 创建任务：简单召唤 ====================

    /**
     * TC-CREATE-01 简单召唤，用hotelId（厂商自动调度机器人）
     * 前置：无
     * 预期：errcode=0000，result.success=true，result.robotId有值（未指定robotId时厂商必须返回）
     */
    static void testCreate_simpleCall_byHotelId() {
        JSONObject body = new JSONObject();
        body.put("requestId", uuid());
        body.put("hotelId", HOTEL_ID);
        body.put("target", TARGET);
        body.put("callbackUrl", CALLBACK_URL);
        post("TC-CREATE-01 [简单召唤-hotelId]", CREATE_TASK_URL, body, true, true);
    }

    /**
     * TC-CREATE-02 简单召唤，指定robotId
     * 前置：robotId对应机器人在线且空闲
     * 预期：errcode=0000，result.success=true
     */
    static void testCreate_simpleCall_byRobotId() {
        JSONObject body = new JSONObject();
        body.put("requestId", uuid());
        body.put("robotId", ROBOT_ID);
        body.put("target", TARGET);
        body.put("callbackUrl", CALLBACK_URL);
        post("TC-CREATE-02 [简单召唤-robotId]", CREATE_TASK_URL, body, true, true);
    }

    /**
     * TC-CREATE-03 简单召唤，指定taskType过滤机器人（由厂商调度支持该类型的机器人）
     * 前置：酒店内有支持delivery类型的机器人
     * 预期：errcode=0000
     */
    static void testCreate_simpleCall_withTaskType() {
        JSONObject body = new JSONObject();
        body.put("requestId", uuid());
        body.put("hotelId", HOTEL_ID);
        body.put("target", TARGET);
        body.put("taskType", "delivery");
        body.put("callbackUrl", CALLBACK_URL);
        post("TC-CREATE-03 [简单召唤-带taskType]", CREATE_TASK_URL, body, true, true);
    }

    /**
     * TC-CREATE-04 简单召唤，缺少必填 target
     * 预期：errcode=0001（业务失败）
     */
    static void testCreate_simpleCall_missingTarget() {
        JSONObject body = new JSONObject();
        body.put("requestId", uuid());
        body.put("hotelId", HOTEL_ID);
        body.put("callbackUrl", CALLBACK_URL);
        post("TC-CREATE-04 [简单召唤-缺target → 0001]", CREATE_TASK_URL, body, true, true);
    }

    /**
     * TC-CREATE-05 简单召唤，缺少必填 callbackUrl
     * 预期：errcode=0001（业务失败）
     */
    static void testCreate_simpleCall_missingCallbackUrl() {
        JSONObject body = new JSONObject();
        body.put("requestId", uuid());
        body.put("hotelId", HOTEL_ID);
        body.put("target", TARGET);
        post("TC-CREATE-05 [简单召唤-缺callbackUrl → 0001]", CREATE_TASK_URL, body, true, true);
    }

    /**
     * TC-CREATE-06 简单召唤，hotelId与robotId均为空
     * 预期：errcode=0001（业务失败，文档规定至少填一个）
     */
    static void testCreate_simpleCall_missingHotelIdAndRobotId() {
        JSONObject body = new JSONObject();
        body.put("requestId", uuid());
        body.put("target", TARGET);
        body.put("callbackUrl", CALLBACK_URL);
        post("TC-CREATE-06 [简单召唤-hotelId与robotId均空 → 0001]", CREATE_TASK_URL, body, true, true);
    }

    /**
     * TC-CREATE-07 简单召唤，缺少必填 requestId
     * 预期：errcode=0001（业务失败）
     */
    static void testCreate_simpleCall_missingRequestId() {
        JSONObject body = new JSONObject();
        body.put("hotelId", HOTEL_ID);
        body.put("target", TARGET);
        body.put("callbackUrl", CALLBACK_URL);
        post("TC-CREATE-07 [简单召唤-缺requestId → 0001]", CREATE_TASK_URL, body, true, true);
    }

    /**
     * TC-CREATE-08 简单召唤，缺少Header syspipe
     * 预期：errcode=0001 或网关直接报错
     */
    static void testCreate_simpleCall_missingSyspipe() {
        JSONObject body = new JSONObject();
        body.put("requestId", uuid());
        body.put("hotelId", HOTEL_ID);
        body.put("target", TARGET);
        body.put("callbackUrl", CALLBACK_URL);
        post("TC-CREATE-08 [简单召唤-缺syspipe]", CREATE_TASK_URL, body, false, true);
    }

    /**
     * TC-CREATE-09 简单召唤，Authorization签名错误
     * 预期：errcode=0006（鉴权失败）
     */
    static void testCreate_simpleCall_wrongSignature() {
        JSONObject body = new JSONObject();
        body.put("requestId", uuid());
        body.put("hotelId", HOTEL_ID);
        body.put("target", TARGET);
        body.put("callbackUrl", CALLBACK_URL);
        post("TC-CREATE-09 [简单召唤-错误签名 → 0006]", CREATE_TASK_URL, body, true, false);
    }

    // ==================== 创建任务：召唤送物（itemParams模式）====================

    /**
     * TC-CREATE-10 召唤送物，单个仓门目标
     * 预期：errcode=0000，result.success=true
     */
    static void testCreate_deliveryCall_singleItem() {
        JSONObject body = new JSONObject();
        body.put("requestId", uuid());
        body.put("hotelId", HOTEL_ID);
        body.put("taskType", "delivery");
        body.put("from", "前台");
        body.put("callbackUrl", CALLBACK_URL);
        JSONArray items = new JSONArray();
        items.add(buildItem(1, "808"));
        body.put("itemParams", items);
        post("TC-CREATE-10 [召唤送物-单目标]", CREATE_TASK_URL, body, true, true);
    }

    /**
     * TC-CREATE-11 召唤送物，多个仓门目标（一次送多个房间）
     * 预期：errcode=0000，result.success=true
     */
    static void testCreate_deliveryCall_multiItems() {
        JSONObject body = new JSONObject();
        body.put("requestId", uuid());
        body.put("hotelId", HOTEL_ID);
        body.put("taskType", "delivery");
        body.put("from", "前台");
        body.put("callbackUrl", CALLBACK_URL);
        JSONArray items = new JSONArray();
        items.add(buildItem(1, "101"));
        items.add(buildItem(2, "808"));
        body.put("itemParams", items);
        post("TC-CREATE-11 [召唤送物-多目标]", CREATE_TASK_URL, body, true, true);
    }

    /**
     * TC-CREATE-12 召唤送物，backToCaller=true（未取物返回召唤点而非充电桩）
     * 预期：errcode=0000
     */
    static void testCreate_deliveryCall_backToCaller() {
        JSONObject body = new JSONObject();
        body.put("requestId", uuid());
        body.put("hotelId", HOTEL_ID);
        body.put("taskType", "delivery");
        body.put("backToCaller", true);
        body.put("callbackUrl", CALLBACK_URL);
        JSONArray items = new JSONArray();
        items.add(buildItem(1, "302"));
        body.put("itemParams", items);
        post("TC-CREATE-12 [召唤送物-backToCaller=true]", CREATE_TASK_URL, body, true, true);
    }

    /**
     * TC-CREATE-13 召唤送物，缺少必填 itemParams
     * 预期：errcode=0001（业务失败）
     */
    static void testCreate_deliveryCall_missingItemParams() {
        JSONObject body = new JSONObject();
        body.put("requestId", uuid());
        body.put("hotelId", HOTEL_ID);
        body.put("taskType", "delivery");
        body.put("callbackUrl", CALLBACK_URL);
        post("TC-CREATE-13 [召唤送物-缺itemParams → 0001]", CREATE_TASK_URL, body, true, true);
    }

    /**
     * TC-CREATE-14 召唤送物，itemParams中缺少必填 doorId
     * 预期：errcode=0001（业务失败）
     */
    static void testCreate_deliveryCall_itemMissingDoorId() {
        JSONObject body = new JSONObject();
        body.put("requestId", uuid());
        body.put("hotelId", HOTEL_ID);
        body.put("taskType", "delivery");
        body.put("callbackUrl", CALLBACK_URL);
        JSONArray items = new JSONArray();
        JSONObject item = new JSONObject();
        item.put("to", "808"); // 无doorId
        items.add(item);
        body.put("itemParams", items);
        post("TC-CREATE-14 [召唤送物-item缺doorId → 0001]", CREATE_TASK_URL, body, true, true);
    }

    /**
     * TC-CREATE-15 召唤送物，itemParams中缺少必填 to（送达点位）
     * 预期：errcode=0001（业务失败）
     */
    static void testCreate_deliveryCall_itemMissingTo() {
        JSONObject body = new JSONObject();
        body.put("requestId", uuid());
        body.put("hotelId", HOTEL_ID);
        body.put("taskType", "delivery");
        body.put("callbackUrl", CALLBACK_URL);
        JSONArray items = new JSONArray();
        JSONObject item = new JSONObject();
        item.put("doorId", 1); // 无to
        items.add(item);
        body.put("itemParams", items);
        post("TC-CREATE-15 [召唤送物-item缺to → 0001]", CREATE_TASK_URL, body, true, true);
    }

    // ==================== 创建任务：货柜送物/接力送（goodsInfo模式）====================

    /**
     * TC-CREATE-16 货柜送物/接力送，基础用例（hotelId+target+goodsInfo）
     * 前置：酒店有货柜机器人可调度
     * 预期：errcode=0000，result.success=true
     */
    static void testCreate_relayDelivery_basic() {
        JSONObject body = new JSONObject();
        body.put("requestId", uuid());
        body.put("hotelId", HOTEL_ID);
        body.put("taskType", "delivery");
        body.put("from", "前台");
        body.put("target", "808");
        body.put("callbackUrl", CALLBACK_URL);
        body.put("goodsInfo", buildGoods("矿泉水", 2));
        post("TC-CREATE-16 [接力送-基础]", CREATE_TASK_URL, body, true, true);
    }

    /**
     * TC-CREATE-17 货柜送物/接力送，带 outTaskId（外部订单ID，回调时原样返回）
     * 预期：errcode=0000，回调中 extraData.outTaskId 与请求一致
     */
    static void testCreate_relayDelivery_withOutTaskId() {
        String outTaskId = "ORDER_" + System.currentTimeMillis();
        JSONObject body = new JSONObject();
        body.put("requestId", uuid());
        body.put("hotelId", HOTEL_ID);
        body.put("taskType", "delivery");
        body.put("target", "302");
        body.put("password", "1234");
        body.put("outTaskId", outTaskId);
        body.put("callbackUrl", CALLBACK_URL);
        body.put("goodsInfo", buildGoods("毛巾", 1));
        log.info("outTaskId={}", outTaskId);
        post("TC-CREATE-17 [接力送-带outTaskId]", CREATE_TASK_URL, body, true, true);
    }

    /**
     * TC-CREATE-18 货柜送物/接力送，hotelId为空（文档规定货柜送物hotelId必填）
     * 预期：errcode=0001（业务失败）
     */
    static void testCreate_relayDelivery_missingHotelId() {
        JSONObject body = new JSONObject();
        body.put("requestId", uuid());
        body.put("taskType", "delivery");
        body.put("target", "808");
        body.put("callbackUrl", CALLBACK_URL);
        body.put("goodsInfo", buildGoods("拖鞋", 1));
        post("TC-CREATE-18 [接力送-缺hotelId → 0001]", CREATE_TASK_URL, body, true, true);
    }

    /**
     * TC-CREATE-19 货柜送物/接力送，缺少必填 goodsInfo
     * 预期：errcode=0001（业务失败）
     */
    static void testCreate_relayDelivery_missingGoodsInfo() {
        JSONObject body = new JSONObject();
        body.put("requestId", uuid());
        body.put("hotelId", HOTEL_ID);
        body.put("taskType", "delivery");
        body.put("target", "808");
        body.put("callbackUrl", CALLBACK_URL);
        post("TC-CREATE-19 [接力送-缺goodsInfo → 0001]", CREATE_TASK_URL, body, true, true);
    }

    /**
     * TC-CREATE-20 货柜送物/接力送，goodsInfo中缺少必填 name
     * 预期：errcode=0001（业务失败）
     */
    static void testCreate_relayDelivery_goodsMissingName() {
        JSONObject body = new JSONObject();
        body.put("requestId", uuid());
        body.put("hotelId", HOTEL_ID);
        body.put("taskType", "delivery");
        body.put("target", "808");
        body.put("callbackUrl", CALLBACK_URL);
        JSONArray goods = new JSONArray();
        JSONObject g = new JSONObject();
        g.put("count", 1); // 无name
        goods.add(g);
        body.put("goodsInfo", goods);
        post("TC-CREATE-20 [接力送-goods缺name → 0001]", CREATE_TASK_URL, body, true, true);
    }

    // ==================== 取消任务 ====================

    /**
     * TC-CANCEL-01 正常取消，autoBack=true（默认，取消后机器人自动返回充电桩）
     * 前置：机器人正在执行任务（可先跑TC-CREATE-02创建任务）
     * 预期：errcode=0000，result.success=true
     */
    static void testCancel_normal_autoBackTrue() {
        JSONObject body = new JSONObject();
        body.put("requestId", uuid());
        body.put("robotId", ROBOT_ID);
        body.put("autoBack", true);
        post("TC-CANCEL-01 [正常取消-autoBack=true]", CANCEL_TASK_URL, body, true, true);
    }

    /**
     * TC-CANCEL-02 取消后停在原地，autoBack=false
     * 前置：机器人正在执行任务
     * 预期：errcode=0000，result.success=true，机器人停止在当前位置不回桩
     */
    static void testCancel_autoBackFalse() {
        JSONObject body = new JSONObject();
        body.put("requestId", uuid());
        body.put("robotId", ROBOT_ID);
        body.put("autoBack", false);
        post("TC-CANCEL-02 [正常取消-autoBack=false停原地]", CANCEL_TASK_URL, body, true, true);
    }

    /**
     * TC-CANCEL-03 指定boxId取消特定仓任务（多仓场景）
     * 前置：机器人正在执行多仓送物任务（如TC-CREATE-11创建的任务）
     * 预期：errcode=0000，仅取消指定仓的任务，其余仓任务继续执行
     */
    static void testCancel_withBoxId() {
        JSONObject body = new JSONObject();
        body.put("requestId", uuid());
        body.put("robotId", ROBOT_ID);
        body.put("boxId", "1");
        body.put("autoBack", true);
        post("TC-CANCEL-03 [取消-指定boxId]", CANCEL_TASK_URL, body, true, true);
    }

    /**
     * TC-CANCEL-04 指定target取消（文档v1.0新增字段）
     * 前置：机器人正在执行任务
     * 预期：errcode=0000
     */
    static void testCancel_withTarget() {
        JSONObject body = new JSONObject();
        body.put("requestId", uuid());
        body.put("robotId", ROBOT_ID);
        body.put("target", TARGET);
        post("TC-CANCEL-04 [取消-指定target]", CANCEL_TASK_URL, body, true, true);
    }

    /**
     * TC-CANCEL-05 同时指定boxId和target
     * 前置：机器人执行多目标任务
     * 预期：errcode=0000，精准取消指定仓+目标的任务
     */
    static void testCancel_withBoxIdAndTarget() {
        JSONObject body = new JSONObject();
        body.put("requestId", uuid());
        body.put("robotId", ROBOT_ID);
        body.put("boxId", "1");
        body.put("target", TARGET);
        body.put("autoBack", false);
        post("TC-CANCEL-05 [取消-boxId+target]", CANCEL_TASK_URL, body, true, true);
    }

    /**
     * TC-CANCEL-06 缺少必填 requestId
     * 预期：errcode=0001（业务失败）
     */
    static void testCancel_missingRequestId() {
        JSONObject body = new JSONObject();
        body.put("robotId", ROBOT_ID);
        post("TC-CANCEL-06 [取消-缺requestId → 0001]", CANCEL_TASK_URL, body, true, true);
    }

    /**
     * TC-CANCEL-07 缺少必填 robotId
     * 预期：errcode=0001（业务失败）
     */
    static void testCancel_missingRobotId() {
        JSONObject body = new JSONObject();
        body.put("requestId", uuid());
        post("TC-CANCEL-07 [取消-缺robotId → 0001]", CANCEL_TASK_URL, body, true, true);
    }

    /**
     * TC-CANCEL-08 robotId不存在
     * 预期：errcode=0001（业务失败，任务不存在）
     */
    static void testCancel_robotIdNotExist() {
        JSONObject body = new JSONObject();
        body.put("requestId", uuid());
        body.put("robotId", "ROBOT_NOT_EXIST_99999");
        post("TC-CANCEL-08 [取消-robotId不存在 → 0001]", CANCEL_TASK_URL, body, true, true);
    }

    /**
     * TC-CANCEL-09 缺少Header syspipe
     * 预期：errcode=0001 或网关报错
     */
    static void testCancel_missingSyspipe() {
        JSONObject body = new JSONObject();
        body.put("requestId", uuid());
        body.put("robotId", ROBOT_ID);
        post("TC-CANCEL-09 [取消-缺syspipe]", CANCEL_TASK_URL, body, false, true);
    }

    /**
     * TC-CANCEL-10 Authorization签名错误
     * 预期：errcode=0006（鉴权失败）
     */
    static void testCancel_wrongSignature() {
        JSONObject body = new JSONObject();
        body.put("requestId", uuid());
        body.put("robotId", ROBOT_ID);
        post("TC-CANCEL-10 [取消-错误签名 → 0006]", CANCEL_TASK_URL, body, true, false);
    }

    // ==================== HTTP执行 ====================

    /**
     * 发送 POST 请求，自动组装完整调用规范的 Header
     *
     * @param withSyspipe 是否携带 syspipe Header
     * @param validSign   true=正常签名，false=使用错误签名（测试0006鉴权失败）
     */
    private static void post(String caseName, String url, JSONObject body,
                             boolean withSyspipe, boolean validSign) {
        String bodyStr = body.toJSONString();
        // ReqId：32位UUID（去横线）
        String reqId = UUID.randomUUID().toString().replace("-", "");
        // Date：GMT格式
        String date = ZonedDateTime.now(ZoneOffset.UTC).format(GMT_FMT);

        try {
            // Digest = "SHA-256=" + base64(HMAC-SHA256(body, secret))
            String digest = "SHA-256=" + hmacBase64(bodyStr, HMAC_SECRET);

            // Authorization 签名字符串（header名全小写，值保持原样）
            String signingStr = "reqid: " + reqId + "\n"
                    + "reqfrom: " + REQ_FROM + "\n"
                    + "digest: " + digest + "\n"
                    + "date: " + date;
            String signature = validSign
                    ? hmacBase64(signingStr, HMAC_SECRET)
                    : "invalid_signature_xxxx";
            String authorization = "hmac username=\"" + HMAC_USERNAME + "\","
                    + " algorithm=\"hmac-sha256\","
                    + " headers=\"reqid reqfrom digest date\","
                    + " signature=\"" + signature + "\"";

            Request.Builder builder = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create(bodyStr, JSON_TYPE))
                    .header("Content-Type", "application/json")
                    .header("ReqId", reqId)
                    .header("ReqFrom", REQ_FROM)
                    .header("Date", date)
                    .header("Digest", digest)
                    .header("Authorization", authorization);

            if (withSyspipe) {
                builder.header("syspipe", SYSPIPE);
            }

            try (Response resp = CLIENT.newCall(builder.build()).execute()) {
                String respBody = resp.body() != null ? resp.body().string() : "";
                String errcode = parseErrcode(respBody);
                log.info("{} → HTTP {} errcode={} body={}", caseName, resp.code(), errcode, respBody);
            }
        } catch (Exception e) {
            log.error("{} → 请求异常: {}", caseName, e.getMessage());
        }
    }

    // ==================== 工具方法 ====================

    /** HMAC-SHA256 签名，返回 Base64 字符串 */
    private static String hmacBase64(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return Base64.getEncoder().encodeToString(
                mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }

    /** 从响应体解析 errcode */
    private static String parseErrcode(String respBody) {
        try {
            return JSON.parseObject(respBody).getString("errcode");
        } catch (Exception e) {
            return "-";
        }
    }

    /** 生成32位UUID（去横线）*/
    private static String uuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /** 构建 itemParams 中的一个 item */
    private static JSONObject buildItem(int doorId, String to) {
        JSONObject item = new JSONObject();
        item.put("doorId", doorId);
        item.put("to", to);
        return item;
    }

    /** 构建 goodsInfo 数组（单品）*/
    private static JSONArray buildGoods(String name, int count) {
        JSONArray goods = new JSONArray();
        JSONObject g = new JSONObject();
        g.put("name", name);
        g.put("count", count);
        goods.add(g);
        return goods;
    }
}
