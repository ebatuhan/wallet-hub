package com.batu.insights_service.config;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

class InboxJpaConfigurationTest {

    private final InboxJpaConfiguration configuration = new InboxJpaConfiguration();

    @Test
    void inboxDataSource_whenCreated_shouldUseConfiguredPostgresConnectionProperties() {
        DataSource dataSource = configuration.inboxDataSource(
                "jdbc:postgresql://localhost:5432/inbox",
                "wallet",
                "password");

        assertThat(dataSource).isInstanceOf(DriverManagerDataSource.class);
        DriverManagerDataSource driverManagerDataSource = (DriverManagerDataSource) dataSource;
        assertThat(driverManagerDataSource.getUrl()).isEqualTo("jdbc:postgresql://localhost:5432/inbox");
        assertThat(driverManagerDataSource.getUsername()).isEqualTo("wallet");
    }

    @Test
    void inboxEntityManagerFactory_whenCreated_shouldUseProvidedDataSource() {
        DataSource dataSource = new DriverManagerDataSource("jdbc:h2:mem:test", "sa", "");

        LocalContainerEntityManagerFactoryBean factoryBean = configuration.inboxEntityManagerFactory(dataSource);

        assertThat(factoryBean.getDataSource()).isSameAs(dataSource);
    }
}
