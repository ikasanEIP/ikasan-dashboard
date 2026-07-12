package org.ikasan.job.orchestration.util.serialise;

import org.ikasan.spec.scheduled.context.model.Context;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

import java.util.Collections;
import java.util.List;

/**
 * Custom JsonSerializer implementation for serializing a list of SchedulerJob objects in a sorted manner based on the
 * job name or hash code.
 */
public class SortedSchedulerJobListSerializer extends ValueSerializer<List<SchedulerJob>> {

    @Override
    public void serialize(List<SchedulerJob> list, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
        if (list != null) {
            // We set the ordinals on export if necessary
            for(int i=0; i<list.size(); i++) {
                if(list.get(i).getOrdinal() == -1) {
                    list.get(i).setOrdinal(i);
                }
            }

            Collections.sort(list, (a, b) -> {
                if (a.getIdentifier() != null && b.getIdentifier() != null) {
                    return a.getJobName().compareTo(b.getJobName());
                } else {
                    return ((Integer) a.hashCode()).compareTo(b.hashCode());
                }
            });
        }

        gen.writeStartArray();
        if (list != null) {
            for (SchedulerJob job : list) {
                ctxt.writeValue(gen, job);
            }
        }
        gen.writeEndArray();
    }

    @Override
    public boolean isEmpty(SerializationContext ctxt, List<SchedulerJob> value) {
        return value == null || value.isEmpty();
    }
}
