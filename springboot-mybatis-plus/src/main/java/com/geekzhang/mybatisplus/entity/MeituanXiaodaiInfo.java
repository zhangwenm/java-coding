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
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 美团小袋注册信息表
 *
 * @author system
 * @since 2025-09-24
 */
@Data
@Accessors(chain = true)
@TableName("t_robot_info_push_new")
public class MeituanXiaodaiInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 产品ID
     */
    private String productId;

    /**
     * 集团ID
     */
    private String groupId;

    /**
     * 门店ID
     */
    private String storeId;

    /**
     * 酒店名称
     */
    private String hotelName;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 负责人
     */
    private String manager;

    /**
     * 负责人手机号
     */
    private String managerPhone;

    /**
     * 消息内容
     */
    private String msg;
    private String mobile;

    /**
     * 金额
     */
    private BigDecimal amount;

    /**
     * 数量
     */
    private Integer count;

    /**
     * 状态：0初始 1同意 2不同意
     */
    private Integer state;

    /**
     * 在线状态：0离线 1在线
     */
    private Integer online;
    private Integer yuncang;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 最新任务开始时间
     */
    private LocalDateTime lastTaskTime;
}

