package com.geekzhang.worktest.workutil.dto;

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
public class DeviceInfo extends  DeviceUp{


    @ColumnWidth(20)
    @ExcelProperty("productId")
    private String productId;
//    @ColumnWidth(20)
//    @ExcelProperty("酒店名称")
//    private String placeName;
//    @ColumnWidth(20)
//    @ExcelProperty("客户ID")
//    private String hotelId;
//    @ColumnWidth(20)
//    @ExcelProperty("心跳")
//    private String ts;


}
