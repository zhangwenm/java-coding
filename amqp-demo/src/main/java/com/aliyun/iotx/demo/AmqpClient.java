package com.aliyun.iotx.demo;

import java.net.URI;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.apache.commons.codec.binary.Base64;
import org.apache.qpid.jms.JmsConnection;
import org.apache.qpid.jms.JmsConnectionListener;
import org.apache.qpid.jms.message.JmsInboundMessageDispatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 健康监测设备 AMQP 云流转客户端
 *
 * 数据来源：阿里云 IoT AMQP 消息队列，设备实时上报健康监测数据（呼吸、心率、睡眠分期等）。
 * 对接协议文档：https://hqmwp7cauk.feishu.cn/wiki/AtVgwVK1Aih6zokwM7mc1PH7n4e
 *
 * 消息 content 字段为 JSON，status 数组中包含物模型数据，常见字段：
 *   H / HeartRate      - 心率（bpm，0~150）
 *   R / RespiratoryRate - 呼吸率（bpm，0~50）
 *   D                  - 睡眠分期（值余8：0初始化/1清醒/2REM/3浅睡/4深睡）
 *   E / A              - 呼吸暂停时长（秒，0表示未发生）
 *   O / People_flag    - 有无人（值余2：0无人/1有人）
 *   M / moving         - 体动（0无/1小体动/2大体动）
 *   S / Amp_value      - 雷达信号幅度（0-30差/30-70良/70-100优）
 *   B / RSSI           - 网络信号质量（-1000~0，越高越好，4G设备每分钟上报一次）
 *   I / id             - 设备ID（纯8位数字）
 *
 * Wi-Fi 与 4G-LTE 设备上报的 Topic 格式略有差异，具体见协议文档第二章。
 */
public class AmqpClient {
    private final static Logger logger = LoggerFactory.getLogger(AmqpClient.class);

    /**
     * 以下参数由厂商账户文档提供，对应协议文档第一章"（3）关于参数文档说明"中的参数表：
     *   accessKey      → ALIBABA_CLOUD_ACCESS_KEY_ID（User AccessKey）
     *   accessSecret   → ALIBABA_CLOUD_ACCESS_KEY_SECRET（User AccessSecret，不出现在请求参数中）
     *   consumerGroupId → ConsumerGroup ID（控制台消费组 ID）
     */
    private static String accessKey = System.getenv("ALIBABA_CLOUD_ACCESS_KEY_ID");
    private static String accessSecret = System.getenv("ALIBABA_CLOUD_ACCESS_KEY_SECRET");
    private static String consumerGroupId = "uvkPpaF8qXXrW2J2yeAf000110";

    // 阿里云 IoT 实例ID。2021-07-30 之前开通的公共实例填空字符串。
    private static String iotInstanceId = "iot-010a0clt";

    // 客户端ID，控制台消费组状态页"客户端ID"列会显示此值。
    // 建议用机器 UUID、MAC 地址、IP 等唯一标识，便于区分多个接收端。
    private static String clientId = "test2123";

    // AMQP 接入域名，固定值，不在账户文档中体现。格式：{iotInstanceId}.amqp.iothub.aliyuncs.com
    private static String host = "iot-010a0clt.amqp.iothub.aliyuncs.com";

    // 单进程启动的连接数。单连接消费速率有限，最大支持 64 个连接，建议每 500 QPS 增加一个连接。
    private static int connectionCount = 4;

    // 业务处理异步线程池：核心线程数=CPU核数，最大=2倍核数，队列容量50000。
    // onMessage 回调中将消息投递到此线程池，避免阻塞 SDK 的消息回调线程。
    private final static ExecutorService executorService = new ThreadPoolExecutor(
        Runtime.getRuntime().availableProcessors(),
        Runtime.getRuntime().availableProcessors() * 2, 60, TimeUnit.SECONDS,
        new LinkedBlockingQueue(50000));

    public static void main(String[] args) throws Exception {
        List<Connection> connections = new ArrayList<>();

        // 每个连接使用独立的 userName（含连接序号 i），避免 clientId 冲突导致 rebalance。
        for (int i = 0; i < connectionCount; i++) {
            long timeStamp = System.currentTimeMillis();
            // 签名方法：支持 hmacmd5、hmacsha1、hmacsha256，这里选用 hmacsha1。
            String signMethod = "hmacsha1";

            // userName 格式：{clientId}-{i}|authMode=aksign,signMethod=...,timestamp=...,authId=...,iotInstanceId=...,consumerGroupId=...|
            // 详见协议文档第一章 AMQP 客户端接入说明。
            String userName = clientId + "-" + i + "|authMode=aksign"
                + ",signMethod=" + signMethod
                + ",timestamp=" + timeStamp
                + ",authId=" + accessKey
                + ",iotInstanceId=" + iotInstanceId
                + ",consumerGroupId=" + consumerGroupId
                + "|";

            // password = HmacSHA1(authId={accessKey}&timestamp={timeStamp}, accessSecret) 后 Base64 编码。
            // accessSecret 只参与签名计算，不出现在请求参数中。
            String signContent = "authId=" + accessKey + "&timestamp=" + timeStamp;
            String password = doSign(signContent, accessSecret, signMethod);

            // failover 协议：连接断开后自动重连，重连延迟 30ms；amqp.idleTimeout=80000ms 为心跳超时。
            String connectionUrl = "failover:(amqps://" + host + ":5671?amqp.idleTimeout=80000)"
                + "?failover.reconnectDelay=30";

            Hashtable<String, String> hashtable = new Hashtable<>();
            hashtable.put("connectionfactory.SBCF", connectionUrl);
            hashtable.put("queue.QUEUE", "default");
            hashtable.put(Context.INITIAL_CONTEXT_FACTORY, "org.apache.qpid.jms.jndi.JmsInitialContextFactory");
            Context context = new InitialContext(hashtable);
            ConnectionFactory cf = (ConnectionFactory) context.lookup("SBCF");
            Destination queue = (Destination) context.lookup("QUEUE");

            Connection connection = cf.createConnection(userName, password);
            connections.add(connection);

            // 注册连接事件监听器，用于感知连接建立、中断、恢复等状态变化。
            ((JmsConnection) connection).addConnectionListener(myJmsConnectionListener);

            // Session.AUTO_ACKNOWLEDGE：SDK 自动 ACK（推荐）。
            // Session.CLIENT_ACKNOWLEDGE：需在 processMessage 中手动调用 message.acknowledge()。
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            connection.start();
            MessageConsumer consumer = session.createConsumer(queue);
            consumer.setMessageListener(messageListener);
        }

        logger.info("amqp client started, press Ctrl+C to stop");

        // 注册 JVM 退出钩子，Ctrl+C 时优雅关闭：先关连接，再等线程池处理完剩余消息。
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("shutting down...");
            connections.forEach(c -> {
                try { c.close(); } catch (JMSException e) { logger.error("failed to close connection", e); }
            });
            executorService.shutdown();
            try {
                if (executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                    logger.info("shutdown success");
                } else {
                    logger.warn("some messages not processed");
                }
            } catch (InterruptedException ignored) {}
        }));

        // 主线程永久阻塞，直到进程被终止（Ctrl+C 或 kill 信号触发 ShutdownHook）。
        Thread.currentThread().join();
    }

    /**
     * 消息监听器：收到消息后立即投递到异步线程池，onMessage 本身不做耗时操作。
     *
     * 注意：若 onMessage 阻塞，会影响 SDK 内部消息回调，导致消息积压或 ACK 超时。
     */
    private static MessageListener messageListener = new MessageListener() {
        @Override
        public void onMessage(final Message message) {
            try {
                // AUTO_ACKNOWLEDGE 模式下此处无需手动 ACK。
                // 若改为 CLIENT_ACKNOWLEDGE，需在 processMessage 中调用 message.acknowledge()。
                executorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        processMessage(message);
                    }
                });
            } catch (Exception e) {
                logger.error("submit task occurs exception ", e);
            }
        }
    };

    /**
     * 处理设备上报消息。
     *
     * 消息结构示例（content 字段，JSON 格式）：
     * <pre>
     * {
     *   "items": {
     *     "H":  { "value": 68,  "time": 1700000000000 },  // 心率（HeartRate），bpm
     *     "R":  { "value": 16,  "time": 1700000000000 },  // 呼吸率（RespiratoryRate），bpm
     *     "D":  { "value": 3,   "time": 1700000000000 },  // 睡眠分期，值余8：0初始化/1清醒/2REM/3浅睡/4深睡
     *     "E":  { "value": 0,   "time": 1700000000000 },  // 呼吸暂停时长，秒，0表示未发生
     *     "O":  { "value": 1,   "time": 1700000000000 },  // 有无人，值余2：0无人/1有人
     *     "M":  { "value": 0,   "time": 1700000000000 },  // 体动：0无/1小体动/2大体动
     *     "S":  { "value": 55,  "time": 1700000000000 },  // 雷达信号幅度：0-30差/30-70良/70-100优
     *     "B":  { "value": -65, "time": 1700000000000 },  // 网络信号质量（RSSI），4G设备每分钟上报一次
     *     "I":  { "value": "51416062", "time": 1700000000000 } // 设备ID（纯8位数字）
     *   },
     *   "productKey": "xxx",
     *   "deviceName": "xxx",
     *   "gmtCreate": 1700000000000
     * }
     * </pre>
     * Wi-Fi 与 4G-LTE 设备的 topic 格式略有不同，详见协议文档第二章第三节。
     */
    private static void processMessage(Message message) {
        try {
            byte[] body = message.getBody(byte[].class);
            String content = new String(body);
            // topic：区分 Wi-Fi 设备与 4G-LTE 设备，格式不同
            String topic = message.getStringProperty("topic");
            if(topic.contains("warn")){
                logger.info("");
            }
            String messageId = message.getStringProperty("messageId");
            // generateTime：消息在云端生成的时间戳（毫秒）
            long generateTime = message.getLongProperty("generateTime");
            logger.info("receive message"
                + ",\n topic = " + topic
                + ",\n messageId = " + messageId
                + ",\n generateTime = " + generateTime
                + ",\n content = " + content);
        } catch (Exception e) {
            logger.error("processMessage occurs error ", e);
        }
    }

    /**
     * AMQP 连接事件监听器，用于监控连接状态。
     * failover 协议会在断线后自动重连，onConnectionRestored 表示重连成功。
     */
    private static JmsConnectionListener myJmsConnectionListener = new JmsConnectionListener() {
        /** 连接成功建立。 */
        @Override
        public void onConnectionEstablished(URI remoteURI) {
            logger.info("onConnectionEstablished, remoteUri:{}", remoteURI);
        }

        /** 超过最大重试次数后连接最终失败，需人工介入排查。 */
        @Override
        public void onConnectionFailure(Throwable error) {
            logger.error("onConnectionFailure, {}", error.getMessage());
        }

        /** 连接中断（网络抖动等），failover 会自动尝试重连。 */
        @Override
        public void onConnectionInterrupted(URI remoteURI) {
            logger.info("onConnectionInterrupted, remoteUri:{}", remoteURI);
        }

        /** 断线后自动重连成功。 */
        @Override
        public void onConnectionRestored(URI remoteURI) {
            logger.info("onConnectionRestored, remoteUri:{}", remoteURI);
        }

        @Override
        public void onInboundMessage(JmsInboundMessageDispatch envelope) {}

        @Override
        public void onSessionClosed(Session session, Throwable cause) {}

        @Override
        public void onConsumerClosed(MessageConsumer consumer, Throwable cause) {}

        @Override
        public void onProducerClosed(MessageProducer producer, Throwable cause) {}
    };

    /**
     * 计算 AMQP 连接密码（password）。
     *
     * 签名算法：HmacSHA1(signContent, accessSecret)，结果 Base64 编码。
     * signContent 格式：authId={accessKey}&timestamp={timestamp}
     * accessSecret 始终保留在本地，不出现在任何请求参数中。
     */
    private static String doSign(String toSignString, String secret, String signMethod) throws Exception {
        SecretKeySpec signingKey = new SecretKeySpec(secret.getBytes(), signMethod);
        Mac mac = Mac.getInstance(signMethod);
        mac.init(signingKey);
        byte[] rawHmac = mac.doFinal(toSignString.getBytes());
        return Base64.encodeBase64String(rawHmac);
    }
}
