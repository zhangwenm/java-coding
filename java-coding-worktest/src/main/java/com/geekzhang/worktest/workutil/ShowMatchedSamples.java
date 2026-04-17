package com.geekzhang.worktest.workutil;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 查看匹配成功的案例
 */
@Slf4j
public class ShowMatchedSamples {

    public static void main(String[] args) {
        String outputPath = "src/main/resources/美团小袋注册_填充storeId.xlsx";

        List<List<String>> dataList = new ArrayList<>();
        EasyExcel.read(outputPath, new AnalysisEventListener<List<String>>() {
            @Override
            public void invoke(List<String> data, AnalysisContext context) {
                dataList.add(data);
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {
            }
        }).sheet().doRead();

        System.out.println("========== 匹配成功的案例（前10条） ==========");
        int count = 0;
        for (int i = 0; i < dataList.size(); i++) {
            List<String> row = dataList.get(i);
            String productId = row.get(0);
            String storeId = row.get(4);

            if (count < 10) {
                System.out.println("行" + (i + 1) + ": productId=" + productId + ", storeId=" + storeId);
                count++;
            }
        }

        System.out.println("\n========== 检查device表中是否有这些productId ==========");
        String devicePath = "src/main/resources/t_device_info.xls";

        List<List<String>> deviceList = new ArrayList<>();
        EasyExcel.read(devicePath, new AnalysisEventListener<List<String>>() {
            @Override
            public void invoke(List<String> data, AnalysisContext context) {
                if (deviceList.size() < 20) {
                    deviceList.add(data);
                }
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {
            }
        }).sheet().doRead();

        System.out.println("Device表前5行数据（第3列是storeId，第6列是name）:");
        for (int i = 0; i < Math.min(5, deviceList.size()); i++) {
            List<String> row = deviceList.get(i);
            System.out.println("行" + (i + 1) + ": storeId=" + row.get(3) + ", name=" + row.get(6));
        }
    }
}
