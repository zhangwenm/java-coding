package com.geekzhang.mybatisplus.dao;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

/**
 * 简单的amount精度测试（不依赖Spring）
 */
public class SimpleAmountTest {

    @Test
    public void testBigDecimalPrecision() {
        // 测试BigDecimal精度
        BigDecimal amount1 = new BigDecimal("4021.24863088718");
        BigDecimal amount2 = new BigDecimal("5977.97260273973");

        System.out.println("amount1 = " + amount1);
        System.out.println("amount1 scale = " + amount1.scale());
        System.out.println("amount1 precision = " + amount1.precision());

        System.out.println("\namount2 = " + amount2);
        System.out.println("amount2 scale = " + amount2.scale());
        System.out.println("amount2 precision = " + amount2.precision());

        // 测试decimal(20,12)能存储的范围
        // 总共20位，小数点后12位，所以整数部分最多8位
        System.out.println("\ndecimal(20,12) 可以存储:");
        System.out.println("- 整数部分最多: 8位");
        System.out.println("- 小数部分最多: 12位");
        System.out.println("- amount1 整数部分: 4位 ✓");
        System.out.println("- amount1 小数部分: 11位 ✓");
        System.out.println("- amount2 整数部分: 4位 ✓");
        System.out.println("- amount2 小数部分: 11位 ✓");
    }

    @Test
    public void testConstructorDifference() {
        // 比较不同构造方式的差异
        double d = 4021.24863088718;
        BigDecimal bd1 = new BigDecimal(d);  // 从double构造 - 有精度问题！
        BigDecimal bd2 = new BigDecimal("4021.24863088718");  // 从String构造 - 精确！

        System.out.println("从double构造: " + bd1);
        System.out.println("从String构造: " + bd2);
        System.out.println("相等? " + bd1.equals(bd2));

        System.out.println("\n结论: 必须使用 new BigDecimal(String) 来保证精度!");
    }
}
