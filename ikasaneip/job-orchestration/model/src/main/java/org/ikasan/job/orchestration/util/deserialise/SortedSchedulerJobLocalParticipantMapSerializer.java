package org.ikasan.job.orchestration.util.deserialise;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.ikasan.spec.scheduled.job.model.SchedulerJobLockParticipant;

import java.io.IOException;
import java.util.*;

/**
 * Custom JSON serializer for sorting and serializing a Map of job names to lists of lock participants.
 * This serializer ensures the output JSON is sorted based on the natural order of the job names.
 */
public class SortedSchedulerJobLocalParticipantMapSerializer extends JsonSerializer<Map<String, List<SchedulerJobLockParticipant>>> {

    @Override
    public void serialize(Map<String, List<SchedulerJobLockParticipant>> map, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        SortedMap<String, List<SchedulerJobLockParticipant>> sortedMap = new TreeMap(Comparator.naturalOrder());
        sortedMap.putAll(map);

        sortedMap.entrySet()
            .forEach(entry -> entry.getValue().sort(Comparator.comparing(SchedulerJobLockParticipant::getJobName)));

        serializerProvider.defaultSerializeValue(sortedMap, jsonGenerator);
    }

    @Override
    public boolean isEmpty(SerializerProvider provider, Map<String, List<SchedulerJobLockParticipant>> map) {
        if (map == null || map.isEmpty()) {
            return true;
        }
        return false;
    }
}
