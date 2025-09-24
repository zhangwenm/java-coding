package com.geekzhang.worktest.workutil.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * @author zwm
 * @desc Device
 * @date 2023年09月04日 11:17
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlaceInfo {
    //清扫门店数
    @ColumnWidth(20)
    @ExcelProperty("设备SN")
    private String  placeId ;
    @ColumnWidth(20)
    @ExcelProperty("锦江CODE")
    private String  outBrandId;
    //清扫门店数
    @ColumnWidth(20)
    @ExcelProperty("GROUP")
    private String  groupId ;
    @ColumnWidth(20)
    @ExcelProperty("NAME")
    //清扫门店数
    private String  name ;

}
