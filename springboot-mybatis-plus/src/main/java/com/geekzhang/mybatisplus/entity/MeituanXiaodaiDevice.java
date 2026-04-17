package com.geekzhang.mybatisplus.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * t_meituan_xiaodai_info 美团小袋注册信息表
 */
@Data
@TableName("t_meituan_xiaodai_info")
public class MeituanXiaodaiDevice implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String placeId;
    private String type;
    private String productId;
    private String deviceId;
    private String keywords;
    private Double lat;
    private Double lng;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String createUser;
    private String updateUser;
}
