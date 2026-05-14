package com.batu.insights_service.repository;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.clickhouse.ClickHouseContainer;

abstract class ClickHouseRepositoryITSupport {

    static final ClickHouseContainer CLICKHOUSE = new ClickHouseContainer("clickhouse/clickhouse-server:24.8.12.28-alpine");
    private static boolean migrated;

    protected static JdbcTemplate jdbcTemplate;

    @BeforeAll
    static void migrateSchema() throws Exception {
        if (!CLICKHOUSE.isRunning()) {
            CLICKHOUSE.start();
        }

        DataSource defaultDataSource = dataSource(defaultJdbcUrl());
        new JdbcTemplate(defaultDataSource).execute("CREATE DATABASE IF NOT EXISTS clickhouse");

        if (!migrated) {
            ScriptUtils.executeSqlScript(defaultDataSource.getConnection(),
                    new ClassPathResource("db/migration/clickhouse/V1__create_insights_projection_tables.sql"));
            ScriptUtils.executeSqlScript(defaultDataSource.getConnection(),
                    new ClassPathResource("db/migration/clickhouse/V2__drop_insights_aggregate_tables.sql"));
            ScriptUtils.executeSqlScript(defaultDataSource.getConnection(),
                    new ClassPathResource("db/migration/clickhouse/V3__reorder_transactions_by_user_date.sql"));
            ScriptUtils.executeSqlScript(defaultDataSource.getConnection(),
                    new ClassPathResource("db/migration/clickhouse/V4__create_spending_aggregate_tables.sql"));
            migrated = true;
        }

        jdbcTemplate = new JdbcTemplate(dataSource(clickhouseJdbcUrl()));
    }

    @BeforeEach
    void cleanTables() {
        jdbcTemplate.execute("TRUNCATE TABLE clickhouse.transactions");
        jdbcTemplate.execute("TRUNCATE TABLE clickhouse.monthly_spending");
        jdbcTemplate.execute("TRUNCATE TABLE clickhouse.weekly_spending");
        jdbcTemplate.execute("TRUNCATE TABLE clickhouse.account_balance_history");
    }

    private static DataSource dataSource(String jdbcUrl) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.clickhouse.jdbc.ClickHouseDriver");
        dataSource.setUrl(jdbcUrl);
        dataSource.setUsername(CLICKHOUSE.getUsername());
        dataSource.setPassword(CLICKHOUSE.getPassword());
        return dataSource;
    }

    private static String defaultJdbcUrl() {
        return "jdbc:clickhouse://" + CLICKHOUSE.getHost() + ":" + CLICKHOUSE.getMappedPort(8123) + "/default";
    }

    private static String clickhouseJdbcUrl() {
        return "jdbc:clickhouse://" + CLICKHOUSE.getHost() + ":" + CLICKHOUSE.getMappedPort(8123) + "/clickhouse";
    }
}
