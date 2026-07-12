package org.ikasan.job.orchestration.util.serialise;

import org.ikasan.spec.scheduled.context.model.Context;
import org.ikasan.spec.scheduled.context.model.JobDependency;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

import java.util.Collections;
import java.util.List;

/**
 * Custom JSON serializer for sorting and serializing a list of JobDependency objects in ascending order based on job identifiers.
 * Extends the JsonSerializer class.
 */
public class SortedJobDependencyListSerializer extends ValueSerializer<List<JobDependency>> {

    @Override
    public void serialize(List<JobDependency> list, JsonGenerator jsonGenerator, SerializationContext ctxt) throws JacksonException {
        if (list != null) {
            Collections.sort(list, (a, b) -> {
                if (a.getJobIdentifier() != null && b.getJobIdentifier() != null) {
                    return a.getJobIdentifier().compareTo(b.getJobIdentifier());
                } else {
                    return ((Integer) a.hashCode()).compareTo(b.hashCode());
                }
            });
        }

        jsonGenerator.writeStartArray();
        if (list != null) {
            for (JobDependency jobDependency : list) {
                ctxt.writeValue(jsonGenerator, jobDependency);
            }
        }
        jsonGenerator.writeEndArray();
    }

    @Override
    public boolean isEmpty(SerializationContext ctxt, List<JobDependency> value) {
        return value == null || value.isEmpty();
    }
}
