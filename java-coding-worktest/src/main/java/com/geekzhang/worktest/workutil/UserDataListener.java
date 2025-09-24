package com.geekzhang.worktest.workutil;



import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.geekzhang.worktest.workutil.dto.Device;
import com.geekzhang.worktest.workutil.dto.DeviceInfo;
import com.geekzhang.worktest.workutil.dto.HdosStore;
import com.geekzhang.worktest.workutil.dto.MeituanStoreInfo;
import com.geekzhang.worktest.workutil.dto.MeituanStoreInfoRegister;
import com.geekzhang.worktest.workutil.dto.MethodDto;
import com.geekzhang.worktest.workutil.dto.PlaceInfo;
import com.geekzhang.worktest.workutil.util.JsonFileUtil;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author zwm
 * @desc UserDataListener
 * @date 2023年12月22日 14:31
 */
@Slf4j
public class UserDataListener extends AnalysisEventListener<MeituanStoreInfo> {
    private Map<String,HdosStore> methodDtos = new HashMap<>();
    private List<MeituanStoreInfo> oldMethodDtos = new ArrayList<>();
    private Map<String,String> map = new HashMap<>();
    @Override
    public void invoke(MeituanStoreInfo data, AnalysisContext context) {
        // 处理每一行数据
        oldMethodDtos.add(data);

        // 在这里可以将数据保存到数据库等业务逻辑
    }
    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {



//        System.out.println(sb.toString());
//
//        System.out.println(JSON.toJSONString(ids));


        int threadNum = 10; // 可根据CPU和接口QPS调整
        ExecutorService executor = Executors.newFixedThreadPool(threadNum);

        List<Future<?>> futures = new ArrayList<>();

        OkHttpClient client = new OkHttpClient().newBuilder().build();
        MediaType mediaType = MediaType.parse("application/json");
        ObjectMapper mapper = new ObjectMapper();

        List<MeituanStoreInfoRegister> meituanStoreInfoRegisters = new ArrayList<>();

        for (MeituanStoreInfo methodDto : oldMethodDtos) {
            if(StringUtils.isNotBlank(methodDto.getPlaceId())){
                MeituanStoreInfoRegister meituanStoreInfoRegister = new MeituanStoreInfoRegister();


                BeanUtils.copyProperties(methodDto, meituanStoreInfoRegister);
                meituanStoreInfoRegister.setType(methodDto.getRobotId().startsWith("G") ? "GEGE" : "NVWA");
                meituanStoreInfoRegister.setModel(methodDto.getRobotId().startsWith("G") ? "GEGE" : "NVWA");
                meituanStoreInfoRegister.setModelName(methodDto.getRobotId().startsWith("G") ? "格格" : "润");
                meituanStoreInfoRegister.setCabNum(methodDto.getRobotId().startsWith("G") ? 2 : 1);

                String deviceId  = StringUtils.right(methodDto.getRobotId(), 6);
                meituanStoreInfoRegister.setRobotName(deviceId);
                meituanStoreInfoRegister.setSn(methodDto.getRobotId());
                meituanStoreInfoRegister.setRobotId(methodDto.getRobotId().startsWith("G") ? methodDto.getChassisId() : methodDto.getRobotId());
                meituanStoreInfoRegister.setServicePhone("400-608-0917");
                meituanStoreInfoRegister.setKeyword( methodDto.getStoreName()+","+methodDto.getAddress());

                if(StringUtils.isNotBlank(methodDto.getCoordinates()) && methodDto.getCoordinates().split(",").length == 2){
                    meituanStoreInfoRegister.setLatitude( methodDto.getCoordinates().split(",")[0]);
                    meituanStoreInfoRegister.setLongitude( methodDto.getCoordinates().split(",")[1]);
                }
                meituanStoreInfoRegisters.add(meituanStoreInfoRegister);
            }


        }



        List<Map<String, Object>> mustList = new ArrayList<>();

        JsonFileUtil.writeListToJsonWithFastJson(meituanStoreInfoRegisters, "/Users/admin/Downloads/data/meituanStoreInfoRegisters.json");

        List<String> errors = new ArrayList<>();

//        meituanStoreInfoRegisters.forEach(ele->{
//            try {
//
//
//                RequestBody body = RequestBody.create(mediaType, JSON.toJSONString(ele));
//
//                Request request = new Request.Builder()
//                        .url("https://wwww.com.cn/api/v4/place/meituan/robot/register")
//                        .method("POST", body)
//                        .addHeader("Content-Type", "application/json")
//                        .addHeader("token", "867676a4b219438893dbf1dec4653f4f")
//                        .build();
//                Response response = client.newCall(request).execute();
//
//                if (response.code() == 200 && response.body() != null) {
//
//                    Map<String, Object> map = mapper.readValue(response.body().byteStream(), Map.class);
//                    if (!map.get("errcode").equals(0)) {
//                        errors.add(ele.getRobotId());
//                        log.error("map:{}",JSON.toJSONString(map));
//                    }
//                    log.error("map:{}",JSON.toJSONString(map));
//                }
//            } catch (Exception e) {
//                errors.add(ele.getRobotId());
//                log.error("meituanStoreInfoRegisters.forEach robotId:{}",ele.getRobotId());
//                e.printStackTrace();
//            }
//
//        });


        List<String> robotIds = new ArrayList<>();

        // 使用 try-with-resources 确保资源释放
//        try (AsyncHttpProcessor processor = new AsyncHttpProcessor()) {
//            CompletableFuture<AsyncHttpProcessor.ProcessResult> future = processor.processAsync(meituanStoreInfoRegisters);
//
//            AsyncHttpProcessor.ProcessResult result = future.get(30, TimeUnit.MINUTES); // 设置超时
//
//            log.info("处理结果: {}", JSON.toJSONString(result));
//            if (!result.getErrors().isEmpty()) {
//                log.warn("失败的robotId: {}", result.getErrors());
//            }
//            if (CollectionUtils.isNotEmpty(result.getErrors())){
//                robotIds.addAll(result.getErrors());
//            }
//
//        } catch (Exception e) {
//            log.error("处理失败", e);
//        }


        log.error("errors------list:{}",JSON.toJSONString(errors));

//        lists.forEach(list->{
//            Future<?> future = executor.submit(() -> {
//
//
//            try {
//                // must
//                Map<String, Object> termsValue = new HashMap<>();
//                termsValue.put("iccid.keyword", list);
//
//                Map<String, Object> terms = new HashMap<>();
//                terms.put("terms", termsValue);
//
//
//                mustList.add(terms);
//
//
//                // bool query
//                Map<String, Object> bool = new HashMap<>();
//                bool.put("must", mustList);
//
//
//                Map<String, Object> query = new HashMap<>();
//                query.put("bool", bool);
//
//                Map<String, Object> root = new HashMap<>();
//                root.put("query", query);
//
//                String jsonBody = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
//
//                RequestBody body = RequestBody.create(mediaType, jsonBody);
//
//                Request request = new Request.Builder()
//                        .url("https://wwww.com.cn/device.info.*/_search")
//                        .method("POST", body)
//                        .addHeader("Content-Type", "application/json")
//                        .addHeader("Authorization", "Basic YWRtaW46eXVuMTdqaTE4")
//                        .build();
//                Response response = client.newCall(request).execute();
//
//                if (response.code() == 200 && response.body() != null) {
//                    JSONObject esRes = JSON.parseObject(response.body().string());
//                    if (esRes != null && esRes.getJSONObject("hits") != null && esRes.getJSONObject("hits").getJSONArray("hits") != null) {
//                        JSONArray devices = esRes.getJSONObject("hits").getJSONArray("hits");
//                        if (devices.size() > 0) {
//                            JSONObject source = devices.getJSONObject(0).getJSONObject("_source");
//                            String placeId = source.getString("placeId");
//                            String hotelId = source.getString("hotelId");
//                            String placeName = source.getString("placeName");
//                            Long ts = source.getLong("ts");
////                                    methodDto.setTs(DateUtils.timestampToString(ts));
//                            System.out.println(placeId + " " + placeName + " " + hotelId + " " + ts);
//
//                        }
//                    }
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//
//
//
//            });
//
//
//            futures.add(future);
//
//        });







//        // 等待所有任务完成
//        for (Future<?> future : futures) {
//            try {
//                future.get(); // 可加超时参数
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//
//        executor.shutdown();
        List<DeviceInfo> deviceInfos = new ArrayList<>();

        if(CollectionUtils.isNotEmpty(robotIds)){
         robotIds.forEach(robotId->{
             DeviceInfo deviceInfo = new DeviceInfo();
             deviceInfo.setProductId(robotId);
             deviceInfos.add(deviceInfo);
         });
        }


        // 写入Excel
        EasyExcel.write("/Users/admin/Downloads/data/error_group_info.xlsx", DeviceInfo.class).sheet("模板").doWrite(deviceInfos);

        System.out.println("--------------------end--------------------");
    }
    public static float calcBoost(String field, String value, boolean isKeyword) {
        int len = value != null ? value.length() : 0;

        // 精确匹配（keyword）权重高，模糊匹配次之，地址最低
        if ("placeName.keyword".equals(field)) {
            if (len < 8) return 6f;
            if (len < 15) return 4f;
            return 6f;
        }
        if ("placeName".equals(field)) {
            if (len < 8) return 3f;
            if (len < 15) return 2f;
            return 1.5f;
        }
        if (field.contains("address")) {
            if (isKeyword) return 0.5f;
            return 0.8f;
        }
        // 其他字段
        return 1.0f;
    }
    public void doAfterAllAnalysed1(AnalysisContext context) {
        //原子类型


        List<Integer> count = new ArrayList<>();
        oldMethodDtos.stream().forEach(methodDto -> {
            try {



                //                DocumentContext context = JsonPath.parse(jsonString);

                // 修改 JSON 数据
//                context.set("$.query.bool.must[0].match.hardwareVersion","2.0");
//                context.set("$.query.bool.must[0].match.type","PHONE");
//                // 获取修改后的 JSON 数据字符串
//                String updatedJson = context.jsonString();

                // 打印修改后的 JSON 数据
//                System.out.println(updatedJson);



                OkHttpClient client = new OkHttpClient().newBuilder()
                        .build();
                MediaType mediaType = MediaType.parse("application/json");

                Map<String, Object> mustTermValue = new HashMap<>();
//                mustTermValue.put("dummyCityName.keyword", methodDto.getCity()+"市");
                Map<String, Object> mustTerm = new HashMap<>();
                mustTerm.put("term", mustTermValue);
                List<Map<String, Object>> mustList = new ArrayList<>();
                mustList.add(mustTerm);

                // should - term with boost
                Map<String, Object> shouldTermValue = new HashMap<>();
//                shouldTermValue.put("value", methodDto.getCityName());
                shouldTermValue.put("boost", 5);
                Map<String, Object> shouldTermField = new HashMap<>();
                shouldTermField.put("placeName.keyword", shouldTermValue);
                Map<String, Object> shouldTerm = new HashMap<>();
                shouldTerm.put("term", shouldTermField);

                // should - match with boost
                Map<String, Object> shouldMatchValue = new HashMap<>();
//                shouldMatchValue.put("query", methodDto.getName());
                shouldMatchValue.put("boost", 2);
                Map<String, Object> shouldMatchField = new HashMap<>();
                shouldMatchField.put("placeName", shouldMatchValue);
                Map<String, Object> shouldMatch = new HashMap<>();
                shouldMatch.put("match", shouldMatchField);

                // should list
                List<Map<String, Object>> shouldList = new ArrayList<>();
                shouldList.add(shouldTerm);
                shouldList.add(shouldMatch);

                // bool query
                Map<String, Object> bool = new HashMap<>();
                bool.put("must", mustList);
                bool.put("should", shouldList);
                bool.put("minimum_should_match", 1);

                Map<String, Object> query = new HashMap<>();
                query.put("bool", bool);

                Map<String, Object> root = new HashMap<>();
                root.put("query", query);

                // 转为JSON字符串
                ObjectMapper mapper = new ObjectMapper();


                // 组装查询
                String jsonBody = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);

// 用于RequestBody
                RequestBody body = RequestBody.create(mediaType, jsonBody);

                Request request = new Request.Builder()
                        .url("https://wwww.com.cn/place.info/_search")
                        .method("POST", body)
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Authorization", "Basic YWRtaW46eXVuMTdqaTE4")
                        .build();
                Response response = client.newCall(request).execute();

                count.add(1);
                if(response.code() == 200&&response.body() != null) {

                    JSONObject esRes = JSON.parseObject(response.body().string());
                    if (esRes != null && esRes.getJSONObject("hits") != null && esRes.getJSONObject("hits").getJSONArray("hits") != null) {
                        JSONArray devices = esRes.getJSONObject("hits").getJSONArray("hits");
                        if(devices.size() > 0){

                            Float score = devices.getJSONObject(0).getFloatValue("_score");
                            if(score>=50){
                                JSONObject source = devices.getJSONObject(0).getJSONObject("_source");
                                String placeId = source.getString("placeId");
                                String address = source.getString("address");
                                String placeName = source.getString("placeName");

//                                methodDto.setPlaceId(placeId);
//                                methodDto.setPlaceName(placeName);
//                                methodDto.setAddressDetail(address);
                            }

                        }


                    }
                }
                System.out.println("----------------next_line----------------"+count.size());
                // do something with line
            } catch (Exception e){
                e.printStackTrace();
            }
        });


        // 这里 需要指定写用哪个class去读，然后写到第一个sheet，名字为模板 然后文件流会自动关闭
        // 如果这里想使用03 则 传入excelType参数即可
        EasyExcel.write("/Users/admin/Downloads/place_info.xlsx",PlaceInfo.class).sheet("模板").doWrite(oldMethodDtos);

        System.out.println("--------------------end--------------------");

        // 所有数据读取完成后的操作，可以在这里进行一些清理工作
    }

    public static String buildEsQuery(Map<String, Object> mustTerms, List<Map<String, Object>> shoulds, int minimumShouldMatch) {
        try {
            // 构建 must 数组
            List<Map<String, Object>> mustList = new ArrayList<>();
            if (mustTerms != null) {
                for (Map.Entry<String, Object> entry : mustTerms.entrySet()) {
                    Map<String, Object> termValue = new HashMap<>();
                    termValue.put(entry.getKey(), entry.getValue());
                    Map<String, Object> term = new HashMap<>();
                    term.put("term", termValue);
                    mustList.add(term);
                }
            }

            // 构建 bool 查询体
            Map<String, Object> bool = new HashMap<>();
            if (!mustList.isEmpty()) {
                bool.put("must", mustList);
            }
            if (shoulds != null && !shoulds.isEmpty()) {
                bool.put("should", shoulds);
            }
            bool.put("minimum_should_match", minimumShouldMatch);

            // 外层 query
            Map<String, Object> query = new HashMap<>();
            query.put("bool", bool);

            Map<String, Object> root = new HashMap<>();
            root.put("query", query);

            // 转 JSON
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch (Exception e) {
            throw new RuntimeException("构建ES查询JSON失败", e);
        }
    }
}
