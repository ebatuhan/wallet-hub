package com.batu.account_service.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationPredicate;
import io.micrometer.tracing.exporter.FinishedSpan;
import io.micrometer.tracing.exporter.SpanExportingPredicate;

class ObservationConfigurationTest {

    private final ObservationConfiguration configuration = new ObservationConfiguration();

    @Test
    void noActuatorServerObservations_whenRequestTargetsActuator_shouldRejectObservation() {
        ObservationPredicate predicate = configuration.noActuatorServerObservations();

        boolean accepted = predicate.test("http.server.requests", new RequestContext(URI.create("/actuator/health")));

        assertThat(accepted).isFalse();
    }

    @Test
    void noActuatorServerObservations_whenRequestTargetsApplicationPath_shouldAcceptObservation() {
        ObservationPredicate predicate = configuration.noActuatorServerObservations();

        boolean accepted = predicate.test("http.server.requests", new RequestContext(URI.create("/accounts")));

        assertThat(accepted).isTrue();
    }

    @Test
    void noActuatorServerObservations_whenContextDoesNotExposeCarrier_shouldAcceptObservation() {
        ObservationPredicate predicate = configuration.noActuatorServerObservations();

        boolean accepted = predicate.test("custom", new Observation.Context());

        assertThat(accepted).isTrue();
    }

    @Test
    void noActuatorSpans_whenSpanNameContainsActuator_shouldRejectSpan() {
        SpanExportingPredicate predicate = configuration.noActuatorSpans();
        FinishedSpan span = span("GET /actuator/health", Map.of());

        assertThat(predicate.isExportable(span)).isFalse();
    }

    @Test
    void noActuatorSpans_whenTagStartsWithActuator_shouldRejectSpan() {
        SpanExportingPredicate predicate = configuration.noActuatorSpans();
        FinishedSpan span = span("GET", Map.of("uri", "/actuator/info"));

        assertThat(predicate.isExportable(span)).isFalse();
    }

    @Test
    void noActuatorSpans_whenSpanTargetsApplicationPath_shouldAcceptSpan() {
        SpanExportingPredicate predicate = configuration.noActuatorSpans();
        FinishedSpan span = span("GET /accounts", Map.of("uri", "/accounts"));

        assertThat(predicate.isExportable(span)).isTrue();
    }

    private static FinishedSpan span(String name, Map<String, String> tags) {
        FinishedSpan span = mock(FinishedSpan.class);
        when(span.getName()).thenReturn(name);
        when(span.getTags()).thenReturn(tags);
        return span;
    }

    static class RequestContext extends Observation.Context {
        private final Carrier carrier;

        RequestContext(URI uri) {
            this.carrier = new Carrier(uri);
        }

        public Carrier getCarrier() {
            return carrier;
        }
    }

    static class Carrier {
        private final URI uri;

        Carrier(URI uri) {
            this.uri = uri;
        }

        public URI getURI() {
            return uri;
        }
    }
}
