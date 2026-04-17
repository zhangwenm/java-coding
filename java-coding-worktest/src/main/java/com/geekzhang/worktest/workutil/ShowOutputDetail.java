package com.geekzhang.worktest.workutil;

import com.alibaba.excel.EasyExcel;
import com.geekzhang.worktest.workutil.dto.XiaodaiRegister;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 查看输出文件的详细数据
 */
@Slf4j
public class ShowOutputDetail {

    public static void main(String[] args) {
        String outputPath = "src/main/resources/美团小袋注册_填充storeId.xlsx";

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

        System.out.println("========== 前10条数据 ==========");
        System.out.println("序号\tProductId\tStoreId\t省份\t城市\t型号\t状态");
        for (int i = 0; i < Math.min(10, outputList.size()); i++) {
            XiaodaiRegister data = outputList.get(i);
            String storeIdDisplay = StringUtils.isNotBlank(data.getStoreId()) ? data.getStoreId() : "(空)";
            System.out.printf("%d\t%s\t%s\t%s\t%s\t%s\t%s%n",
                i + 1,
                data.getProductId(),
                storeIdDisplay,
                data.getProvince(),
                data.getCity(),
                data.getModel(),
                data.getStatus()
            );
        }

        System.out.println("\n========== 验证匹配情况 ==========");
        int matchedCount = 0;
        int notMatchedCount = 0;

        System.out.println("========== 找出未匹配的几条数据 ==========");
        int nullCount = 0;
        for (int i = 0; i < outputList.size(); i++) {
            XiaodaiRegister data = outputList.get(i);
            String storeId = data.getStoreId();
            if (storeId == null || storeId.isEmpty()) {
                System.out.printf("行%d: productId=%s, storeId=空%n", i + 1, data.getProductId());
                nullCount++;
                if (nullCount >= 10) {
                    break;
                }
            }
        }

        for (XiaodaiRegister data : outputList) {
            String storeId = data.getStoreId();
            if (storeId != null && !storeId.isEmpty()) {
                matchedCount++;
            } else {
                notMatchedCount++;
            }
        }
        System.out.println("\n总数据量: " + outputList.size());
        System.out.println("匹配成功（storeId有值）: " + matchedCount);
        System.out.println("未匹配（storeId为空）: " + notMatchedCount);
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
