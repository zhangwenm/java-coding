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
public class HdosStore  {
    //清扫门店数
    @ColumnWidth(20)
    @ExcelProperty("酒店名称")
    private String  name ;
    @ColumnWidth(20)
    @ExcelProperty("酒店城市")
    private String  storeId;
    //清扫门店数
    @ColumnWidth(20)
    @ExcelProperty("酒店地址")
    private String  brand ;
    @ColumnWidth(20)
    @ExcelProperty("省份")
    //清扫门店数
    private String  province ;
    @ColumnWidth(20)
    @ExcelProperty("清洁机器人上线门店数量")
    private Long cleanStoreCount;
    @ColumnWidth(20)
    @ExcelProperty("AI电话上线门店数量")
    //AI门店数
    private Long aiStoreCount= 0L;
    @ColumnWidth(20)
    @ExcelProperty("AI电话时长")
    //纯ai通话时长
    private BigDecimal aiDuration;
    @ColumnWidth(20)
    @ExcelProperty("AI电话通话量")
    //ai总量
    private BigDecimal aiTotal;
    @ColumnWidth(20)
    @ExcelProperty("AI承接率")
    //承接率
    private String  undertake;
    //送物任务数

    @ColumnWidth(20)
    @ExcelProperty("送物任务数")
    private Integer deliveryCount;
    @ColumnWidth(20)
    @ExcelProperty("送物任务时长")
    //送物工作时长
    private BigDecimal deliveryDuration;
    @ColumnWidth(20)
    @ExcelProperty("清洁平方数")
    //清扫面积
    private BigDecimal sweepArea;
    @ColumnWidth(20)
    @ExcelProperty("清洁时长")
    //清扫时长
    private BigDecimal sweepDuration;
    @ColumnWidth(20)
    @ExcelProperty("送物工单数量")
    //送物工单
    private Integer deliveryWorkCount;
    @ColumnWidth(20)
    @ExcelProperty("送物工单15分钟完成率")
    //送物工单15
    private Integer deliveryWorkCount15;
    @ColumnWidth(20)
    @ExcelProperty("维修工单数量")
    //清扫任务数
    private Integer repairWorkCount;
    @ColumnWidth(20)
    @ExcelProperty("维修工单15分钟完成率")
    private Integer repairWorkCount15;
    @ColumnWidth(20)
    @ExcelProperty("清扫工单数量")
    //清扫任务数
    private Integer cleanWorkCount;
    @ColumnWidth(20)
    @ExcelProperty("清扫工单15分钟完成率")
    private Integer cleanWorkCount15;
    @ColumnWidth(20)
    @ExcelProperty("客诉")
    private Integer warnWorkCount;
    @ColumnWidth(20)
    @ExcelProperty("客诉1小时知晓率")
    private Integer warnWorkCount60;
}
