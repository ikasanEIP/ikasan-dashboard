package org.ikasan.job.orchestration.builder.util;

import org.ikasan.spec.scheduled.context.model.ContextTemplate;

import java.util.Comparator;
import java.util.List;
import java.util.OptionalInt;
import java.util.stream.Collectors;

public class ContextTemplateUtils
{
    /**
     * Protect any existing ordinals (non -1).
     * If an ordinal is not set, assign it a value greater than any current ordinal, in the order of appearance in the
     *      collection.
     * Reorder the collection to respect the ordinal.
     * @param contexts to be updated.
     * @return The reordered and ContextTemplates or the input param if it was null / empty.
     */
    public static List<ContextTemplate> setOrdinalsInContextTemplates(List<ContextTemplate> contexts) {
        List<ContextTemplate> sortedDirContexts = contexts;
        if (contexts != null && !contexts.isEmpty()) {
            OptionalInt max = contexts.stream().mapToInt(ContextTemplate::getOrdinal).max();
            int startIndex = Math.max(max.orElse(0) + 1,0);

            for(ContextTemplate contextTemplate:contexts) {
                if (contextTemplate.getOrdinal() == -1) {
                    contextTemplate.setOrdinal(startIndex);
                    startIndex++;
                }
            }
            sortedDirContexts = contexts.stream()
                .sorted(Comparator.comparingInt(ContextTemplate::getOrdinal))
                .collect(Collectors.toList());
        }
        return sortedDirContexts;
    }
}
