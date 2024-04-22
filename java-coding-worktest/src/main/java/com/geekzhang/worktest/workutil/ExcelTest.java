package com.geekzhang.worktest.workutil;

import com.alibaba.excel.EasyExcel;
import com.geekzhang.worktest.workutil.dto.MethodDto;

/**
 * @author zwm
 * @desc ExcelTest
 * @date 2024年01月18日 10:40
 */
public class ExcelTest {

    public static void main(String[] args) {
        String path = "/Users/admin/Downloads/demo.xlsx"; // 替换成你的包名
//        InputStream resourceAsStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("excel/4.xlsx");
        EasyExcel.read(path, MethodDto.class,new UserDataListener()).sheet("Sheet1").doRead();
    }
}
