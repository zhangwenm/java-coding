package com.geekzhang.worktest.workutil.dto;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author zwm
 * @desc Device
 * @date 2023年09月04日 11:17
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Device extends  DeviceUp{


    @ColumnWidth(20)
    @ExcelProperty("设备SN")
    private String productId;
    @ColumnWidth(20)
    @ExcelProperty("place_name")
    private String placeName;
    @ColumnWidth(20)
    @ExcelProperty("city_name")
    private String dummyCityName;
    @ColumnWidth(20)
    @ExcelProperty("address")
    private String address;
    @ColumnWidth(20)
    @ExcelProperty("province_name")
    private String provinceName;
    @ColumnWidth(20)
    @ExcelProperty("group_name")
    private String groupName;
    @ColumnWidth(20)
    @ExcelProperty("district_name")
    private String districtName;
    @ColumnWidth(20)
    @ExcelProperty("place_id")
    private String placeId;
    @ColumnWidth(20)
    @ExcelProperty("chassis_id（底盘ID）")
    private String chassisId;
    @ColumnWidth(20)
    @ExcelProperty("类型")
    private String type;

}
