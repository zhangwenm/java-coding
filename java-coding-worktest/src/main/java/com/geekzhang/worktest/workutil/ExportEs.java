package com.geekzhang.worktest.workutil;

import com.alibaba.excel.EasyExcel;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.geekzhang.worktest.workutil.dto.AllDataStatisticsResponse;
import com.geekzhang.worktest.workutil.dto.Brand;
import com.geekzhang.worktest.workutil.dto.HdosStore;
import com.geekzhang.worktest.workutil.dto.Province;
import com.geekzhang.worktest.workutil.dto.StoreInfo;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.io.LineIterator;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * @author zwm
 * @desc ExportEs
 * @date 2023年09月04日 11:12
 */
@Slf4j
public class ExportEs {
    public static void main(String[] args) throws IOException {

//        System.out.println( 9/10 +"%");

        InputStream resourceAsStream0 = Thread.currentThread().getContextClassLoader().getResourceAsStream("iccid.json");
        JSONArray store = JSON.parseObject(resourceAsStream0 , JSONArray.class);

        List<AllDataStatisticsResponse> resList = new ArrayList<>();

        for (int i = 0; i <store.size() ; i++) {
            JSONObject j = store.getJSONObject(i);

            String iccid = j.getString("_id");
            System.out.println(iccid);
        }


//        store.forEach(item->{
//            JSONObject it = item.get;
//            try {
//
//
//
//                String url ="?startDate=1739894400000&endDate=1740499199999&userId=YJ_CUSCiMoQj3r&Id=&areaId=1344285221922672640&province&brandId&groupId=10461&signature=S5YqYmCD5DAMVjsFjS%2BBJkCfGy8%3D&accessKeyId=cfa78fa01fbe1020&timestamp=2025-02-27T07%3A27%3A21.752Z&signatureNonce=790339835&storeId="+item.getId();
//
//                String preUrl1 =
//                        "https://www.cn/work/v2/group/statistics/integral";
//                String url1 = preUrl1+url;
//
//                AllDataStatisticsResponse res = new AllDataStatisticsResponse();
//
//                res.setName(item.getName());
//
//                if(brandMap.containsKey(Integer.parseInt(item.getIotBrandId()))){
//                    res.setBrand(brandMap.get(Integer.parseInt(item.getIotBrandId())).getBrandName());
//                }
//
//                if(provinceMap.containsKey( item.getProvince())){
//                    res.setProvince(provinceMap.get( item.getProvince()).getLabel());
//                }
//
//
//                OkHttpClient client1 = new OkHttpClient().newBuilder()
//                        .build();
//                MediaType mediaType = MediaType.parse("text/plain");
//                //Ai门店  清洁
//                Request request1 = new Request.Builder()
//                        .url(url1)
//                        .get()
//                        .addHeader("token", "70c2dad5-8360-40d0-8b40-1d2850d481ab")
//                        .build();
//                Response response1 = client1.newCall(request1).execute();
//
//                String  res1 = response1.body().string();
//                System.out.println("res1:"+res1);
//
//
//                JSONObject jsonObject1 = JSONObject.parseObject(res1);
//
//
//                JSONObject jsonObjectRes1 = jsonObject1.getJSONObject("data");
//                res.setAiDuration(jsonObjectRes1.getBigDecimal("aiDuration"));
//                res.setAiStoreCount(jsonObjectRes1.getLong("aiStoreCount"));
//                res.setCleanStoreCount(jsonObjectRes1.getLong("cleanStoreCount"));
//
//                res.setDeliveryCount(jsonObjectRes1.getInteger("deliveryCount"));
//                res.setDeliveryDuration(jsonObjectRes1.getBigDecimal("deliveryDuration"));
//
//                res.setSweepArea(jsonObjectRes1.getBigDecimal("sweepArea"));
//                res.setSweepDuration(jsonObjectRes1.getBigDecimal("sweepDuration"));
//
//
//                //ai电话
//                OkHttpClient client2 = new OkHttpClient().newBuilder()
//                        .build();
//                String preUrl2 =
//                        "https://wwww.cn/work/v2/group/statistics/ai";
//                String url2 = preUrl2+url;
//                        Request request2 = new Request.Builder()
//                        .url(url2)                    .get()
//                        .addHeader("token", "70c2dad5-8360-40d0-8b40-1d2850d481ab")
//                        .build();
//                Response response2 = client2.newCall(request2).execute();
//
//                String  res2 = response2.body().string();
//                System.out.println("res2:"+res2);
//
//
//                JSONObject jsonObject2 = JSONObject.parseObject(res2);
//
//
//                JSONObject jsonObjectRes2 = jsonObject2.getJSONObject("data");
//
//                res.setAiTotal(jsonObjectRes2.getBigDecimal("total"));
//                res.setUndertake(jsonObjectRes2.getString("undertake"));
//
//
//                //工单
//                OkHttpClient client3 = new OkHttpClient().newBuilder()
//                        .build();
//
//                String preUrl3 =
//                        "https://wwww.cn/work/v2/group/statistics/requirementWarnningService/distributions";
//
//                String url3 = preUrl3+url;
//                Request request3 = new Request.Builder().url(url3)
//                        .get()
//                        .addHeader("token", "70c2dad5-8360-40d0-8b40-1d2850d481ab")
//                        .build();
//                Response response3 = client3.newCall(request3).execute();
//
//                String  res3 = response3.body().string();
//                System.out.println("res3:"+res3);
//
//                JSONObject jsonObject3 = JSONObject.parseObject(res3);
//
//
//                JSONObject jsonObjectRes3 = jsonObject3.getJSONObject("data");
//                res.setWarnWorkCount(jsonObjectRes3.getInteger("totalCount"));
//
//                res.setWarnWorkCount60(jsonObjectRes3.getInteger("totalLimit60Count") );
//
//                //客诉
//                OkHttpClient client4 = new OkHttpClient().newBuilder()
//                        .build();
//                String preUrl4 =
//                        "https://wwww.cn/work/v2/group/statistics/requirementService/distributions";
//
//                String url4 = preUrl4+url;
//                Request request4 = new Request.Builder().url(url4)
//                        .get()
//                        .addHeader("token", "70c2dad5-8360-40d0-8b40-1d2850d481ab")
//                        .build();
//                Response response4 = client4.newCall(request4).execute();
//
//                String  res4 = response4.body().string();
//                System.out.println("res4:"+res4);
//                JSONObject jsonObject4 = JSONObject.parseObject(res4);
//
//
//                JSONArray jsonObjectRes4 = jsonObject4.getJSONObject("data").getJSONArray("distributionStatisticsVOList");
//
//
//                for (int i = 0; i < jsonObjectRes4.size(); i++) {
//                    JSONObject jsonObjectRes41 = jsonObjectRes4.getJSONObject(i);
//
//                    Integer group = jsonObjectRes41.getInteger("groupId");
//
//                    if(group == 2){
//                        res.setDeliveryWorkCount(jsonObjectRes41.getInteger("totalCount"));
//                        res.setDeliveryWorkCount15(jsonObjectRes41.getInteger("limit15Count") );
//                    }
//                    if(group == 3){
//                        res.setRepairWorkCount(jsonObjectRes41.getInteger("totalCount"));
//                        res.setRepairWorkCount15(jsonObjectRes41.getInteger("limit15Count") );
//                    }
//                }
//
//
//                resList.add(res);
//
//                System.out.println("end" );
//            } catch (Exception e){
//                e.printStackTrace();
//            } finally {
//                LineIterator.closeQuietly(it);
//            }
//        });
//        // 这里 需要指定写用哪个class去读，然后写到第一个sheet，名字为模板 然后文件流会自动关闭
//        // 如果这里想使用03 则 传入excelType参数即可
//        EasyExcel.write("/Users/admin/Downloads/dev/hdos2.xlsx", HdosStore.class).sheet("模板").doWrite(resList);
    }
}
