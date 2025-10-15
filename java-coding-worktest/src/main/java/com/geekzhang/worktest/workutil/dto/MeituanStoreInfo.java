package com.geekzhang.worktest.workutil.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/**
 * @author zwm
 * @desc MeituanStoreInfo
 * @date 2025年09月23日 11:06
 */
@Data
public class MeituanStoreInfo {

    @ExcelProperty("product_id")
    private String robotId;
    @ExcelProperty("chassis_id")
    private String chassisId;
    @ExcelProperty("hdos_store_id")
    private String storeId;
    @ExcelProperty("latitude")
    private String latitude;
    @ExcelProperty("longitude")
    private String longitude;
    @ExcelProperty("province_name")
    private String province;
    @ExcelProperty("city_name")
    private String city;
    @ExcelProperty("district_name")
    private String district;
    @ExcelProperty("address")
    private String address;
    @ExcelProperty("hdos_store_name")
    private String storeName;
    @ExcelProperty("place_id")
    private String placeId;
    @ExcelProperty("group_name")
    private String groupName;

}
