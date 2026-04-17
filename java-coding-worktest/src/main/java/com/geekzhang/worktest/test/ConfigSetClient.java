package com.geekzhang.worktest.test;

import com.geekzhang.worktest.workutil.util.DeviceIdGenerator;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * configset 接口调用示例
 *
 * 对应 curl：
 *   curl -X POST "http://59.110.175.77:4877/configset?id=34638044&db=fm_dev&product=i0m7VIQh7mG
 *                 &record_ed=36000&appkey=1&timestamp=0&sign=0"
 *
 * sign 生成：DeviceIdGenerator.generateSign()
 *   - 所有业务参数（不含 sign 本身）按 key 排序拼接
 *   - 末尾追加 secret=YOUR_SECRET
 *   - MD5 大写
 */
@Slf4j
public class ConfigSetClient {

    private static final String BASE_URL = "http://59.110.175.77:4877/configset";

    public static void main(String[] args) throws Exception {
        // 业务参数（不含 timestamp、sign，由 generateSign 内部填充 timestamp）
        Map<String, String> params = new HashMap<>();
        params.put("id",        "34641630");
        params.put("db",        "bjyj_dev");
        params.put("product",   "i0m7S3JQXku");
        params.put("warn_st", "6:00");
        params.put("warn_ed", "22:00");
        params.put("appkey",    "78e51369d811632f0dc13e45c11afe07");

        // 生成 sign，同时会向 params 中写入 timestamp
        String sign = DeviceIdGenerator.generateSign(params);
        params.put("sign", sign);

        String result = doPost(BASE_URL, params);
        log.info("configset response: {}", result);
    }

    /**
     * 拼接参数到 URL 后发 POST 请求（无 body，参数全在 query string）
     */
    private static String doPost(String baseUrl, Map<String, String> params) throws Exception {
        StringBuilder query = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (query.length() > 0) query.append("&");
            query.append(entry.getKey()).append("=").append(entry.getValue());
        }

        URL url = new URL(baseUrl + "?" + query);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        conn.setDoInput(true);
        conn.connect();

        int code = conn.getResponseCode();
        InputStream is = code == 200 ? conn.getInputStream() : conn.getErrorStream();
        StringBuilder sb = new StringBuilder();
        byte[] buf = new byte[1024];
        int len;
        while ((len = is.read(buf)) != -1) {
            sb.append(new String(buf, 0, len, "UTF-8"));
        }
        is.close();
        conn.disconnect();

        log.info("HTTP {}", code);
        return sb.toString();
    }
}
