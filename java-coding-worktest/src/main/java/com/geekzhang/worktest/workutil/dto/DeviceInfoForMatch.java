package com.geekzhang.worktest.workutil.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 设备信息数据实体类
 */
@Data
public class DeviceInfoForMatch {

    @ExcelProperty(index = 0)
    private String id;

    @ExcelProperty(index = 1)
    private String deviceType;

    @ExcelProperty(index = 2)
    private String other1;

    @ExcelProperty(index = 3)
    private String other2;

    @ExcelProperty(index = 4)
    private String other3;

    @ExcelProperty(index = 5)
    private String storeId;

    @ExcelProperty(index = 6)
    private String name;

    @ExcelProperty(index = 7)
    private String other4;

    @ExcelProperty(index = 8)
    private String deviceName;
}
