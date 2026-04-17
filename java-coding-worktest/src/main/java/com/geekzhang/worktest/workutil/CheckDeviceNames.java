package com.geekzhang.worktest.workutil;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.geekzhang.worktest.workutil.dto.DeviceInfoForMatch;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 检查device表中是否有匹配的name
 */
@Slf4j
public class CheckDeviceNames {

    public static void main(String[] args) {
        String devicePath = "src/main/resources/t_device_info.xls";

        List<DeviceInfoForMatch> deviceList = new ArrayList<>();
        EasyExcel.read(devicePath, DeviceInfoForMatch.class, new AnalysisEventListener<DeviceInfoForMatch>() {
            @Override
            public void invoke(DeviceInfoForMatch data, AnalysisContext context) {
                deviceList.add(data);
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {
            }
        }).sheet().doRead();

        System.out.println("========== device表前10个name值 ==========");
        for (int i = 0; i < Math.min(10, deviceList.size()); i++) {
            DeviceInfoForMatch device = deviceList.get(i);
            System.out.println("name: " + device.getName() + ", storeId: " + device.getStoreId());
        }

        Set<String> deviceNames = new HashSet<>();
        for (DeviceInfoForMatch device : deviceList) {
            if (device.getName() != null && !device.getName().trim().isEmpty()) {
                deviceNames.add(device.getName().trim());
            }
        }

        System.out.println("\n========== 前几个productId在device表中的匹配情况 ==========");
        String[] testProductIds = {"GGG403214B3714126", "GGG403214B3714150", "GGG403214B3714187", "GGGC0321382W05543", "GGGC0321382X05565"};
        for (String productId : testProductIds) {
            boolean exists = deviceNames.contains(productId);
            System.out.println("productId: " + productId + ", 在device表中存在: " + exists);
        }
    }
}
