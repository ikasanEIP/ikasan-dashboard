package org.ikasan.testharness.seed;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves time tokens against a fixed anchor, so datasets and expectations can be written relative to "now"
 * and behave the same on any day.
 * <pre>
 *   {{now}}                  the anchor, epoch millis
 *   {{now-2h}} {{now+30m}}   offset from the anchor; units s m h d w
 *   {{today@09:30}}          the anchor's date (system time zone) at the given time
 *   {{now-1d|iso}}           with a format: ms (default) | s | iso | date (yyyy-MM-dd) | time (HH:mm)
 *                            | table (as dashboard grids show it: dd/MM/yyyy HH:mm:ss.SSS [zone - abbreviation])
 * </pre>
 * A string that is exactly one token resolves to a number for the ms/s formats and to a string otherwise.
 * Tokens inside a longer string are substituted as text.
 */
public final class Clock
{
    private static final Pattern TOKEN = Pattern.compile(
        "\\{\\{\\s*(now|today)(?:@(\\d{1,2}):(\\d{2}))?\\s*(?:([+-])\\s*(\\d+)\\s*([smhdw]))?\\s*(?:\\|\\s*(ms|s|iso|date|time|table))?\\s*}}");

    /** org.ikasan.dashboard.ui.util.DateFormatter.DATE_FORMAT_TABLE_VIEWS */
    private static final DateTimeFormatter TABLE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss.SSS '['VV '-' z']'");

    private final long anchor;
    private final ZoneId zone;

    public Clock(long anchorMillis)
    {
        this(anchorMillis, ZoneId.systemDefault());
    }

    public Clock(long anchorMillis, ZoneId zone)
    {
        this.anchor = anchorMillis;
        this.zone = zone;
    }

    /** A clock anchored at the given ISO instant, or now when blank. */
    public static Clock anchoredAt(String isoInstant)
    {
        return anchoredAt(isoInstant, ZoneId.systemDefault());
    }

    /** A clock anchored at the given ISO instant (or now when blank), formatting times in the given zone. */
    public static Clock anchoredAt(String isoInstant, ZoneId zone)
    {
        return new Clock(isoInstant == null || isoInstant.isBlank() ? System.currentTimeMillis()
            : Instant.parse(isoInstant).toEpochMilli(), zone);
    }

    public long anchor()
    {
        return anchor;
    }

    /** Resolve tokens in any JSON-like value (String, Map, List, other values unchanged). */
    @SuppressWarnings("unchecked")
    public <T> T resolve(T value)
    {
        if (value instanceof String s)
        {
            return (T) resolveString(s);
        }
        if (value instanceof Map<?, ?> map)
        {
            Map<String, Object> out = new LinkedHashMap<>();
            map.forEach((k, v) -> out.put(String.valueOf(k), resolve(v)));
            return (T) out;
        }
        if (value instanceof List<?> list)
        {
            List<Object> out = new ArrayList<>(list.size());
            list.forEach(v -> out.add(resolve(v)));
            return (T) out;
        }
        return value;
    }

    public Object resolveString(String value)
    {
        Matcher whole = TOKEN.matcher(value.trim());
        if (whole.matches())
        {
            return format(evaluate(whole), whole.group(7));
        }
        Matcher m = TOKEN.matcher(value);
        StringBuilder sb = new StringBuilder();
        while (m.find())
        {
            m.appendReplacement(sb, Matcher.quoteReplacement(String.valueOf(format(evaluate(m), m.group(7)))));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /** Epoch millis of a single token, e.g. {@code millis("{{now-1d}}")}. */
    public long millis(String token)
    {
        Matcher m = TOKEN.matcher(token.trim());
        if (!m.matches())
        {
            throw new IllegalArgumentException("Not a time token: " + token);
        }
        return evaluate(m);
    }

    private long evaluate(Matcher m)
    {
        long t = anchor;
        if ("today".equals(m.group(1)))
        {
            LocalDate date = Instant.ofEpochMilli(anchor).atZone(zone).toLocalDate();
            LocalTime time = m.group(2) == null ? LocalTime.MIDNIGHT
                : LocalTime.of(Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)));
            t = date.atTime(time).atZone(zone).toInstant().toEpochMilli();
        }
        if (m.group(4) != null)
        {
            long amount = Long.parseLong(m.group(5)) * unitMillis(m.group(6));
            t += "-".equals(m.group(4)) ? -amount : amount;
        }
        return t;
    }

    private static long unitMillis(String unit)
    {
        return switch (unit)
        {
            case "s" -> 1_000L;
            case "m" -> 60_000L;
            case "h" -> 3_600_000L;
            case "d" -> 86_400_000L;
            case "w" -> 604_800_000L;
            default -> throw new IllegalArgumentException("Unknown time unit " + unit);
        };
    }

    private Object format(long t, String fmt)
    {
        if (fmt == null || fmt.equals("ms"))
        {
            return t;
        }
        return switch (fmt)
        {
            case "s" -> t / 1000;
            case "iso" -> Instant.ofEpochMilli(t).toString();
            case "date" -> DateTimeFormatter.ISO_LOCAL_DATE.format(Instant.ofEpochMilli(t).atZone(zone));
            case "time" -> DateTimeFormatter.ofPattern("HH:mm").format(Instant.ofEpochMilli(t).atZone(zone));
            case "table" -> TABLE.format(Instant.ofEpochMilli(t).atZone(zone));
            default -> throw new IllegalArgumentException("Unknown token format " + fmt);
        };
    }
}
