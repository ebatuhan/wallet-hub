package com.batu.shared.error;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ProblemDetail;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({ ProblemDetail.class, ResponseEntityExceptionHandler.class })
public class CommonErrorAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(CommonApplicationErrorAdvice.class)
    CommonApplicationErrorAdvice commonApplicationErrorAdvice() {
        return new CommonApplicationErrorAdvice();
    }
}
