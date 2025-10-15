package com.geekzhang.worktest.workutil;

import com.alibaba.excel.EasyExcel;
import com.geekzhang.worktest.workutil.dto.Device;
import com.geekzhang.worktest.workutil.dto.DeviceInfo;
import com.geekzhang.worktest.workutil.dto.HdosStore;
import com.geekzhang.worktest.workutil.dto.MeituanStoreInfo;
import com.geekzhang.worktest.workutil.dto.MethodDto;
import com.geekzhang.worktest.workutil.dto.PlaceInfo;

/**
 * @author zwm
 * @desc ExcelTest
 * @date 2024年01月18日 10:40
 */
public class ExcelTest {

    public static void main(String[] args) {
        String path = "/Users/admin/Downloads/data/tenet.xlsx"; // 替换成你的包名
//        InputStream resourceAsStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("excel/4.xlsx");
        EasyExcel.read(path, MeituanStoreInfo.class, new UserDataListener()).sheet().doRead();
    }
}
