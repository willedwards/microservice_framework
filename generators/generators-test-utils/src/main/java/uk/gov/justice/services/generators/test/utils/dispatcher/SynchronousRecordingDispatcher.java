package uk.gov.justice.services.generators.test.utils.dispatcher;

import uk.gov.justice.services.core.dispatcher.SynchronousDispatcher;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import javax.inject.Singleton;

@Singleton
public class SynchronousRecordingDispatcher extends BasicRecordingDispatcher implements SynchronousDispatcher {

    private List<MockResponse> mockResponses = new LinkedList<>();

    @Override
    public JsonEnvelope dispatch(final JsonEnvelope dispatchedEnvelope) {
        record(dispatchedEnvelope);
        return responseTo(dispatchedEnvelope);
    }

    public void setupResponse(final String payloadElementNameCriteria, final String payloadElementValueCriteria,
                              final JsonEnvelope envelopeToReturn) {
        mockResponses.add(new MockResponse(payloadElementNameCriteria, payloadElementValueCriteria, envelopeToReturn));
    }

    private JsonEnvelope responseTo(final JsonEnvelope dispatchedEnvelope) {
        final Optional<MockResponse> response = mockResponses.stream().filter(r -> r.matches(dispatchedEnvelope)).findFirst();
        return response.isPresent() ? response.get().envelopeToReturn() : null;
    }

    private static class MockResponse {
        private final String payloadElementName;
        private final String payloadElementValue;
        private final JsonEnvelope envelopeToReturn;

        MockResponse(final String payloadElementName, final String payloadElementValue, final JsonEnvelope envelopeToReturn) {
            this.payloadElementName = payloadElementName;
            this.payloadElementValue = payloadElementValue;
            this.envelopeToReturn = envelopeToReturn;
        }

        boolean matches(final JsonEnvelope dispatchedEnvelope) {
            return payloadElementValue.equals(dispatchedEnvelope.payloadAsJsonObject().getString(payloadElementName));
        }

        JsonEnvelope envelopeToReturn() {
            return envelopeToReturn;
        }
    }
}
