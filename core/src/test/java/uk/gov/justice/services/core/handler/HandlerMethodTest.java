package uk.gov.justice.services.core.handler;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.handler.HandlerMethod.handlerMethod;
import static uk.gov.justice.services.core.handler.Handlers.handlerMethodsFrom;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.handler.exception.HandlerExecutionException;
import uk.gov.justice.services.core.handler.registry.exception.InvalidHandlerException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjectEnvelopeConverter;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;

import com.google.common.io.Resources;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class HandlerMethodTest {

    @Mock
    private AsynchronousCommandHandler asynchronousCommandHandler;

    @Mock
    private SynchronousCommandHandler synchronousCommandHandler;

    private JsonEnvelope envelope;

    @Before
    public void setup() throws Exception {
        envelope = testEnvelope("envelope.json");
    }

    @Test
    public void shouldExecuteAsynchronousHandlerMethod() throws Exception {
        Object result = asyncHandlerInstance().execute(envelope);
        verify(asynchronousCommandHandler).handles(envelope);
        assertThat(result, nullValue());
    }

    @Test
    public void shouldExecuteSynchronousHandlerMethod() throws Exception {
        when(synchronousCommandHandler.handles(envelope)).thenReturn(envelope);
        Object result = syncHandlerInstance().execute(envelope);
        assertThat(result, sameInstance(envelope));
    }

    @Test(expected = HandlerExecutionException.class)
    public void shouldThrowHandlerExecutionExceptionIfExceptionThrown() throws Exception {
        doThrow(new RuntimeException()).when(asynchronousCommandHandler).handles(envelope);
        asyncHandlerInstance().execute(envelope);
    }

    @Test
    public void shouldReturnStringDescriptionOfHandlerInstanceAndMethod() {
        assertThat(asyncHandlerInstance().toString(), notNullValue());
    }

    @Test
    public void shouldBeSynchronous() throws Exception {
        assertThat(syncHandlerInstance().isSynchronous(), is(true));
    }

    @Test
    public void shouldBeAsynchronous() throws Exception {
        assertThat(asyncHandlerInstance().isSynchronous(), is(false));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionWithNullHandlerInstance() {
        handlerMethod(null, method(new AsynchronousCommandHandler(), "handles"), Void.TYPE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionWithNullMethod() {
        handlerMethod(asynchronousCommandHandler, null, Void.TYPE);
    }

    @Test(expected = InvalidHandlerException.class)
    public void shouldThrowExceptionWithSynchronousMethod() {
        handlerMethod(asynchronousCommandHandler, method(new AsynchronousCommandHandler(), "handlesSync"), Void.TYPE);
    }

    @Test(expected = InvalidHandlerException.class)
    public void shouldThrowExceptionWithAsynchronousMethod() {
        handlerMethod(synchronousCommandHandler, method(new SynchronousCommandHandler(), "handlesAsync"), JsonEnvelope.class);
    }

    @Test(expected = InvalidHandlerException.class)
    public void shouldThrowExceptionWithAMethodWithNoParameters() {
        handlerMethod(synchronousCommandHandler, method(new InvalidCommandHandler(), "handlesNoParameter"), JsonEnvelope.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionWithAMethodWithNonJsonEnvelopeParameter() {
        handlerMethod(synchronousCommandHandler, method(new InvalidCommandHandler(), "handlesNonEnvelope"), JsonEnvelope.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldOnlyAcceptVoidOrEnvelopeReturnTypes() {
        handlerMethod(synchronousCommandHandler, method(new SynchronousCommandHandler(), "handles"), Object.class);
    }

    @Test
    public void shouldCallTraceLoggerForAsync() throws Exception {
        final List<String> log = new LinkedList<>();
        final AsynchronousCommandHandler commandHandler = new AsynchronousCommandHandler();
        new HandlerMethod(commandHandler, method(commandHandler, "handles"), Void.TYPE,
                (logger) -> (stringSupplier) -> log.add(stringSupplier.get()))
                .execute(envelope);

        assertThat(log.size(), is(2));
        assertThat(log.get(0), is("Dispatching to handler uk.gov.justice.services.core.handler.HandlerMethodTest$AsynchronousCommandHandler.handles : {\"id\":\"861c9430-7bc6-4bf0-b549-6534394b8d65\",\"name\":\"test.command.do-something\",\"correlation\":\"d51597dc-2526-4c71-bd08-5031c79f11e1\",\"session\":\"45b0c3fe-afe6-4652-882f-7882d79eadd9\",\"user\":\"72251abb-5872-46e3-9045-950ac5bae399\",\"causation\":[\"cd68037b-2fcf-4534-b83d-a9f08072f2ca\",\"43464b22-04c1-4d99-8359-82dc1934d763\"]}"));
        assertThat(log.get(1), is("Response from handler class uk.gov.justice.services.core.handler.HandlerMethodTest$AsynchronousCommandHandler.handles with id 861c9430-7bc6-4bf0-b549-6534394b8d65 was void"));
    }

    @Test
    public void shouldCallTraceLoggerForSync() throws Exception {
        final List<String> log = new LinkedList<>();
        final SynchronousCommandHandler commandHandler = new SynchronousCommandHandler();
        new HandlerMethod(commandHandler, method(commandHandler, "handles"), JsonEnvelope.class,
                (logger) -> (stringSupplier) -> log.add(stringSupplier.get()))
                .execute(envelope);

        assertThat(log.size(), is(2));
        assertThat(log.get(0), is("Dispatching to handler uk.gov.justice.services.core.handler.HandlerMethodTest$SynchronousCommandHandler.handles : {\"id\":\"861c9430-7bc6-4bf0-b549-6534394b8d65\",\"name\":\"test.command.do-something\",\"correlation\":\"d51597dc-2526-4c71-bd08-5031c79f11e1\",\"session\":\"45b0c3fe-afe6-4652-882f-7882d79eadd9\",\"user\":\"72251abb-5872-46e3-9045-950ac5bae399\",\"causation\":[\"cd68037b-2fcf-4534-b83d-a9f08072f2ca\",\"43464b22-04c1-4d99-8359-82dc1934d763\"]}"));
        assertThat(log.get(1), is("Response received from handler class uk.gov.justice.services.core.handler.HandlerMethodTest$SynchronousCommandHandler.handles : {\"id\":\"861c9430-7bc6-4bf0-b549-6534394b8d65\",\"name\":\"test.command.do-something\",\"correlation\":\"d51597dc-2526-4c71-bd08-5031c79f11e1\",\"session\":\"45b0c3fe-afe6-4652-882f-7882d79eadd9\",\"user\":\"72251abb-5872-46e3-9045-950ac5bae399\",\"causation\":[\"cd68037b-2fcf-4534-b83d-a9f08072f2ca\",\"43464b22-04c1-4d99-8359-82dc1934d763\"]}"));
    }

    private HandlerMethod asyncHandlerInstance() {
        return handlerMethod(asynchronousCommandHandler, method(new AsynchronousCommandHandler(), "handles"), Void.TYPE);
    }

    private HandlerMethod syncHandlerInstance() {
        return handlerMethod(synchronousCommandHandler, method(new SynchronousCommandHandler(), "handles"), JsonEnvelope.class);
    }

    private Method method(final Object object, final String methofName) {
        return handlerMethodsFrom(object).stream()
                .filter(m -> methofName.equals(m.getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(format("Cannot find method with name %s", methofName)));
    }

    private JsonEnvelope testEnvelope(String fileName) throws IOException {
        String jsonString = Resources.toString(Resources.getResource("json/" + fileName), Charset.defaultCharset());
        return new JsonObjectEnvelopeConverter().asEnvelope(new StringToJsonObjectConverter().convert(jsonString));
    }

    public static class AsynchronousCommandHandler {

        @Handles("test-context.command.create-something")
        public void handles(final JsonEnvelope envelope) {
        }

        @Handles("test-context.command.create-something-else")
        public JsonEnvelope handlesSync(final JsonEnvelope envelope) {
            return envelope;
        }
    }

    public static class SynchronousCommandHandler {

        @Handles("test-context.command.create-something")
        public JsonEnvelope handles(final JsonEnvelope envelope) {
            return envelope;
        }

        @Handles("test-context.command.create-something-else")
        public void handlesAsync(final JsonEnvelope envelope) {
        }
    }

    public static class InvalidCommandHandler {

        @Handles("test-context.command.create-something")
        public JsonEnvelope handlesNoParameter() {
            return null;
        }

        @Handles("test-context.command.create-something-else")
        public void handlesNonEnvelope(final Object envelope) {
        }
    }
}
