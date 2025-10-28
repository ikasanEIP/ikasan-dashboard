package org.ikasan.job.orchestration.util.deserialise;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Custom JsonSerializer implementation for serializing a list of SchedulerJob objects in a sorted manner based on the job name or hash code.
 */
public class SortedSchedulerJobListSerializer extends JsonSerializer<List<SchedulerJob>> {

    @Override
    public void serialize(List<SchedulerJob> list, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        if (list != null) {
            Collections.sort(list, (a, b) -> {
                if (a.getIdentifier() != null && b.getIdentifier() != null) {
                    return a.getJobName().compareTo(b.getJobName());
                } else {
                    return ((Integer) a.hashCode()).compareTo(b.hashCode());
                }
            });
        }

        serializerProvider.defaultSerializeValue(list, jsonGenerator);
    }

    @Override
    public boolean isEmpty(SerializerProvider provider, List<SchedulerJob> list) {
        if (list == null || list.isEmpty()) {
            return true;
        }
        return false;
    }
}
