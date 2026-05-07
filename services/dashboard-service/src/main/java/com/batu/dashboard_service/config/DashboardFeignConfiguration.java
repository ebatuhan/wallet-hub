package com.batu.dashboard_service.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.openfeign.support.FeignHttpMessageConverters;
import org.springframework.cloud.openfeign.support.HttpMessageConverterCustomizer;
import org.springframework.cloud.openfeign.support.ResponseEntityDecoder;
import org.springframework.cloud.openfeign.support.SpringDecoder;
import org.springframework.cloud.openfeign.support.SpringEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;

import feign.codec.Decoder;
import feign.codec.Encoder;

@Configuration
public class DashboardFeignConfiguration {

    @Bean
    FeignHttpMessageConverters feignHttpMessageConverters(
            ObjectProvider<HttpMessageConverter<?>> messageConverters,
            ObjectProvider<HttpMessageConverterCustomizer> customizers) {
        return new FeignHttpMessageConverters(messageConverters, customizers);
    }

    @Bean
    Encoder feignEncoder(ObjectProvider<FeignHttpMessageConverters> messageConverters) {
        return new SpringEncoder(messageConverters);
    }

    @Bean
    Decoder feignDecoder(ObjectProvider<FeignHttpMessageConverters> messageConverters) {
        return new ResponseEntityDecoder(new SpringDecoder(messageConverters));
    }
}
