package com.geekzhang.worktest.workutil.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @author zwm
 * @desc AllDataStatisticsResponse
 * @date 2025年02月27日 14:26
 */
@Data
public class AllDataStatisticsResponse {

    //清扫门店数
    private String  name ;
    //清扫门店数
    private String  storeId;

    //清扫门店数
    private String  brand;

    //清扫门店数
    private String  province ;

    //清扫门店数
    private Long cleanStoreCount= 0L;
    //AI门店数
    private Long aiStoreCount= 0L;
    //纯ai通话时长
    private BigDecimal aiDuration;
    //ai总量
    private BigDecimal aiTotal;
    //承接率
    private String  undertake;
    //送物任务数
    private Integer deliveryCount;
    //送物工作时长
    private BigDecimal deliveryDuration;
    //清扫面积
    private BigDecimal sweepArea;
    //清扫时长
    private BigDecimal sweepDuration;
    //送物工单
    private Integer deliveryWorkCount;
    //送物工单15
    private Integer deliveryWorkCount15;
    //清扫任务数
    private Integer repairWorkCount;
    private Integer repairWorkCount15;
    //清扫任务数
    private Integer cleanWorkCount;
    private Integer cleanWorkCount15;
    private Integer warnWorkCount;
    private Integer warnWorkCount60;


}
