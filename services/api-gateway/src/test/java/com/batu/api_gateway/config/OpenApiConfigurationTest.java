package com.batu.api_gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OpenApiConfigurationTest {

    @Test
    void apiGatewayOpenAPI_whenCreated_shouldExposeGatewayMetadata() {
        var openApi = new OpenApiConfiguration().apiGatewayOpenAPI();

        assertThat(openApi.getInfo().getTitle()).isEqualTo("Wallet Hub API Gateway");
        assertThat(openApi.getInfo().getVersion()).isEqualTo("v1");
    }
}
