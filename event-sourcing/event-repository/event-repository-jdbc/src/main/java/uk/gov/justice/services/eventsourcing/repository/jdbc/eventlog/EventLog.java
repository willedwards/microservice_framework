package uk.gov.justice.services.eventsourcing.repository.jdbc.eventlog;

import lombok.*;

import java.util.Objects;
import java.util.UUID;

/**
 * Entity class to represent a persisted event.
 */
@Getter
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class EventLog {

    private final UUID id;
    private final UUID streamId;
    private final Long sequenceId;
    private final String name;
    private final String payload;
    private final String metadata;

}