package uk.gov.justice.services.eventsourcing.repository.jdbc.eventlog;

import lombok.*;

import java.util.UUID;

/**
 * Entity class to represent a persisted event.
 */
@Value
@EqualsAndHashCode
@ToString(includeFieldNames=true)
public class EventLog {


    private UUID id;
    private UUID streamId;
    private Long sequenceId;
    private String name;
    private String payload;
    private String metadata;

}