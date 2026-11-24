package org.ikasan.testharness.support;

import org.ikasan.testharness.config.HarnessConfig;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The run's tag selection ({@code run.include} / {@code run.exclude}). Tags compare case-insensitively and a
 * leading {@code @} is ignored, so {@code smoke}, {@code @smoke} and {@code SMOKE} are the same tag.
 */
public final class TagFilter
{
    private final Set<String> include;
    private final Set<String> exclude;

    public TagFilter(List<String> include, List<String> exclude)
    {
        this.include = normalise(include);
        this.exclude = normalise(exclude);
    }

    public static TagFilter of(HarnessConfig config)
    {
        return new TagFilter(config.run().include(), config.run().exclude());
    }

    private static Set<String> normalise(Collection<String> tags)
    {
        return tags == null ? Set.of() : tags.stream().map(TagFilter::normalise).collect(Collectors.toSet());
    }

    public static String normalise(String tag)
    {
        return tag.trim().replaceFirst("^@", "").toLowerCase(Locale.ROOT);
    }

    /** True when a test or scenario with these tags should run. */
    public boolean accepts(Collection<String> tags)
    {
        Set<String> t = normalise(tags);
        if (t.stream().anyMatch(exclude::contains))
        {
            return false;
        }
        return include.isEmpty() || t.stream().anyMatch(include::contains);
    }

    public boolean isUnrestricted()
    {
        return include.isEmpty() && exclude.isEmpty();
    }
}
