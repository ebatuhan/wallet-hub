package com.batu.plaid_adapter_service.config;

import java.net.URI;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.micrometer.observation.ObservationPredicate;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;
import io.micrometer.tracing.exporter.FinishedSpan;
import io.micrometer.tracing.exporter.SpanExportingPredicate;

@Configuration
public class ObservationConfiguration {

    @Bean
    ObservedAspect observedAspect(ObservationRegistry observationRegistry) {
        return new ObservedAspect(observationRegistry);
    }

    @Bean
    ObservationPredicate noActuatorServerObservations() {
        return (name, context) -> {
            URI uri = serverRequestUri(context);
            if (uri != null) {
                return !uri.getPath().startsWith("/actuator");
            }
            return true;
        };
    }

    private URI serverRequestUri(Object context) {
        try {
            Object carrier = context.getClass().getMethod("getCarrier").invoke(context);
            Object uri = carrier.getClass().getMethod("getURI").invoke(carrier);
            return uri instanceof URI requestUri ? requestUri : null;
        } catch (ReflectiveOperationException ex) {
            return null;
        }
    }

    @Bean
    SpanExportingPredicate noActuatorSpans() {
        return span -> !isActuatorSpan(span);
    }

    private boolean isActuatorSpan(FinishedSpan span) {
        String name = span.getName();
        if (name != null && name.contains("actuator")) {
            return true;
        }
        return span.getTags().values().stream().anyMatch(value -> value != null && value.startsWith("/actuator"));
    }
}
