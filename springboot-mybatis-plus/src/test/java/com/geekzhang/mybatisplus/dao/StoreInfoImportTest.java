package com.geekzhang.mybatisplus.dao;

import cn.hutool.core.text.csv.CsvReader;
import cn.hutool.core.text.csv.CsvRow;
import cn.hutool.core.text.csv.CsvUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 将 output/t_store_info.csv 导入 t_store_info 表
 */
@Slf4j
@SpringBootTest
public class StoreInfoImportTest {

    private static final String CSV_FILE =
            Paths.get("").toAbsolutePath().toString().replaceAll("springboot-mybatis-plus.*", "springboot-mybatis-plus")
            + "/output/t_store_info.csv";

    // 每批插入行数
    private static final int BATCH_SIZE = 500;

    // 跳过 GEOMETRY 等特殊类型列（无法通过 JDBC 字符串直接写入）
    private static final Set<String> SKIP_COLUMNS = Set.of("loc");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void testImportStoreInfo() throws Exception {
        log.info("读取 CSV: {}", CSV_FILE);

        CsvReader reader = CsvUtil.getReader();
        List<CsvRow> rows = reader.read(
                new FileReader(CSV_FILE, StandardCharsets.UTF_8)
        ).getRows();

        if (rows.isEmpty()) {
            log.warn("CSV 为空");
            return;
        }

        // 第一行为表头，过滤掉 SKIP_COLUMNS
        CsvRow headerRow = rows.get(0);
        List<String> allColumns = headerRow.getRawList();

        // 记录参与 INSERT 的列下标
        List<Integer> activeIndexes = new ArrayList<>();
        List<String> activeColumns = new ArrayList<>();
        for (int ci = 0; ci < allColumns.size(); ci++) {
            String col = allColumns.get(ci);
            if (!SKIP_COLUMNS.contains(col)) {
                activeIndexes.add(ci);
                activeColumns.add(col);
            }
        }
        int colCount = allColumns.size();
        log.info("CSV 总列数: {}，跳过列: {}，实际插入列数: {}", colCount, SKIP_COLUMNS, activeColumns.size());

        // 构建 INSERT SQL
        String colNames = String.join(", ", activeColumns);
        String placeholders = "?" + ", ?".repeat(activeColumns.size() - 1);
        String sql = "INSERT IGNORE INTO t_store_info (" + colNames + ") VALUES (" + placeholders + ")";
        log.info("INSERT SQL 模板: {}", sql);

        // 分批插入
        List<Object[]> batch = new ArrayList<>(BATCH_SIZE);
        int total = 0;
        int skipCount = 0;

        for (int i = 1; i < rows.size(); i++) {
            List<String> vals = rows.get(i).getRawList();

            // 列数不匹配则跳过（防止脏数据）
            if (vals.size() != colCount) {
                log.warn("第 {} 行列数不符（期望 {} 实际 {}），跳过", i + 1, colCount, vals.size());
                skipCount++;
                continue;
            }

            // 只取 activeIndexes 对应列，空字符串转 null
            Object[] args = activeIndexes.stream()
                    .map(idx -> {
                        String v = vals.get(idx);
                        return (v == null || v.isEmpty()) ? null : v;
                    })
                    .toArray();
            batch.add(args);

            if (batch.size() >= BATCH_SIZE) {
                jdbcTemplate.batchUpdate(sql, batch);
                total += batch.size();
                log.info("已插入 {} 行", total);
                batch.clear();
            }
        }

        // 剩余不足一批的数据
        if (!batch.isEmpty()) {
            jdbcTemplate.batchUpdate(sql, batch);
            total += batch.size();
        }

        log.info("导入完成：成功 {} 行，跳过 {} 行", total, skipCount);
    }
}
