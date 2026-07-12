package org.ikasan.job.orchestration.util.serialise;

import org.ikasan.spec.scheduled.context.model.Context;
import org.ikasan.spec.scheduled.context.model.JobLock;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

import java.util.Collections;
import java.util.List;

/**
 * Custom ValueSerializer implementation for serializing a list of JobLock objects in a sorted manner based on the lock name.
 */
public class SortedJobLockListSerializer extends ValueSerializer<List<JobLock>> {

    @Override
    public void serialize(List<JobLock> list, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
        if (list != null) {
            Collections.sort(list, (a, b) -> {
                if (a.getName() != null && b.getName() != null) {
                    return a.getName().compareTo(b.getName());
                } else {
                    return ((Integer) a.hashCode()).compareTo(b.hashCode());
                }
            });
        }

        gen.writeStartArray();
        if (list != null) {
            for (JobLock lock : list) {
                ctxt.writeValue(gen, lock);
            }
        }
        gen.writeEndArray();
    }

    @Override
    public boolean isEmpty(SerializationContext ctxt, List<JobLock> value) {
        return value == null || value.isEmpty();
    }
}
