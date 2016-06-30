package uk.gov.justice.services.messaging.logging;

import java.util.function.Supplier;

@FunctionalInterface
public interface Tracer {
    void trace(Supplier<String> supplier);
}
