package com.geekzhang.mybatisplus.entity;

/**
 * @author zwm
 * @desc MeituanXiaodaiInfo
 * @date 2025年09月24日 11:07
 */


import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 美团小袋注册信息表
 *
 * @author system
 * @since 2025-09-24
 */
@Data
@Accessors(chain = true)
@TableName("t_meituan_xiaodai_info")
public class MeituanXiaodaiInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 场所ID
     */
    @TableField("place_id")
    private String placeId;

    /**
     * 类型
     */
    @TableField("type")
    private String type;

    /**
     * 产品ID
     */
    @TableField("product_id")
    private String productId;

    /**
     * 设备ID
     */
    @TableField("device_id")
    private String deviceId;

    /**
     * 关键词
     */
    @TableField("keywords")
    private String keywords;

    /**
     * 纬度
     */
    @TableField("lat")
    private Double lat;

    /**
     * 经度
     */
    @TableField("lng")
    private Double lng;

    /**
     * 状态：1-有效，0-失效
     */
    @TableField("status")
    private Integer status;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 创建人
     */
    @TableField("create_user")
    private String createUser;

    /**
     * 更新人
     */
    @TableField("update_user")
    private String updateUser;
}

