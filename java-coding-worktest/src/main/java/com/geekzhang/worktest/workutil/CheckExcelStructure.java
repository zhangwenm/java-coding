package com.geekzhang.worktest.workutil;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 查看Excel数据结构
 */
@Slf4j
public class CheckExcelStructure {

    public static void main(String[] args) {
        String baseDir = "src/main/resources/";
        String xiaodaiPath = baseDir + "美团小袋注册.xlsx";
        String devicePath = baseDir + "t_device_info.xls";

        System.out.println("============ 美团小袋注册.xlsx ============");
        readExcelStructure(xiaodaiPath, 5);

        System.out.println("\n============ t_device_info.xls ============");
        readExcelStructure(devicePath, 5);
    }

    private static void readExcelStructure(String filePath, int maxRows) {
        List<Map<Integer, String>> dataList = new ArrayList<>();
        EasyExcel.read(filePath, new AnalysisEventListener<Map<Integer, String>>() {
            @Override
            public void invoke(Map<Integer, String> data, AnalysisContext context) {
                if (dataList.size() < maxRows) {
                    dataList.add(data);
                }
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {
            }
        }).sheet().doRead();

        for (int i = 0; i < dataList.size(); i++) {
            Map<Integer, String> row = dataList.get(i);
            System.out.println("Row " + (i + 1) + ":");
            for (Map.Entry<Integer, String> entry : row.entrySet()) {
                System.out.println("  [" + entry.getKey() + "] " + entry.getValue());
            }
            System.out.println();
        }
    }
}
