package uk.gov.justice.services.adapter.messaging;

import static java.lang.String.format;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.services.core.json.JsonValidationLogger.toValidationTrace;
import static uk.gov.justice.services.messaging.jms.HeaderConstants.JMS_HEADER_CPPNAME;
import static uk.gov.justice.services.messaging.logging.JmsMessageLoggerHelper.toJmsTraceString;

import uk.gov.justice.services.core.json.JsonSchemaValidator;

import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;
import javax.jms.TextMessage;

import org.everit.json.schema.ValidationException;
import org.slf4j.Logger;

/**
 * Interceptor for validating messages against a JSON schema.
 */
public class JsonSchemaValidationInterceptor {

    private static final Logger LOGGER = getLogger(JsonSchemaValidationInterceptor.class);

    @Inject
    JsonSchemaValidator validator;

    @AroundInvoke
    protected Object validate(final InvocationContext context) throws Exception {
        final Object[] parameters = context.getParameters();

        if (parameters.length != 1) {
            throw new IllegalArgumentException("JSON validation interceptor can only be used on single argument methods");
        }

        if (!(parameters[0] instanceof TextMessage)) {
            throw new IllegalArgumentException(
                    format("JSON validation interceptor can only be used on a JMS TextMessage, not %s", parameters[0].getClass().getName()));
        }

        final TextMessage message = (TextMessage) parameters[0];
        try {
            validator.validate(message.getText(), message.getStringProperty(JMS_HEADER_CPPNAME));
        } catch (ValidationException validationException) {
            LOGGER.debug(format("JSON schema validation has failed for %s due to %s",
                    toJmsTraceString(message),
                    toValidationTrace(validationException)));
            throw validationException;
        }


        return context.proceed();
    }
}
