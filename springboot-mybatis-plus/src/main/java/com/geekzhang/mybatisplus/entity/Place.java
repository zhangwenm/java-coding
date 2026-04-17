package com.geekzhang.mybatisplus.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * t_place 场所表
 */
@Data
@TableName("t_place")
public class Place implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String type;
    private String state;
    private String placeId;
    private String placeName;
    private Integer regionId;
    private String address;
    private Integer star;
    private Double lat;
    private Double lng;
    private String creator;
    private LocalDateTime submitTime;
    private Integer isSign;
    private LocalDateTime signTime;
    private String notifyType;
    private String enterpriseType;
    private String mobileNotifier;
    private String storeId;
    private String crmId;
    private String hotelId;
    private String customerId;
    private String customerName;
    private Long ctripId;
    private Integer meshChannel;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Long newId;
    private String contactRemark;
    private LocalDateTime aiDeployFinishTime;
}
