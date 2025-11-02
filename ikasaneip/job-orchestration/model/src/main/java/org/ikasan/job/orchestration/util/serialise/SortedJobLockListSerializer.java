package org.ikasan.job.orchestration.util.serialise;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.ikasan.spec.scheduled.context.model.JobLock;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Custom JsonSerializer implementation for serializing a list of JobLock objects in a sorted manner based on the lock name.
 */
public class SortedJobLockListSerializer extends JsonSerializer<List<JobLock>> {

    @Override
    public void serialize(List<JobLock> list, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        if (list != null) {
            Collections.sort(list, (a, b) -> {
                if (a.getName() != null && b.getName() != null) {
                    return a.getName().compareTo(b.getName());
                } else {
                    return ((Integer) a.hashCode()).compareTo(b.hashCode());
                }
            });
        }

        serializerProvider.defaultSerializeValue(list, jsonGenerator);
    }

    @Override
    public boolean isEmpty(SerializerProvider provider, List<JobLock> list) {
        if (list == null || list.isEmpty()) {
            return true;
        }
        return false;
    }
}
