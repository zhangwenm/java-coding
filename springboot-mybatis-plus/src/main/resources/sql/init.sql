-- SpringBoot + MyBatis-Plus 数据库初始化脚本
-- 创建数据库
CREATE DATABASE IF NOT EXISTS `test_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `test_db`;

-- 创建用户表
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码',
    `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `avatar` VARCHAR(500) DEFAULT NULL COMMENT '头像',
    `status` INT DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_user` VARCHAR(50) DEFAULT 'system' COMMENT '创建人',
    `update_user` VARCHAR(50) DEFAULT 'system' COMMENT '更新人',
    `deleted` INT DEFAULT 0 COMMENT '逻辑删除标志：0-未删除，1-已删除',
    `version` INT DEFAULT 1 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_email` (`email`),
    UNIQUE KEY `uk_phone` (`phone`),
    INDEX `idx_status` (`status`),
    INDEX `idx_create_time` (`create_time`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';

-- 插入测试数据
INSERT INTO `sys_user` (`username`, `password`, `nickname`, `email`, `phone`, `status`) VALUES
('admin', '$2a$10$7JB720yubVSOfvVWdBYoOeymQxFYdZpLaiTYd19HtfTddZNIN4FTi', '系统管理员', 'admin@example.com', '13800000001', 1),
('user001', '$2a$10$7JB720yubVSOfvVWdBYoOeymQxFYdZpLaiTYd19HtfTddZNIN4FTi', '普通用户1', 'user001@example.com', '13800000002', 1),
('user002', '$2a$10$7JB720yubVSOfvVWdBYoOeymQxFYdZpLaiTYd19HtfTddZNIN4FTi', '普通用户2', 'user002@example.com', '13800000003', 1),
('user003', '$2a$10$7JB720yubVSOfvVWdBYoOeymQxFYdZpLaiTYd19HtfTddZNIN4FTi', '普通用户3', 'user003@example.com', '13800000004', 0),
('test', '$2a$10$7JB720yubVSOfvVWdBYoOeymQxFYdZpLaiTYd19HtfTddZNIN4FTi', '测试用户', 'test@example.com', '13800000005', 1);

-- 创建美团小袋信息表（基于之前的需求）
DROP TABLE IF EXISTS `t_meituan_xiaodai_info`;
CREATE TABLE `t_meituan_xiaodai_info` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `place_id` VARCHAR(36) NOT NULL COMMENT '场所ID',
    `type` VARCHAR(20) NOT NULL DEFAULT '' COMMENT '类型',
    `product_id` VARCHAR(36) NOT NULL COMMENT '产品ID',
    `device_id` VARCHAR(36) DEFAULT '' COMMENT '设备ID',
    `keywords` VARCHAR(255) DEFAULT '' COMMENT '关键词',
    `lat` DOUBLE NOT NULL DEFAULT '360' COMMENT '纬度',
    `lng` DOUBLE NOT NULL DEFAULT '360' COMMENT '经度',
    `status` INT NOT NULL DEFAULT '1' COMMENT '状态：1-有效，0-失效',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_user` VARCHAR(255) DEFAULT '' COMMENT '创建人',
    `update_user` VARCHAR(255) DEFAULT '' COMMENT '更新人',
    PRIMARY KEY (`id`),
    INDEX `idx_place_id` (`place_id`),
    INDEX `idx_product_id` (`product_id`),
    INDEX `idx_device_id` (`device_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_create_time` (`create_time`),
    INDEX `idx_location` (`lat`, `lng`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='美团小袋注册信息表';

-- 插入美团小袋测试数据
INSERT INTO `t_meituan_xiaodai_info` (`place_id`, `type`, `product_id`, `device_id`, `keywords`, `lat`, `lng`, `status`) VALUES
('PLACE_001', 'restaurant', 'PROD_001', 'DEV_001', '餐厅,美食,外卖', 39.9042, 116.4074, 1),
('PLACE_002', 'shop', 'PROD_002', 'DEV_002', '商店,购物,零售', 39.9142, 116.4174, 1),
('PLACE_003', 'hotel', 'PROD_003', 'DEV_003', '酒店,住宿,旅行', 39.9242, 116.4274, 1),
('PLACE_004', 'cafe', 'PROD_004', 'DEV_004', '咖啡,饮品,休闲', 39.9342, 116.4374, 0),
('PLACE_005', 'market', 'PROD_005', 'DEV_005', '市场,生鲜,超市', 39.9442, 116.4474, 1);

-- 创建语音上传记录表（基于之前的需求）
DROP TABLE IF EXISTS `pad_voice_uploads`;
CREATE TABLE `pad_voice_uploads` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `voice_name` VARCHAR(255) DEFAULT NULL COMMENT '文件名称',
    `voice_url` VARCHAR(500) DEFAULT NULL COMMENT '音频地址',
    `store_id` VARCHAR(50) DEFAULT NULL COMMENT '门店ID',
    `room_no` VARCHAR(50) DEFAULT NULL COMMENT '房间号',
    `upload_time` VARCHAR(50) DEFAULT NULL COMMENT '上传时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `status` INT DEFAULT 0 COMMENT '上传状态：0-失败，1-成功',
    `update_user` VARCHAR(50) DEFAULT NULL COMMENT '更新人',
    `create_user` VARCHAR(50) DEFAULT NULL COMMENT '创建人',
    PRIMARY KEY (`id`),
    INDEX `idx_store_id` (`store_id`) COMMENT '门店ID索引',
    INDEX `idx_room_no` (`room_no`) COMMENT '房间号索引',
    INDEX `idx_status` (`status`) COMMENT '状态索引',
    INDEX `idx_create_time` (`create_time`) COMMENT '创建时间索引',
    INDEX `idx_store_room` (`store_id`, `room_no`) COMMENT '门店房间联合索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='平板语音上传记录表';

-- 插入语音上传测试数据
INSERT INTO `pad_voice_uploads` (`voice_name`, `voice_url`, `store_id`, `room_no`, `upload_time`, `status`) VALUES
('voice_001.mp3', '/uploads/voice/voice_001.mp3', 'STORE_001', '101', '2025-09-24 10:30:00', 1),
('voice_002.wav', '/uploads/voice/voice_002.wav', 'STORE_001', '102', '2025-09-24 10:35:00', 1),
('voice_003.mp3', '/uploads/voice/voice_003.mp3', 'STORE_002', '201', '2025-09-24 10:40:00', 0),
('voice_004.wav', '/uploads/voice/voice_004.wav', 'STORE_002', '202', '2025-09-24 10:45:00', 1),
('voice_005.mp3', '/uploads/voice/voice_005.mp3', 'STORE_003', '301', '2025-09-24 10:50:00', 1);

-- 查询验证数据
SELECT '用户表数据验证' as info;
SELECT COUNT(*) as user_count FROM sys_user;
SELECT username, nickname, email, status FROM sys_user LIMIT 3;

SELECT '美团小袋信息表数据验证' as info;
SELECT COUNT(*) as xiaodai_count FROM t_meituan_xiaodai_info;
SELECT place_id, type, product_id, status FROM t_meituan_xiaodai_info LIMIT 3;

SELECT '语音上传记录表数据验证' as info;
SELECT COUNT(*) as voice_count FROM pad_voice_uploads;
SELECT voice_name, store_id, room_no, status FROM pad_voice_uploads LIMIT 3;
