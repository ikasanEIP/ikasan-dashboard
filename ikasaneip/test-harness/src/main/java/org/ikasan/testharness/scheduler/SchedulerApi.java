package org.ikasan.testharness.scheduler;

import org.ikasan.testharness.support.DashboardApi;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * The dashboard's scheduler REST api, for test set-up, waiting and assertions (JWT via {@link DashboardApi}).
 * Operator actions under test go through the UI; this is for what the UI cannot do or would make slow.
 * <ul>
 *     <li>{@code PUT /rest/module/metadata}: register a scheduler agent</li>
 *     <li>{@code /rest/contextMachine/{id}/...}: status of a live instance (404 once it has left the cache, e.g. ended)</li>
 *     <li>{@code GET /rest/contextStatus/json/allInstance}: live instances</li>
 *     <li>{@code PUT /rest/event/globalScheduled}: raise a global event</li>
 * </ul>
 */
public class SchedulerApi
{
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final DashboardApi api;

    public SchedulerApi(DashboardApi api)
    {
        this.api = api;
    }

    public DashboardApi dashboard()
    {
        return api;
    }

    private static String enc(String s)
    {
        return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
    }

    // ------------------------------------------------------------------------------------------------- agents

    /** Register (or update) a scheduler agent: module metadata of type SCHEDULER_AGENT pointing at the agent's url. */
    public void registerAgent(String name, String url, String description)
    {
        java.net.URI uri = java.net.URI.create(url);
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("name", name);
        metadata.put("type", "SCHEDULER_AGENT");
        metadata.put("url", url);
        metadata.put("protocol", uri.getScheme());
        metadata.put("host", uri.getHost());
        metadata.put("port", uri.getPort());
        metadata.put("context", uri.getPath().replaceFirst("^/", ""));
        metadata.put("description", description);
        metadata.put("flows", List.of());
        api.request("PUT", "/rest/module/metadata", JSON.writeValueAsString(metadata));
    }

    /**
     * Wait until an agent has registered itself (module metadata, read with
     * {@code GET /rest/data-sharing/module-metadata}) with the given url. The real agent registers itself on start.
     */
    public void awaitAgentRegistered(String name, String url, Duration timeout)
    {
        waitFor(() -> {
            HttpResponse<String> res = api.call("GET", "/rest/data-sharing/module-metadata?moduleNames=" + enc(name), null);
            if (res.statusCode() != 200)
            {
                return "HTTP " + res.statusCode();
            }
            for (JsonNode module : JSON.readTree(res.body()).path("data"))
            {
                if (name.equals(module.path("name").asString()))
                {
                    return module.path("url").asString();
                }
            }
            return "<not registered>";
        }, url, "registration of agent " + name, timeout);
    }

    // ------------------------------------------------------------------------------------------------- job plans

    /** Does a job plan of this name exist? ({@code GET /rest/export/context/{name}}) */
    public boolean planExists(String name)
    {
        return api.call("GET", "/rest/export/context/" + enc(name), null).statusCode() == 200;
    }

    // ------------------------------------------------------------------------------------------------- instances

    /** A live instance as reported by {@code /rest/contextStatus/json/allInstance}. */
    public record Instance(String contextName, String contextInstanceId, String instanceStatus) {}

    /** Live instances (including PREPARED ones when asked). */
    public List<Instance> instances(boolean includePrepared)
    {
        HttpResponse<String> res = api.call("GET", "/rest/contextStatus/json/allInstance?includePrepared=" + includePrepared, null);
        List<Instance> out = new ArrayList<>();
        if (res.statusCode() != 200 || res.body().isBlank())
        {
            return out;
        }
        JsonNode root = JSON.readTree(res.body());
        collectInstances(root, out);
        return out;
    }

    private static void collectInstances(JsonNode node, List<Instance> out)
    {
        if (node.isArray())
        {
            node.forEach(n -> collectInstances(n, out));
        }
        else if (node.isObject())
        {
            if (node.has("contextInstanceId"))
            {
                out.add(new Instance(node.path("contextName").asString(), node.path("contextInstanceId").asString(),
                    node.path("instanceStatus").asString()));
            }
            else
            {
                node.forEach(n -> collectInstances(n, out));
            }
        }
    }

    /** Status of a live instance (e.g. WAITING, RUNNING, COMPLETE, ERROR); empty when it is no longer live (ended). */
    public Optional<String> instanceStatus(String contextInstanceId)
    {
        HttpResponse<String> res = api.call("GET", "/rest/contextMachine/" + enc(contextInstanceId) + "/instanceStatus", null);
        if (res.statusCode() != 200)
        {
            return Optional.empty();
        }
        // {"contextName":..,"instanceStatus":"WAITING","contextInstanceStatuses":[..child contexts..],...}
        JsonNode root = JSON.readTree(res.body());
        return Optional.of(root.isObject() ? root.path("instanceStatus").asString() : unquote(res.body()));
    }

    /**
     * Status of a job in a live instance.
     *
     * @param childContextName the (child) context the job is in
     * @param jobIdentifier    agentName-jobName, e.g. scheduler-agent-okA
     */
    public Optional<String> jobStatus(String contextInstanceId, String childContextName, String jobIdentifier)
    {
        HttpResponse<String> res = api.call("GET", "/rest/contextMachine/" + enc(contextInstanceId) + "/jobStatus/"
            + enc(childContextName) + "/" + enc(jobIdentifier), null);
        return res.statusCode() == 200 ? Optional.of(unquote(res.body())) : Optional.empty();
    }

    private static String unquote(String body)
    {
        return body.trim().replaceAll("^\"|\"$", "");
    }

    /** Wait until the job reaches the status. */
    public void waitForJob(String contextInstanceId, String childContextName, String jobIdentifier, String status,
                           Duration timeout)
    {
        waitFor(() -> jobStatus(contextInstanceId, childContextName, jobIdentifier).orElse("<not live>"), status,
            "job " + jobIdentifier + " of instance " + contextInstanceId, timeout);
    }

    /** Wait until the instance reaches the status; {@code ENDED} means it has left the live cache. */
    public void waitForInstance(String contextInstanceId, String status, Duration timeout)
    {
        waitFor(() -> instanceStatus(contextInstanceId).orElse("ENDED"), status, "instance " + contextInstanceId, timeout);
    }

    private static void waitFor(Supplier<String> current, String wanted, String what, Duration timeout)
    {
        long deadline = System.nanoTime() + timeout.toNanos();
        String last = null;
        while (System.nanoTime() < deadline)
        {
            last = current.get();
            if (wanted.equals(last))
            {
                return;
            }
            sleep(250);
        }
        throw new AssertionError(what + " did not reach " + wanted + " within " + timeout + " (last status: " + last + ")");
    }

    // ------------------------------------------------------------------------------------------------- events

    /** Raise a global event: every live instance with a GLOBAL_EVENT-&lt;name&gt; job receives it. */
    public void raiseGlobalEvent(String name)
    {
        api.request("PUT", "/rest/event/globalScheduled", name);
    }

    /**
     * An operator action on one job of a live instance via {@code PUT /rest/contextMachine/{id}/{action}}, e.g.
     * hold, release, skip, disable, reinstateDisabled, reset. For test set-up only - the actions under test use the UI.
     */
    public void jobAction(String contextInstanceId, String action, String jobIdentifier, String childContextName)
    {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("jobIdentifier", jobIdentifier);
        dto.put("childContextName", childContextName);
        dto.put("skipFlag", "skip".equals(action));
        api.request("PUT", "/rest/contextMachine/" + enc(contextInstanceId) + "/" + action, JSON.writeValueAsString(dto));
    }

    private static void sleep(long millis)
    {
        try
        {
            Thread.sleep(millis);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
    }
}
