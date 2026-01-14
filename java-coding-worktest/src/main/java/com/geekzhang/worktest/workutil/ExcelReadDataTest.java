package com.geekzhang.worktest.workutil;

import com.alibaba.excel.EasyExcel;
import com.geekzhang.worktest.workutil.dto.MeituanStoreInfo;

/**
 * @author zwm
 * @desc ExcelTest
 * @date 2024年01月18日 10:40
 */
public class ExcelReadDataTest {

    public static void main(String[] args) {
        String path = "/Users/admin/Downloads/data/zhouji.xlsx"; // 替换成你的包名
        EasyExcel.read(path, MeituanStoreInfo.class, new MeiTuanDataListener()).sheet().doRead();
    }
}
