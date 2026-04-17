package com.geekzhang.mybatisplus.dao;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.*;

/**
 * Amount精度测试 - 不依赖Spring容器
 */
public class AmountPrecisionTest {

    private Connection connection;

    @BeforeEach
    public void setup() throws Exception {
        // 直接建立JDBC连接
        String url = "jdbc:mysql://proxy:13306/rw_config?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2B8";
        String username = System.getenv().getOrDefault("DB_USERNAME", "root");
        String password = System.getenv().getOrDefault("DB_PASSWORD", "");

        connection = DriverManager.getConnection(url, username, password);
        System.out.println("✅ 数据库连接成功");
    }

    @AfterEach
    public void teardown() throws Exception {
        if (connection != null && !connection.isClosed()) {
            connection.close();
            System.out.println("✅ 数据库连接关闭");
        }
    }

    @Test
    public void testAmountInsert() throws Exception {
        // 测试BigDecimal精度
        BigDecimal amount1 = new BigDecimal("4021.24863088718");
        BigDecimal amount2 = new BigDecimal("5977.97260273973");

        System.out.println("\n========== 测试开始 ==========");
        System.out.println("amount1 = " + amount1);
        System.out.println("amount2 = " + amount2);

        // 插入测试数据
        String insertSql = "INSERT INTO t_robot_info_push_new (product_id, amount, state, `count`) VALUES (?, ?, 0, 0)";

        try (PreparedStatement pstmt = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            // 插入第一条
            pstmt.setString(1, "test_" + System.currentTimeMillis());
            pstmt.setBigDecimal(2, amount1);
            int rows1 = pstmt.executeUpdate();

            ResultSet rs1 = pstmt.getGeneratedKeys();
            long id1 = 0;
            if (rs1.next()) {
                id1 = rs1.getLong(1);
            }
            rs1.close();

            System.out.println("\n✅ 插入记录1: ID=" + id1 + ", rows=" + rows1);

            // 查询验证
            String selectSql = "SELECT amount FROM t_robot_info_push_new WHERE id = ?";
            try (PreparedStatement queryStmt = connection.prepareStatement(selectSql)) {
                queryStmt.setLong(1, id1);
                ResultSet rs = queryStmt.executeQuery();
                if (rs.next()) {
                    BigDecimal savedAmount = rs.getBigDecimal("amount");
                    System.out.println("插入的amount: " + amount1);
                    System.out.println("保存的amount: " + savedAmount);
                    System.out.println("是否相等: " + amount1.compareTo(savedAmount));

                    if (amount1.compareTo(savedAmount) == 0) {
                        System.out.println("✅ 精度保持一致！");
                    } else {
                        System.out.println("❌ 精度丢失！");
                        System.out.println("差值: " + amount1.subtract(savedAmount));
                    }
                }
                rs.close();
            }

            // 清理测试数据
            String deleteSql = "DELETE FROM t_robot_info_push_new WHERE id = ?";
            try (PreparedStatement deleteStmt = connection.prepareStatement(deleteSql)) {
                deleteStmt.setLong(1, id1);
                deleteStmt.executeUpdate();
                System.out.println("✅ 测试数据已清理");
            }
        }

        System.out.println("========== 测试结束 ==========\n");
    }

    @Test
    public void testTableStructure() throws Exception {
        // 检查表结构
        String sql = "SHOW FULL COLUMNS FROM t_robot_info_push_new WHERE Field = 'amount'";

        try (PreparedStatement pstmt = connection.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                System.out.println("\n========== amount字段信息 ==========");
                System.out.println("Field: " + rs.getString("Field"));
                System.out.println("Type: " + rs.getString("Type"));
                System.out.println("Null: " + rs.getString("Null"));
                System.out.println("Key: " + rs.getString("Key"));
                System.out.println("Default: " + rs.getString("Default"));
                System.out.println("Comment: " + rs.getString("Comment"));
                System.out.println("========================================\n");

                String type = rs.getString("Type");
                if (type.contains("decimal")) {
                    System.out.println("✅ amount字段类型正确: " + type);
                    if (type.contains("20,12") || type.contains("20,11")) {
                        System.out.println("✅ 精度足够存储11位小数");
                    } else if (type.contains("10,2")) {
                        System.out.println("❌ 精度不足！只能存储2位小数，需要修改为decimal(20,12)");
                    }
                } else {
                    System.out.println("❌ amount字段类型错误: " + type);
                }
            }
        }
    }
}
