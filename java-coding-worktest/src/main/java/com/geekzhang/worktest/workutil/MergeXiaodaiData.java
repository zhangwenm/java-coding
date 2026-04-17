package com.geekzhang.worktest.workutil;

import com.alibaba.excel.EasyExcel;
import com.geekzhang.worktest.workutil.dto.DeviceInfoForMatch;
import com.geekzhang.worktest.workutil.dto.XiaodaiRegister;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 美团小袋数据整合工具
 */
@Slf4j
public class MergeXiaodaiData {

    public static void main(String[] args) {
        String baseDir = "src/main/resources/";
        String xiaodaiPath = baseDir + "美团小袋注册.xlsx";
        String devicePath = baseDir + "t_device_info.xls";
        String outputPath = baseDir + "美团小袋注册_填充storeId.xlsx";

        log.info("开始处理数据整合任务");
        log.info("美团小袋注册文件: {}", xiaodaiPath);
        log.info("设备信息文件: {}", devicePath);
        log.info("输出文件: {}", outputPath);

        try {
            mergeXiaodaiWithDevice(xiaodaiPath, devicePath, outputPath);
            log.info("数据处理完成！");
        } catch (Exception e) {
            log.error("数据处理失败", e);
        }
    }

    public static void mergeXiaodaiWithDevice(String xiaodaiPath, String devicePath, String outputPath) {
        File xiaodaiFile = new File(xiaodaiPath);
        File deviceFile = new File(devicePath);

        if (!xiaodaiFile.exists()) {
            throw new RuntimeException("美团小袋注册文件不存在: " + xiaodaiPath);
        }
        if (!deviceFile.exists()) {
            throw new RuntimeException("设备信息文件不存在: " + devicePath);
        }

        List<XiaodaiRegister> xiaodaiList = new ArrayList<>();
        List<DeviceInfoForMatch> deviceList = new ArrayList<>();

        log.info("读取美团小袋注册数据...");
        EasyExcel.read(xiaodaiPath, XiaodaiRegister.class, new SimpleAnalysisEventListener<XiaodaiRegister>() {
            @Override
            public void invoke(XiaodaiRegister data, com.alibaba.excel.context.AnalysisContext context) {
                xiaodaiList.add(data);
            }

            @Override
            public void doAfterAllAnalysed(com.alibaba.excel.context.AnalysisContext context) {
                log.info("美团小袋注册数据读取完成，共 {} 条", xiaodaiList.size());
            }
        }).sheet().doRead();

        log.info("读取设备信息数据...");
        EasyExcel.read(devicePath, DeviceInfoForMatch.class, new SimpleAnalysisEventListener<DeviceInfoForMatch>() {
            @Override
            public void invoke(DeviceInfoForMatch data, com.alibaba.excel.context.AnalysisContext context) {
                deviceList.add(data);
            }

            @Override
            public void doAfterAllAnalysed(com.alibaba.excel.context.AnalysisContext context) {
                log.info("设备信息数据读取完成，共 {} 条", deviceList.size());
            }
        }).sheet().doRead();

        log.info("开始数据匹配...");
        int matchCount = 0;
        int notMatchCount = 0;

        Map<String, String> deviceNameToStoreIdMap = new HashMap<>();
        for (DeviceInfoForMatch device : deviceList) {
            if (StringUtils.isNotBlank(device.getName()) && StringUtils.isNotBlank(device.getStoreId())) {
                deviceNameToStoreIdMap.put(device.getName().trim(), device.getStoreId().trim());
            }
        }

        for (XiaodaiRegister xiaodai : xiaodaiList) {
            if (StringUtils.isNotBlank(xiaodai.getProductId())) {
                String productId = xiaodai.getProductId().trim();
                if (deviceNameToStoreIdMap.containsKey(productId)) {
                    xiaodai.setStoreId(deviceNameToStoreIdMap.get(productId));
                    matchCount++;
                } else {
                    xiaodai.setStoreId(null);
                    notMatchCount++;
                }
            } else {
                System.out.println("productId为空: " + xiaodai.getProductId());
            }
        }

        log.info("数据匹配完成，匹配成功: {}, 未匹配: {}", matchCount, notMatchCount);

        log.info("开始写入输出文件...");
        EasyExcel.write(outputPath, XiaodaiRegister.class).sheet("美团小袋注册").doWrite(xiaodaiList);
        log.info("输出文件写入完成: {}", outputPath);
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
