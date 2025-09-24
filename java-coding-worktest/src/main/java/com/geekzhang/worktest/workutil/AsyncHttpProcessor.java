package com.geekzhang.worktest.workutil;

/**
 * @author zwm
 * @desc AsyncHttpProcessor
 * @date 2025年09月23日 14:02
 */
import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.geekzhang.worktest.workutil.dto.MeituanStoreInfoRegister;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.compress.utils.Lists;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
public class AsyncHttpProcessor implements AutoCloseable {

    // 共享资源，避免重复创建
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ExecutorService executor;
    private final Semaphore semaphore;
    private final OkHttpClient httpClient;

    public AsyncHttpProcessor() {
        this.executor = Executors.newFixedThreadPool(10, r -> {
            Thread t = new Thread(r, "async-http-processor");
            t.setDaemon(true); // 设置为守护线程
            return t;
        });
        this.semaphore = new Semaphore(20);

        // 优化的 HTTP 客户端配置
        this.httpClient = new OkHttpClient.Builder()
                .connectionPool(new ConnectionPool(20, 5, TimeUnit.MINUTES))
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
    }

    public CompletableFuture<ProcessResult> processAsync(List<MeituanStoreInfoRegister> registers) {
        if (registers == null || registers.isEmpty()) {
            return CompletableFuture.completedFuture(new ProcessResult(0, Lists.newArrayList()));
        }

        List<CompletableFuture<String>> futures = registers.stream()
                .map(this::processRegisterAsync)
                .collect(Collectors.toList());

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    List<String> errors = futures.stream()
                            .map(CompletableFuture::join)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());

                    int successCount = registers.size() - errors.size();
                    log.info("处理完成 - 总数:{}, 成功:{}, 失败:{}", registers.size(), successCount, errors.size());

                    return new ProcessResult(successCount, errors);
                })
                .exceptionally(throwable -> {
                    log.error("批量处理异常", throwable);
                    return new ProcessResult(0, registers.stream()
                            .map(MeituanStoreInfoRegister::getRobotId)
                            .collect(Collectors.toList()));
                });
    }

    private CompletableFuture<String> processRegisterAsync(MeituanStoreInfoRegister ele) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                semaphore.acquire(); // 限流
                return processRegister(ele);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("线程中断 - robotId:{}", ele.getRobotId());
                return ele.getRobotId();
            } catch (Exception e) {
                log.error("处理异常 - robotId:{}, error:{}", ele.getRobotId(), e.getMessage());
                return ele.getRobotId();
            } finally {
                semaphore.release();
            }
        }, executor);
    }

    private String processRegister(MeituanStoreInfoRegister ele) throws IOException {
        RequestBody body = RequestBody.create(JSON_MEDIA_TYPE, JSON.toJSONString(ele));
        Request request = new Request.Builder()
                .url("https://wwww.com.cn/api/v4/place/meituan/robot/register")
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("token", "wewewe25f6500cbbc84709960a178389a052c3")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                Map<String, Object> map = OBJECT_MAPPER.readValue(response.body().byteStream(), Map.class);

                Object errcode = map.get("errcode");
                if (errcode != null && !errcode.equals(0)) {
                    log.error("API错误 - robotId:{}, response:{}", ele.getRobotId(), JSON.toJSONString(map));
                    return ele.getRobotId();
                }
                Object res = map.get("result");

                log.debug("成功处理 - robotId:{}", ele.getRobotId());
                return null; // 成功
            } else {
                log.error("HTTP错误 - robotId:{}, code:{}, message:{}",
                        ele.getRobotId(), response.code(), response.message());
                return ele.getRobotId();
            }
        }
    }

    @Override
    public void close() {
        log.info("关闭 AsyncHttpProcessor");

        // 优雅关闭线程池
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    log.warn("线程池未能正常关闭");
                }
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // 关闭 HTTP 客户端
        httpClient.dispatcher().executorService().shutdown();
        httpClient.connectionPool().evictAll();
    }

    public static class ProcessResult {
        private final int successCount;
        private final List<String> errors;

        public ProcessResult(int successCount, List<String> errors) {
            this.successCount = successCount;
            this.errors = errors;
        }

        public int getSuccessCount() {
            return successCount;
        }

        public List<String> getErrors() {
            return errors;
        }

        public int getErrorCount() {
            return errors.size();
        }

        public int getTotalCount() {
            return successCount + errors.size();
        }

        @Override
        public String toString() {
            return String.format("ProcessResult{成功=%d, 失败=%d, 总计=%d}",
                    successCount, errors.size(), getTotalCount());
        }
    }
}