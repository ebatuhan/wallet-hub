package com.batu.insights_service.config;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class ClickHouseJdbcConfigurationTest {

    private final ClickHouseJdbcConfiguration configuration = new ClickHouseJdbcConfiguration();

    @Test
    void clickHouseDataSource_whenCreated_shouldUseConfiguredConnectionProperties() {
        DataSource dataSource = configuration.clickHouseDataSource(
                "jdbc:clickhouse://localhost:8123/default",
                "default",
                "password");

        assertThat(dataSource).isNotNull();
    }

    @Test
    void clickHouseJdbcTemplate_whenCreated_shouldUseProvidedDataSource() {
        DataSource dataSource = new DriverManagerDataSource("jdbc:h2:mem:test", "sa", "");

        JdbcTemplate jdbcTemplate = configuration.clickHouseJdbcTemplate(dataSource);

        assertThat(jdbcTemplate.getDataSource()).isSameAs(dataSource);
    }
}
