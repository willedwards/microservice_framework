package uk.gov.justice.services.core.handler.registry;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.nullValue;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.core.handler.HandlerMethod.ASYNCHRONOUS;
import static uk.gov.justice.services.core.handler.HandlerMethod.SYNCHRONOUS;
import static uk.gov.justice.services.messaging.DefaultJsonEnvelope.envelope;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.handler.HandlerMethod;
import uk.gov.justice.services.core.handler.exception.MissingHandlerException;
import uk.gov.justice.services.core.handler.registry.exception.DuplicateHandlerException;
import uk.gov.justice.services.core.handler.registry.exception.InvalidHandlerException;
import uk.gov.justice.services.core.util.RecordingTestHandler;
import uk.gov.justice.services.messaging.JsonEnvelope;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Unit tests for the HandlerRegistry class.
 */
@RunWith(MockitoJUnitRunner.class)
public class HandlerRegistryTest {

    private static final String COMMAND_NAME = "test.command.mock-command";

    @Mock
    private TestCommandHandler commandHandler;

    @Mock
    private TestCommandHandlerDuplicate commandHandlerDuplicate;

    private HandlerRegistry registry;

    @Test
    public void shouldReturnMethodOfTheRegisteredAsynchronousHandler() {
        final TestCommandHandler testCommandHandler = new TestCommandHandler();
        createRegistryWith(testCommandHandler);
        final HandlerMethod handlerMethod = registry.get(COMMAND_NAME, ASYNCHRONOUS);
        assertHandlerMethodInvokesHandler(handlerMethod, testCommandHandler);
    }

    @Test
    public void shouldReturnMethodOfTheRegisteredSynchronousHandler() {
        final TestCommandHandlerWithSynchronousHandler testCommandHandlerWithSynchronousHandler = new TestCommandHandlerWithSynchronousHandler();
        createRegistryWith(testCommandHandlerWithSynchronousHandler);
        final HandlerMethod handlerMethod = registry.get(COMMAND_NAME, SYNCHRONOUS);
        assertHandlerMethodInvokesHandler(handlerMethod, testCommandHandlerWithSynchronousHandler);
    }



    @Test
    public void shouldReturnMethodOfTheAllEventsHandler() {
        final TestAllEventsHandler testAllEventsHandler = new TestAllEventsHandler();
        createRegistryWith(testAllEventsHandler);
        final HandlerMethod handlerMethod = registry.get("some.name", ASYNCHRONOUS);

        assertHandlerMethodInvokesHandler(handlerMethod, testAllEventsHandler);

    }

    @Test
    public void namedHandlerShouldTakePriorityOverAllHandler() {
        final TestAllEventsHandler testAllEventsHandler = new TestAllEventsHandler();
        final TestCommandHandler testCommandHandler = new TestCommandHandler();

        createRegistryWith(testAllEventsHandler, testCommandHandler);
        final HandlerMethod handlerMethod = registry.get(COMMAND_NAME, ASYNCHRONOUS);

        assertHandlerMethodInvokesHandler(handlerMethod, testCommandHandler);
        assertThat(testAllEventsHandler.recordedEnvelope(), nullValue());
    }

    private void assertHandlerMethodInvokesHandler(final HandlerMethod handlerMethod, final RecordingTestHandler handler) {
        assertThat(handlerMethod, notNullValue());

        final JsonEnvelope envelope = envelope().build();
        handlerMethod.execute(envelope);

        assertThat(handler.recordedEnvelope(), sameInstance(envelope));
    }

    @Test(expected = MissingHandlerException.class)
    public void shouldThrowExceptionForAsyncMismatch() {
        createRegistryWith(new TestCommandHandler());
        assertThat(registry.get(COMMAND_NAME, SYNCHRONOUS), nullValue());
    }

    @Test(expected = MissingHandlerException.class)
    public void shouldThrowExceptionForSyncMismatch() {
        createRegistryWith(new TestCommandHandlerWithSynchronousHandler());
        assertThat(registry.get(COMMAND_NAME, ASYNCHRONOUS), nullValue());
    }

    @Test(expected = InvalidHandlerException.class)
    public void shouldThrowExceptionWithMultipleArgumentsAsynchronousHandler() {
        createRegistryWith(new TestCommandHandlerWithWrongHandler());
    }

    @Test(expected = InvalidHandlerException.class)
    public void shouldThrowExceptionWithMultipleArgumentsSynchronousHandler() {
        createRegistryWith(new TestCommandHandlerWithWrongSynchronousHandler());
    }

    @Test(expected = DuplicateHandlerException.class)
    public void shouldThrowExceptionWithDuplicateAsynchronousHandlers() {
        createRegistryWith(new TestCommandHandler(), new TestCommandHandlerDuplicate());
    }

    @Test(expected = DuplicateHandlerException.class)
    public void shouldThrowExceptionWithDuplicateSynchronousHandlers() {
        createRegistryWith(new TestCommandHandlerWithSynchronousHandler(), new TestCommandHandlerWithSynchronousHandlerDuplicate());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionWithAsynchronousWrongParameters() {
        createRegistryWith(new TestCommandHandler(), new TestCommandHandlerWithWrongParameter());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionWithSynchronousWrongParameters() {
        createRegistryWith(new TestCommandHandlerWithSynchronousHandler(), new TestCommandSynchronousHandlerWithWrongParameter());
    }

    private void createRegistryWith(Object... handlers) {
        registry = new HandlerRegistry();
        asList(handlers).stream().forEach(x -> registry.register(x));
    }

    public static class MockCommand {
    }

    @ServiceComponent(COMMAND_HANDLER)
    public static class TestCommandHandler extends RecordingTestHandler {

        @Handles(COMMAND_NAME)
        public void handle(JsonEnvelope envelope) {
            doHandle(envelope);
        }

    }

    @ServiceComponent(COMMAND_HANDLER)
    public static class TestCommandHandlerDuplicate {

        @Handles(COMMAND_NAME)
        public void handle(JsonEnvelope envelope) {
        }

    }

    @ServiceComponent(COMMAND_HANDLER)
    public static class TestCommandHandlerWithWrongHandler {

        @Handles(COMMAND_NAME)
        public void handle1(JsonEnvelope envelope, Object invalidSecondArgument) {
        }

    }

    @ServiceComponent(COMMAND_HANDLER)
    public static class TestCommandHandlerWithWrongSynchronousHandler {

        @Handles(COMMAND_NAME)
        public JsonEnvelope handle1(JsonEnvelope envelope, Object invalidSecondArgument) {
            return null;
        }

    }

    @ServiceComponent(COMMAND_HANDLER)
    public static class TestCommandHandlerWithSynchronousHandler extends RecordingTestHandler {

        @Handles(COMMAND_NAME)
        public JsonEnvelope handle1(JsonEnvelope envelope) {
            doHandle(envelope);
            return envelope;
        }

    }

    @ServiceComponent(COMMAND_HANDLER)
    public static class TestCommandHandlerWithSynchronousHandlerDuplicate {

        @Handles(COMMAND_NAME)
        public JsonEnvelope handle1(JsonEnvelope envelope) {
            return envelope;
        }

    }

    @ServiceComponent(COMMAND_HANDLER)
    public static class TestCommandHandlerWithAsynchronousHandler {

        @Handles(COMMAND_NAME)
        public void handle1(JsonEnvelope envelope) {

        }

    }

    @ServiceComponent(COMMAND_HANDLER)
    public static class TestCommandHandlerWithWrongParameter {

        @Handles(COMMAND_NAME)
        public void handle1(Object invalidSecondArgument) {
        }

    }

    @ServiceComponent(COMMAND_HANDLER)
    public static class TestCommandSynchronousHandlerWithWrongParameter {

        @Handles(COMMAND_NAME)
        public void handle1(Object invalidSecondArgument) {
        }

    }

    @ServiceComponent(COMMAND_HANDLER)
    public static class TestAllEventsHandler extends RecordingTestHandler {

        @Handles("*")
        public void handle(JsonEnvelope envelope) {
            doHandle(envelope);
        }

    }

}
