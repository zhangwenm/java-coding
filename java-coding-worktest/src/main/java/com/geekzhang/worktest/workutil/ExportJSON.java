package com.geekzhang.worktest.workutil;

import com.alibaba.excel.EasyExcel;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.geekzhang.worktest.workutil.dto.Device;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.LineIterator;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


/**
 * @author zwm
 * @desc ExportEs
 * @date 2023年09月04日 11:12
 */
@Slf4j
public class ExportJSON {
    public static void main(String[] args) throws IOException {

//        System.out.println( 9/10 +"%");

        InputStream resourceAsStream0 = Thread.currentThread().getContextClassLoader().getResourceAsStream("order3.json");
        JSONArray deviceList = JSON.parseObject(resourceAsStream0 , JSONArray.class);

        List<Device> resList = new ArrayList<>();

        deviceList.forEach(item->{
            LineIterator it = null;
            try {
                JSONObject jsonObject = (JSONObject)item;
                JSONObject res  =  ((JSONObject) item).getJSONObject("_source");

                Device device = new Device();
                device.setProductId(res.getString("id"));
                device.setPlaceId(res.getString("iccid"));
                device.setPlaceName(res.getString("placeName"));
                device.setType(res.getString("type"));
                resList.add(device);

                System.out.println("end" );
            } catch (Exception e){
                e.printStackTrace();
            } finally {
                LineIterator.closeQuietly(it);
            }
        });
        // 这里 需要指定写用哪个class去读，然后写到第一个sheet，名字为模板 然后文件流会自动关闭
        // 如果这里想使用03 则 传入excelType参数即可
        EasyExcel.write("/Users/admin/Downloads/data/device.xlsx", Device.class).sheet("模板").doWrite(resList);
        System.out.println("success");
    }
}
