package uk.gov.justice.services.core.extension;

import static uk.gov.justice.services.core.annotation.ComponentNameUtil.componentFrom;
import static uk.gov.justice.services.core.annotation.ServiceComponentLocation.componentLocationFrom;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.justice.services.core.annotation.FrameworkComponent;
import uk.gov.justice.services.core.annotation.Provider;
import uk.gov.justice.services.core.annotation.ServiceComponent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.util.AnnotationLiteral;

/**
 * Scans all beans and processes framework specific annotations.
 */
public class AnnotationScanner implements Extension {

    private List<Object> events = Collections.synchronizedList(new ArrayList<>());
    private List<Object> providers = Collections.synchronizedList(new ArrayList<>());

    @SuppressWarnings("unused")
    <T> void processAnnotatedType(@Observes final ProcessAnnotatedType<T> pat) {
        final AnnotatedType<T> annotatedType = pat.getAnnotatedType();
        if (annotatedType.isAnnotationPresent(Event.class)) {
            events.add(new EventFoundEvent(annotatedType.getJavaClass(), annotatedType.getAnnotation(Event.class).value()));
        }
    }

    @SuppressWarnings("unused")
    void afterDeploymentValidation(@Observes final AfterDeploymentValidation event, final BeanManager beanManager) {
        beanManager.getBeans(Object.class, annotationLiteral()).stream()
                .filter(b -> b.getBeanClass().isAnnotationPresent(ServiceComponent.class)
                        || b.getBeanClass().isAnnotationPresent(FrameworkComponent.class))
                .forEach(this::processServiceComponentsForEvents);

        beanManager.getBeans(Object.class, annotationLiteral()).stream()
                .filter(b -> b.getBeanClass().isAnnotationPresent(Provider.class))
                .forEach(this::processProviderForEvents);

        fireAllCollectedEvents(beanManager);
    }

    private AnnotationLiteral<Any> annotationLiteral() {
        return new AnnotationLiteral<Any>() {
            private static final long serialVersionUID = -3118797828842400134L;
        };
    }

    /**
     * Processes bean for annotations and adds events to the list.
     *
     * @param bean a bean that has an annotation and could be of interest to the framework wiring.
     */
    private void processServiceComponentsForEvents(final Bean<?> bean) {
        final Class<?> clazz = bean.getBeanClass();
        events.add(new ServiceComponentFoundEvent(componentFrom(clazz), bean, componentLocationFrom(clazz)));
    }

    private void processProviderForEvents(final Bean<?> bean) {
        providers.add(new ProviderFoundEvent(bean));
    }

    private void fireAllCollectedEvents(final BeanManager beanManager) {
        Stream.concat(events.stream(), providers.stream()).forEach(beanManager::fireEvent);
    }
}
