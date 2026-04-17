package com.geekzhang.worktest.workutil.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 美团小袋注册数据实体类
 */
@Data
public class XiaodaiRegister {

    @ExcelProperty(index = 0)
    private String productId;

    @ExcelProperty(index = 1)
    private String province;

    @ExcelProperty(index = 2)
    private String city;

    @ExcelProperty(index = 3)
    private String model;

    @ExcelProperty(index = 4)
    private String storeId;

    @ExcelProperty(index = 5)
    private String status;
}
