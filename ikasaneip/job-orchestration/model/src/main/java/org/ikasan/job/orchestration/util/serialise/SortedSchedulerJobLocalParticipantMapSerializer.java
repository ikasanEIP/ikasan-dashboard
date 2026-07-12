package org.ikasan.job.orchestration.util.serialise;

import org.ikasan.spec.scheduled.context.model.Context;
import org.ikasan.spec.scheduled.job.model.SchedulerJobLockParticipant;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

import java.util.*;

/**
 * Custom JSON serializer for sorting and serializing a Map of job names to lists of lock participants.
 * This serializer ensures the output JSON is sorted based on the natural order of the job names.
 */
public class SortedSchedulerJobLocalParticipantMapSerializer extends ValueSerializer<Map<String, List<SchedulerJobLockParticipant>>> {

    @Override
    public void serialize(Map<String, List<SchedulerJobLockParticipant>> value, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
        SortedMap<String, List<SchedulerJobLockParticipant>> sortedMap = new TreeMap<>(Comparator.naturalOrder());
        if (value != null) {
            sortedMap.putAll(value);

            sortedMap.entrySet()
                .forEach(entry -> entry.getValue().sort(Comparator.comparing(SchedulerJobLockParticipant::getJobName)));
        }

        gen.writeStartObject();
        for (Map.Entry<String, List<SchedulerJobLockParticipant>> entry : sortedMap.entrySet()) {
            gen.writeName(entry.getKey());
            gen.writeStartArray();
            for (SchedulerJobLockParticipant participant : entry.getValue()) {
                ctxt.writeValue(gen, participant);
            }
            gen.writeEndArray();
        }
        gen.writeEndObject();
    }

    @Override
    public boolean isEmpty(SerializationContext ctxt, Map<String, List<SchedulerJobLockParticipant>> value) {
        return value == null || value.isEmpty();
    }
}
