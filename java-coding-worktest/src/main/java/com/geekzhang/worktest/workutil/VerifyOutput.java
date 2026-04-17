package com.geekzhang.worktest.workutil;

import com.alibaba.excel.EasyExcel;
import com.geekzhang.worktest.workutil.dto.DeviceInfoForMatch;
import com.geekzhang.worktest.workutil.dto.XiaodaiRegister;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 验证输出结果
 */
@Slf4j
public class VerifyOutput {

    public static void main(String[] args) {
        String outputPath = "src/main/resources/美团小袋注册_填充storeId.xlsx";
        String devicePath = "src/main/resources/t_device_info.xls";

        List<XiaodaiRegister> outputList = new ArrayList<>();
        EasyExcel.read(outputPath, XiaodaiRegister.class, new SimpleAnalysisEventListener<XiaodaiRegister>() {
            @Override
            public void invoke(XiaodaiRegister data, com.alibaba.excel.context.AnalysisContext context) {
                outputList.add(data);
            }

            @Override
            public void doAfterAllAnalysed(com.alibaba.excel.context.AnalysisContext context) {
            }
        }).sheet().doRead();

        List<DeviceInfoForMatch> deviceList = new ArrayList<>();
        EasyExcel.read(devicePath, DeviceInfoForMatch.class, new SimpleAnalysisEventListener<DeviceInfoForMatch>() {
            @Override
            public void invoke(DeviceInfoForMatch data, com.alibaba.excel.context.AnalysisContext context) {
                deviceList.add(data);
            }

            @Override
            public void doAfterAllAnalysed(com.alibaba.excel.context.AnalysisContext context) {
            }
        }).sheet().doRead();

        Map<String, String> deviceNameToStoreIdMap = new HashMap<>();
        for (DeviceInfoForMatch device : deviceList) {
            if (StringUtils.isNotBlank(device.getName()) && StringUtils.isNotBlank(device.getStoreId())) {
                deviceNameToStoreIdMap.put(device.getName().trim(), device.getStoreId().trim());
            }
        }

        int totalCount = outputList.size();
        int matchedCount = 0;
        int notMatchedCount = 0;
        int correctMatchCount = 0;
        int wrongMatchCount = 0;

        System.out.println("========== 前10条数据展示 ==========");
        System.out.println("序号\tProductId\tStoreId\t设备表StoreId\t省份\t城市\t状态");
        for (int i = 0; i < Math.min(10, outputList.size()); i++) {
            XiaodaiRegister data = outputList.get(i);
            String deviceStoreId = deviceNameToStoreIdMap.get(data.getProductId());
            String status = deviceStoreId != null && deviceStoreId.equals(data.getStoreId()) ? "✓" : "✗";
            System.out.printf("%d\t%s\t%s\t%s\t%s\t%s\t%s%n",
                i + 1,
                data.getProductId(),
                StringUtils.isNotBlank(data.getStoreId()) ? data.getStoreId() : "(未匹配)",
                deviceStoreId != null ? deviceStoreId : "(无)",
                data.getProvince(),
                data.getCity(),
                status
            );
        }

        for (XiaodaiRegister data : outputList) {
            if (StringUtils.isNotBlank(data.getProductId())) {
                String deviceStoreId = deviceNameToStoreIdMap.get(data.getProductId().trim());
                if (deviceStoreId != null) {
                    matchedCount++;
                    if (deviceStoreId.equals(data.getStoreId())) {
                        correctMatchCount++;
                    } else {
                        wrongMatchCount++;
                    }
                } else {
                    notMatchedCount++;
                }
            }
        }

        System.out.println("\n========== 统计信息 ==========");
        System.out.println("总数据量: " + totalCount);
        System.out.println("设备表中有匹配的: " + matchedCount);
        System.out.println("设备表中无匹配: " + notMatchedCount);
        System.out.println("正确填充storeId: " + correctMatchCount);
        System.out.println("填充错误: " + wrongMatchCount);
        System.out.println("匹配率: " + (correctMatchCount * 100.0 / totalCount) + "%");
    }

    static class SimpleAnalysisEventListener<T> extends com.alibaba.excel.event.AnalysisEventListener<T> {
        private final List<T> dataList = new ArrayList<>();

        @Override
        public void invoke(T data, com.alibaba.excel.context.AnalysisContext context) {
            dataList.add(data);
        }

        @Override
        public void doAfterAllAnalysed(com.alibaba.excel.context.AnalysisContext context) {
        }

        public List<T> getDataList() {
            return dataList;
        }
    }
}
