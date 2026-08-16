package com.example.carrot.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 轻量 schema 迁移：为既有的 SQLite 库补齐新增列（幂等）。
 *
 * <p>schema.sql 的 CREATE TABLE IF NOT EXISTS 只对新建库生效，无法给已存在的表加列；
 * 而 SQLite 不支持 ADD COLUMN IF NOT EXISTS。这里在应用启动后通过 PRAGMA table_info
 * 检查列是否存在，缺失才补 ALTER，多次启动无副作用。迁移先于任何业务读写执行。</p>
 */
@Component
public class SchemaMigrator implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public SchemaMigrator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        addColumnIfMissing("tasks", "created_by", "TEXT");
        addColumnIfMissing("redemptions", "created_by", "TEXT");
    }

    private void addColumnIfMissing(String table, String column, String type) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pragma_table_info('" + table + "') WHERE name = ?",
                Integer.class, column);
        if (count == null || count == 0) {
            jdbcTemplate.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + type);
        }
    }
}
