package org.ikasan.testharness.scheduler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.ikasan.testharness.support.DashboardApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A stand-in for an Ikasan scheduler agent, running inside the harness JVM.
 * <p>
 * The dashboard sends command jobs to the agent registered under the job's agent name
 * ({@code PUT <agentUrl>/rest/schedulerJobInitiation}); the agent runs the job and reports back by PUTting
 * {@code ContextualisedScheduledProcessEvent}s to the dashboard's {@code /rest/event/scheduled} - first
 * {@code jobStarting=true}, then the outcome. This class does the same, without running anything:
 * <ul>
 *     <li>the outcome comes from the job's {@code commandLine}, so job plan fixtures behave the same on a real agent:
 *     {@code exit N} (N &gt; 0) fails with return code N, {@code sleep S} keeps the job RUNNING for S seconds,
 *     anything else succeeds; {@code echo} output becomes the job's output</li>
 *     <li>tests can override that per job ({@link #script}), complete held jobs ({@link #complete}) and simulate
 *     quartz/file jobs firing on the agent ({@link #fireJob})</li>
 *     <li>every request from the dashboard is recorded ({@link #calls()}) for assertions</li>
 * </ul>
 * Every other agent endpoint the dashboard calls (job provisioning, context instance publication, dry run, ...)
 * is accepted with 200.
 */
public class FakeSchedulerAgent implements AutoCloseable
{
    private static final Logger logger = LoggerFactory.getLogger(FakeSchedulerAgent.class);
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final Pattern EXIT = Pattern.compile("\\bexit\\s+(\\d+)");
    private static final Pattern SLEEP = Pattern.compile("\\bsleep\\s+(\\d+)");
    private static final Pattern ECHO = Pattern.compile("\\becho\\s+([^;&|]*)");

    public static final String CONTEXT_PATH = "/scheduler-agent";

    /** A request the dashboard made to the agent. */
    public record Call(String method, String path, String body, long timestamp)
    {
        @SuppressWarnings("unchecked")
        public Map<String, Object> json()
        {
            return body == null || body.isBlank() ? Map.of() : JSON.readValue(body, Map.class);
        }
    }

    /** How a job run ends. */
    public sealed interface Outcome
    {
        record Succeed(String output) implements Outcome {}

        record Fail(int returnCode, String output) implements Outcome {}

        /** Stays RUNNING until {@link FakeSchedulerAgent#complete} (or killed). */
        record RunUntilCompleted() implements Outcome {}

        static Outcome succeed()
        {
            return new Succeed("");
        }

        static Outcome fail(int returnCode)
        {
            return new Fail(returnCode, "");
        }

        static Outcome runUntilCompleted()
        {
            return new RunUntilCompleted();
        }
    }

    private final String agentName;
    private final String username;
    private final String password;
    private final DashboardApi dashboard;
    private final int port;
    private final List<Call> calls = new CopyOnWriteArrayList<>();
    private final Map<String, Outcome> scripts = new ConcurrentHashMap<>();
    private final Map<Long, RunningJob> running = new ConcurrentHashMap<>();
    private final AtomicLong pids = new AtomicLong(40_000);
    private final ExecutorService workers = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "fake-agent-job");
        t.setDaemon(true);
        return t;
    });
    private HttpServer server;

    /** A job the agent is currently "running". */
    private record RunningJob(long pid, Map<String, Object> initiation, CountDownLatch done, Outcome[] result) {}

    /**
     * @param agentName the agent's name (the module name jobs refer to, normally {@code scheduler-agent})
     * @param port      port to listen on, on all interfaces (the dashboard container reaches it via host.docker.internal)
     * @param username  basic auth user the dashboard sends ({@code rest.module.username})
     * @param password  basic auth password ({@code rest.module.password})
     * @param dashboard client the agent posts its events with
     */
    public FakeSchedulerAgent(String agentName, int port, String username, String password, DashboardApi dashboard)
    {
        this.agentName = agentName;
        this.port = port;
        this.username = username;
        this.password = password;
        this.dashboard = dashboard;
    }

    public FakeSchedulerAgent start()
    {
        try
        {
            server = HttpServer.create(new InetSocketAddress(port), 0);
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Cannot start the fake scheduler agent on port " + port, e);
        }
        server.createContext(CONTEXT_PATH, this::handle);
        server.setExecutor(Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "fake-agent-http");
            t.setDaemon(true);
            return t;
        }));
        server.start();
        logger.info("fake scheduler agent '{}' listening on port {}", agentName, port);
        return this;
    }

    public String agentName()
    {
        return agentName;
    }

    // ------------------------------------------------------------------------------------------------- test API

    /** Override how the next runs of a job end (until {@link #clearScripts()}). */
    public void script(String jobName, Outcome outcome)
    {
        scripts.put(jobName, outcome);
    }

    public void clearScripts()
    {
        scripts.clear();
    }

    /** Every request the dashboard made, in order. */
    public List<Call> calls()
    {
        return List.copyOf(calls);
    }

    /** Requests matching method and path suffix, e.g. {@code calls("PUT", "/rest/schedulerJobInitiation")}. */
    public List<Call> calls(String method, String pathSuffix)
    {
        return calls.stream().filter(c -> c.method().equals(method) && c.path().endsWith(pathSuffix)).toList();
    }

    /** Job initiations received for a context instance, in order. */
    public List<Map<String, Object>> initiations(String contextInstanceId)
    {
        return calls("PUT", "/rest/schedulerJobInitiation").stream().map(Call::json)
            .filter(j -> Objects.equals(contextInstanceId, j.get("contextInstanceId"))).toList();
    }

    /** Wait until a request matching the predicate has been received. */
    public Call awaitCall(Predicate<Call> matching, Duration timeout)
    {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline)
        {
            for (Call c : calls)
            {
                if (matching.test(c))
                {
                    return c;
                }
            }
            sleep(100);
        }
        throw new AssertionError("The fake agent did not receive the expected request within " + timeout
            + "; received: " + calls.stream().map(c -> c.method() + " " + c.path()).toList());
    }

    /** Wait until the dashboard asks the agent to fire a quartz/file job now (UI Submit), for the correlation id. */
    public Call awaitFireNow(String jobName, String correlationId, Duration timeout)
    {
        // the flow is <jobName>_<contextName>
        return awaitCall(c -> c.path().matches("/rest/scheduler/[^/]+/" + Pattern.quote(jobName) + "(_[^/]*)?/"
            + Pattern.quote(correlationId) + ".*"), timeout);
    }

    /** Wait until the job has been initiated for the instance. */
    public Map<String, Object> awaitInitiation(String contextInstanceId, String jobName, Duration timeout)
    {
        return awaitCall(c -> c.method().equals("PUT") && c.path().endsWith("/rest/schedulerJobInitiation")
            && Objects.equals(contextInstanceId, c.json().get("contextInstanceId"))
            && Objects.equals(jobName, c.json().get("jobName")), timeout).json();
    }

    /** Finish a job that is running with {@link Outcome.RunUntilCompleted} (or a sleep), with the given outcome. */
    public void complete(String contextInstanceId, String jobName, Outcome outcome)
    {
        RunningJob job = running.values().stream()
            .filter(r -> Objects.equals(contextInstanceId, r.initiation().get("contextInstanceId"))
                && Objects.equals(jobName, r.initiation().get("jobName")))
            .findFirst().orElseThrow(() -> new IllegalStateException(jobName + " is not running on the fake agent"));
        job.result()[0] = outcome;
        job.done().countDown();
    }

    /**
     * Simulate a job that the agent itself triggers (a quartz or file watcher job) firing for a context instance:
     * posts the starting event and the outcome.
     */
    public void fireJob(String contextName, String contextInstanceId, String jobName, List<String> childContextNames,
                        Outcome outcome)
    {
        Map<String, Object> initiation = new LinkedHashMap<>();
        initiation.put("agentName", agentName);
        initiation.put("jobName", jobName);
        initiation.put("contextName", contextName);
        initiation.put("contextInstanceId", contextInstanceId);
        initiation.put("childContextNames", childContextNames);
        run(initiation, outcome);
    }

    // ------------------------------------------------------------------------------------------------- http

    private void handle(HttpExchange exchange) throws IOException
    {
        try (exchange)
        {
            String path = exchange.getRequestURI().getPath().substring(CONTEXT_PATH.length());
            String body = read(exchange.getRequestBody());
            calls.add(new Call(exchange.getRequestMethod(), path, body, System.currentTimeMillis()));
            logger.debug("agent <- {} {}", exchange.getRequestMethod(), path);

            if (!authorised(exchange))
            {
                respond(exchange, 401, "");
                return;
            }
            String method = exchange.getRequestMethod();
            if (method.equals("PUT") && path.equals("/rest/schedulerJobInitiation"))
            {
                Map<String, Object> initiation = JSON.readValue(body, new TypeReference<LinkedHashMap<String, Object>>() {});
                respond(exchange, 200, "");
                workers.submit(() -> run(initiation, null));
                return;
            }
            Matcher kill = Pattern.compile("^/rest/jobUtils/kill/(\\d+)").matcher(path);
            if (kill.find())
            {
                RunningJob job = running.get(Long.parseLong(kill.group(1)));
                if (job != null)
                {
                    job.result()[0] = new Outcome.Fail(-1, "killed");
                    job.done().countDown();
                }
                respond(exchange, 200, "");
                return;
            }
            respond(exchange, 200, "");
        }
        catch (RuntimeException e)
        {
            logger.error("fake agent failed to handle {} {}", exchange.getRequestMethod(), exchange.getRequestURI(), e);
            respond(exchange, 500, e.toString());
        }
    }

    private boolean authorised(HttpExchange exchange)
    {
        String header = exchange.getRequestHeaders().getFirst("Authorization");
        String expected = "Basic " + Base64.getEncoder()
            .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
        return expected.equals(header);
    }

    private static String read(InputStream in) throws IOException
    {
        return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException
    {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
        if (bytes.length > 0)
        {
            exchange.getResponseBody().write(bytes);
        }
    }

    // ------------------------------------------------------------------------------------------------- running jobs

    @SuppressWarnings("unchecked")
    private static String commandLine(Map<String, Object> initiation)
    {
        Object job = initiation.get("internalEventDrivenJob");
        return job instanceof Map<?, ?> m && m.get("commandLine") != null ? String.valueOf(m.get("commandLine")) : "";
    }

    /** The outcome a command line produces (the same rules a test can expect from a real agent). */
    static Outcome outcomeOf(String commandLine)
    {
        String output = "";
        Matcher echo = ECHO.matcher(commandLine);
        if (echo.find())
        {
            output = echo.group(1).trim();
        }
        Matcher exit = EXIT.matcher(commandLine);
        if (exit.find() && Integer.parseInt(exit.group(1)) != 0)
        {
            return new Outcome.Fail(Integer.parseInt(exit.group(1)), output);
        }
        return new Outcome.Succeed(output);
    }

    static long sleepSeconds(String commandLine)
    {
        Matcher sleep = SLEEP.matcher(commandLine);
        return sleep.find() ? Long.parseLong(sleep.group(1)) : 0;
    }

    /** Report a run: starting event, optional wait, then the outcome. */
    private void run(Map<String, Object> initiation, Outcome fixed)
    {
        String jobName = String.valueOf(initiation.get("jobName"));
        String commandLine = commandLine(initiation);
        Outcome outcome = fixed != null ? fixed : scripts.getOrDefault(jobName, outcomeOf(commandLine));
        long pid = pids.incrementAndGet();
        long fireTime = System.currentTimeMillis();
        try
        {
            if (Boolean.TRUE.equals(initiation.get("skipped")))
            {
                // like the real agent: a skipped job is not run, it is reported back as a skipped success
                postEvent(initiation, false, true, 0, "", pid, fireTime, System.currentTimeMillis());
                return;
            }
            postEvent(initiation, true, true, 0, "", pid, fireTime, 0);

            long sleepSeconds = sleepSeconds(commandLine);
            if (outcome instanceof Outcome.RunUntilCompleted || sleepSeconds > 0)
            {
                Outcome[] result = {outcome instanceof Outcome.RunUntilCompleted ? Outcome.succeed() : outcome};
                RunningJob job = new RunningJob(pid, initiation, new CountDownLatch(1), result);
                running.put(pid, job);
                try
                {
                    if (outcome instanceof Outcome.RunUntilCompleted)
                    {
                        job.done().await();
                    }
                    else
                    {
                        job.done().await(sleepSeconds, TimeUnit.SECONDS);
                    }
                }
                finally
                {
                    running.remove(pid);
                }
                outcome = result[0];
            }

            if (outcome instanceof Outcome.Fail fail)
            {
                postEvent(initiation, false, false, fail.returnCode(), fail.output(), pid, fireTime, System.currentTimeMillis());
            }
            else
            {
                String output = outcome instanceof Outcome.Succeed s ? s.output() : "";
                postEvent(initiation, false, true, 0, output, pid, fireTime, System.currentTimeMillis());
            }
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
        catch (RuntimeException e)
        {
            logger.error("fake agent could not report job {} of instance {}", jobName, initiation.get("contextInstanceId"), e);
        }
    }

    private void postEvent(Map<String, Object> initiation, boolean starting, boolean successful, int returnCode,
                           String output, long pid, long fireTime, long completionTime)
    {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("agentName", initiation.getOrDefault("agentName", agentName));
        event.put("jobName", initiation.get("jobName"));
        event.put("contextName", initiation.get("contextName"));
        event.put("contextInstanceId", initiation.get("contextInstanceId"));
        event.put("childContextNames", initiation.get("childContextNames"));
        event.put("commandLine", commandLine(initiation));
        event.put("jobStarting", starting);
        event.put("successful", successful);
        event.put("returnCode", returnCode);
        event.put("resultOutput", output);
        event.put("pid", pid);
        event.put("user", "harness");
        event.put("fireTime", fireTime);
        event.put("completionTime", completionTime);
        event.put("skipped", Boolean.TRUE.equals(initiation.get("skipped")));
        if (initiation.get("internalEventDrivenJob") != null)
        {
            event.put("internalEventDrivenJob", initiation.get("internalEventDrivenJob"));
        }
        logger.debug("agent -> event {} {} starting={} successful={}", initiation.get("contextInstanceId"),
            initiation.get("jobName"), starting, successful);
        dashboard.request("PUT", "/rest/event/scheduled", JSON.writeValueAsString(event));
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

    @Override
    public void close()
    {
        if (server != null)
        {
            server.stop(0);
        }
        running.values().forEach(r -> r.done().countDown());
        workers.shutdownNow();
        logger.info("fake scheduler agent stopped");
    }

    /** All job runs still in progress (for diagnostics). */
    public List<String> runningJobs()
    {
        List<String> out = new ArrayList<>();
        running.values().forEach(r -> out.add(r.initiation().get("contextInstanceId") + "/" + r.initiation().get("jobName")));
        return out;
    }
}
