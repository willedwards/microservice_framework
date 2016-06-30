package uk.gov.justice.services.core.handler;

import static java.lang.String.format;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.services.messaging.logging.JsonEnvelopeLoggerHelper.toEnvelopeTraceString;
import static uk.gov.justice.services.messaging.logging.LoggerUtils.tracer;

import uk.gov.justice.services.core.handler.exception.HandlerExecutionException;
import uk.gov.justice.services.core.handler.registry.exception.InvalidHandlerException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.logging.Tracer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import org.slf4j.Logger;

/**
 * Encapsulates a handler class instance and a handler method.
 *
 * Asynchronous handler methods will return a null {@link Void} whereas synchronous handler methods
 * must return an {@link JsonEnvelope}.
 */
public class HandlerMethod {

    public static final boolean SYNCHRONOUS = true;
    public static final boolean ASYNCHRONOUS = false;

    private static final String DISPATCHING_TO_HANDLER_FORMAT = "Dispatching to handler %s.%s : %s";
    private static final String RESPONSE_RECEIVED_MESSAGE = "Response received from handler %s.%s : %s";
    private static final String VOID_RESPONSE_MESSAGE = "Response from handler %s.%s with id %s was void";

    private final Object handlerInstance;
    private final Method handlerMethod;

    private final boolean isSynchronous;
    private final Tracer logger;

    public static HandlerMethod handlerMethod(final Object object, final Method method, final Class<?> expectedReturnType) {
        return new HandlerMethod(object, method, expectedReturnType, tracer());
    }

    /**
     * Constructor with handler method validator.
     *
     * @param object             the instance of the handler object
     * @param method             the method on the handler object
     * @param expectedReturnType the expected return type for the method
     */
    HandlerMethod(final Object object, final Method method, final Class<?> expectedReturnType, final Function<Logger, Tracer> logger) {
        this.logger = logger.apply(getLogger(HandlerMethod.class));

        if (object == null) {
            throw new IllegalArgumentException("Handler instance cannot be null");
        }

        if (method == null) {
            throw new IllegalArgumentException("Handler method cannot be null");
        }

        final Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length != 1) {
            throw new InvalidHandlerException(
                    format("Handles method must have exactly one parameter; found %d", parameterTypes.length));
        }
        if (parameterTypes[0] != JsonEnvelope.class) {
            throw new IllegalArgumentException(
                    format("Handler methods must take an JsonEnvelope as the argument, not a %s", parameterTypes[0]));
        }

        this.isSynchronous = !isVoid(expectedReturnType);

        if (!isSynchronous && !isVoid(method.getReturnType())) {
            throw new InvalidHandlerException("Asynchronous handler must return void");
        }
        if (isSynchronous && !isEnvelope(expectedReturnType)) {
            throw new IllegalArgumentException("Synchronous handler method must handle envelopes");
        }
        if (isSynchronous && !isEnvelope(method.getReturnType())) {
            throw new InvalidHandlerException("Synchronous handler must return an envelope");
        }

        this.handlerInstance = object;
        this.handlerMethod = method;
    }

    private static boolean isVoid(final Class<?> clazz) {
        return Void.TYPE.equals(clazz);
    }

    private static boolean isEnvelope(final Class<?> clazz) {
        return JsonEnvelope.class.equals(clazz);
    }

    /**
     * Invokes the handler method passing the <code>envelope</code> to it.
     *
     * @param envelope the envelope that is passed to the handler method
     * @return the result of invoking the handler, which will either be an {@link JsonEnvelope} or a
     * null {@link Void}
     */
    @SuppressWarnings("unchecked")
    public Object execute(final JsonEnvelope envelope) {
        logger.trace(dispatchingToHandlerMessage(envelope));
        try {
            final Object obj = handlerMethod.invoke(handlerInstance, envelope);
            logger.trace(responseMessage(envelope, obj));
            return obj;
        } catch (IllegalAccessException | InvocationTargetException ex) {
            throw new HandlerExecutionException(
                    format("Error while invoking command handler method %s with parameter %s",
                            handlerMethod, envelope), ex.getCause());
        }
    }

    /**
     * Check if this handler method is synchronous.
     *
     * @return true if the method returns a value
     */
    public boolean isSynchronous() {
        return isSynchronous;
    }

    @Override
    public String toString() {
        return format("HandlerMethod[ Class: %s method: %s]",
                handlerInstance != null ? handlerInstance.getClass().getName() : null,
                handlerMethod != null ? handlerMethod.getName() : null);
    }

    private Supplier<String> dispatchingToHandlerMessage(final JsonEnvelope envelope) {
        return () -> format(DISPATCHING_TO_HANDLER_FORMAT,
                handlerInstance.getClass().getName(),
                handlerMethod.getName(),
                toEnvelopeTraceString(envelope));
    }

    private Supplier<String> responseMessage(final JsonEnvelope envelope, final Object obj) {
        return () -> {
            final Optional<Object> response = Optional.ofNullable(obj);

            if (response.isPresent() && response.get() instanceof JsonEnvelope) {
                return format(RESPONSE_RECEIVED_MESSAGE,
                        handlerInstance.getClass().toString(),
                        handlerMethod.getName(),
                        toEnvelopeTraceString((JsonEnvelope) response.get()));
            }

            return format(VOID_RESPONSE_MESSAGE,
                    handlerInstance.getClass().toString(),
                    handlerMethod.getName(),
                    envelope.metadata().id().toString());
        };
    }
}
