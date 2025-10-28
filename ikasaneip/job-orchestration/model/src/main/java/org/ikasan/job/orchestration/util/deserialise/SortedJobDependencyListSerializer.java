package org.ikasan.job.orchestration.util.deserialise;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.ikasan.spec.scheduled.context.model.And;
import org.ikasan.spec.scheduled.context.model.JobDependency;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Custom JSON serializer for sorting and serializing a list of JobDependency objects in ascending order based on job identifiers.
 * Extends the JsonSerializer class.
 */
public class SortedJobDependencyListSerializer extends JsonSerializer<List<JobDependency>> {

    @Override
    public void serialize(List<JobDependency> list, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        if (list != null) {
            Collections.sort(list, (a, b) -> {
                if (a.getJobIdentifier() != null && b.getJobIdentifier() != null) {
                    return a.getJobIdentifier().compareTo(b.getJobIdentifier());
                } else {
                    return ((Integer) a.hashCode()).compareTo(b.hashCode());
                }
            });
        }

        serializerProvider.defaultSerializeValue(list, jsonGenerator);
    }

    @Override
    public boolean isEmpty(SerializerProvider provider, List<JobDependency> list) {
        if (list == null || list.isEmpty()) {
            return true;
        }
        return false;
    }
}
