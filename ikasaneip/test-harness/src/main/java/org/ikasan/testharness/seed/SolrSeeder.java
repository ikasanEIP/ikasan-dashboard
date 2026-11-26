package org.ikasan.testharness.seed;

import org.ikasan.testharness.support.Timeouts;
import org.ikasan.testharness.config.HarnessConfig;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Solr seeder: plain HTTP against the {@code ikasan} core using the JSON update and select handlers. */
public class SolrSeeder implements Seeder
{
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final String base;
    private final String auth;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Timeouts.seconds(10)).build();

    public SolrSeeder(HarnessConfig config)
    {
        HarnessConfig.Solr solr = config.solr();
        this.base = solr.url().replaceAll("/$", "") + "/" + solr.core();
        this.auth = "Basic " + Base64.getEncoder()
            .encodeToString((solr.username() + ":" + solr.password()).getBytes(StandardCharsets.UTF_8));
    }

    private JsonNode request(String path, Object body)
    {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + path))
            .timeout(Timeouts.of(Duration.ofMinutes(2)))
            .header("Authorization", auth)
            .header("Content-Type", "application/json");
        b = body == null ? b.GET() : b.POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body)));
        try
        {
            HttpResponse<String> res = http.send(b.build(), HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() >= 300)
            {
                throw new IllegalStateException("Solr " + path + " failed: HTTP " + res.statusCode() + " "
                    + res.body().substring(0, Math.min(500, res.body().length())));
            }
            return JSON.readTree(res.body());
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Solr " + path + " failed", e);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private static String quote(String v)
    {
        return "\"" + v.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static String anyOf(String field, List<String> values)
    {
        return field + ":(" + values.stream().map(SolrSeeder::quote).collect(Collectors.joining(" OR ")) + ")";
    }

    private static String enc(String s)
    {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    @Override
    public void wipe(List<String> preserve)
    {
        String query = preserve.isEmpty() ? "*:*" : "*:* -" + anyOf("type", preserve) + " -" + anyOf("id", preserve);
        request("/update?commit=true", Map.of("delete", Map.of("query", query)));
    }

    @Override
    public void load(List<Map<String, Object>> documents)
    {
        for (int i = 0; i < documents.size(); i += 1000)
        {
            request("/update?commit=true", documents.subList(i, Math.min(i + 1000, documents.size())));
        }
    }

    @Override
    public void delete(List<String> ids)
    {
        request("/update?commit=true", Map.of("delete", ids));
    }

    @Override
    public long count(String type)
    {
        return request("/select?rows=0&wt=json&q=" + enc("type:" + quote(type)), null)
            .path("response").path("numFound").asLong();
    }

    @Override
    public List<Map<String, Object>> dump(List<String> types)
    {
        String q = types == null || types.isEmpty() ? "*:*" : anyOf("type", types);
        List<Map<String, Object>> out = new ArrayList<>();
        String cursor = "*";
        while (true)
        {
            JsonNode r = request("/select?rows=1000&sort=id+asc&wt=json&q=" + enc(q) + "&cursorMark=" + enc(cursor), null);
            for (JsonNode d : r.path("response").path("docs"))
            {
                Map<String, Object> doc = JSON.convertValue(d, new TypeReference<LinkedHashMap<String, Object>>() {});
                doc.remove("_version_");
                out.add(doc);
            }
            String next = r.path("nextCursorMark").asString();
            if (next.equals(cursor))
            {
                return out;
            }
            cursor = next;
        }
    }

    @Override
    public void close() {}
}
