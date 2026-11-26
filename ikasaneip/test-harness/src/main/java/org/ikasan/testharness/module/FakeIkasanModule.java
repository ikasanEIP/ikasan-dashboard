package org.ikasan.testharness.module;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.ikasan.testharness.support.DashboardApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.function.Predicate;

/**
 * A stand-in for Ikasan integration modules, running inside the harness JVM. Each module is served under its own
 * context path, {@code http://<host>:<port>/<moduleName>}, so one server can play several modules.
 * <p>
 * The dashboard calls a module to act on events found by search:
 * <ul>
 *     <li>{@code PUT <module url>/rest/resubmission} - resubmit or ignore an exclusion; the module's url comes from
 *     its module metadata ({@link #register}), the credentials are the dashboard's {@code rest.module.*}</li>
 *     <li>{@code PUT <target url>/rest/replay} - replay an event; the url and credentials are typed by the user</li>
 * </ul>
 * It also answers the dashboard's question for a flow's state ({@code GET <url>/rest/moduleControl/<module>/<flow>})
 * with the state a test set ({@link #flowState}).
 * <p>
 * Every request is recorded ({@link #calls}) and answered 200, unless it is not authorised (401) or a test has
 * scripted a failure for it ({@link #failWith}).
 */
public class FakeIkasanModule implements AutoCloseable
{
    private static final Logger logger = LoggerFactory.getLogger(FakeIkasanModule.class);
    private static final JsonMapper JSON = JsonMapper.builder().build();

    /** The port used unless {@code -Dharness.fakeModulePort} says otherwise (the fake scheduler agent uses 18090). */
    public static final int DEFAULT_PORT = 18091;

    /** A request the dashboard made to a module. */
    public record Call(String method, String path, String authorization, String body)
    {
        @SuppressWarnings("unchecked")
        public Map<String, Object> json()
        {
            return body == null || body.isBlank() ? Map.of() : JSON.readValue(body, Map.class);
        }

        /** The {@code user:password} of the request's basic authentication, or null. */
        public String basicCredentials()
        {
            if (authorization == null || !authorization.startsWith("Basic "))
            {
                return null;
            }
            return new String(Base64.getDecoder().decode(authorization.substring("Basic ".length())), StandardCharsets.UTF_8);
        }
    }

    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final List<Call> calls = new CopyOnWriteArrayList<>();
    private final Map<String, Integer> failures = new ConcurrentHashMap<>();
    private final Map<String, String> flowStates = new ConcurrentHashMap<>();
    private static final java.util.regex.Pattern FLOW_STATE = java.util.regex.Pattern.compile("^/[^/]+/rest/moduleControl/([^/]+)/([^/]+)$");
    private HttpServer server;

    /**
     * @param host     the host the dashboard reaches this JVM on (host.docker.internal from the dashboard container)
     * @param port     port to listen on, on all interfaces
     * @param username the basic auth user every request must carry
     * @param password the basic auth password
     */
    public FakeIkasanModule(String host, int port, String username, String password)
    {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    /** The port from {@code -Dharness.fakeModulePort}, else {@link #DEFAULT_PORT}. */
    public static int configuredPort()
    {
        return Integer.getInteger("harness.fakeModulePort", DEFAULT_PORT);
    }

    public FakeIkasanModule start()
    {
        try
        {
            server = HttpServer.create(new InetSocketAddress(port), 0);
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Cannot start the fake Ikasan module on port " + port, e);
        }
        server.createContext("/", this::handle);
        server.setExecutor(Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "fake-module-http");
            t.setDaemon(true);
            return t;
        }));
        server.start();
        logger.info("fake Ikasan module listening on port {}", port);
        return this;
    }

    /** The url of a module served by this fake, as the dashboard reaches it. */
    public String url(String moduleName)
    {
        return "http://" + host + ":" + port + "/" + moduleName;
    }

    /**
     * Register a module with the dashboard ({@code PUT /rest/module/metadata}, type INTEGRATION_MODULE) with this
     * fake's url for it, so the dashboard's resubmit/ignore of the module's exclusions come here.
     */
    public void register(DashboardApi dashboard, String moduleName)
    {
        register(dashboard, moduleName, "Test harness fake Ikasan module", List.of());
    }

    /**
     * Register a module with a description and flows. The dashboard asks the module for the state of each flow
     * ({@code GET <url>/rest/moduleControl/<module>/<flow>}) the first time it needs it: set it with {@link #flowState}.
     */
    public void register(DashboardApi dashboard, String moduleName, String description, List<String> flows)
    {
        URI uri = URI.create(url(moduleName));
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("name", moduleName);
        metadata.put("type", "INTEGRATION_MODULE");
        metadata.put("url", uri.toString());
        metadata.put("protocol", uri.getScheme());
        metadata.put("host", uri.getHost());
        metadata.put("port", uri.getPort());
        metadata.put("context", moduleName);
        metadata.put("description", description);
        metadata.put("flows", flows.stream().map(flow -> Map.of("name", flow)).toList());
        dashboard.request("PUT", "/rest/module/metadata", JSON.writeValueAsString(metadata));
    }

    // ------------------------------------------------------------------------------------------------- test API

    /** Answer requests whose path ends with the suffix (e.g. {@code /rest/resubmission}) with this status. */
    public void failWith(String pathSuffix, int status)
    {
        failures.put(pathSuffix, status);
    }

    /**
     * The state a flow reports when the dashboard asks the module (running, stopped, stoppedInError, recovering,
     * paused, ...).
     */
    public void flowState(String moduleName, String flowName, String state)
    {
        flowStates.put(moduleName + "/" + flowName, state);
    }

    /** Forget recorded calls and scripted failures. */
    public void reset()
    {
        calls.clear();
        failures.clear();
    }

    /** Every request received, in order. */
    public List<Call> calls()
    {
        return List.copyOf(calls);
    }

    /** Requests with the method whose path ends with the suffix, e.g. {@code calls("PUT", "/rest/replay")}. */
    public List<Call> calls(String method, String pathSuffix)
    {
        return calls.stream().filter(c -> c.method().equals(method) && c.path().endsWith(pathSuffix)).toList();
    }

    /** Wait until {@code count} requests match, and return them. */
    public List<Call> awaitCalls(Predicate<Call> matching, int count, Duration timeout)
    {
        long deadline = System.nanoTime() + timeout.toNanos();
        List<Call> matched;
        do
        {
            matched = calls.stream().filter(matching).toList();
            if (matched.size() >= count)
            {
                return matched;
            }
            sleep(100);
        }
        while (System.nanoTime() < deadline);
        throw new AssertionError("The fake module received " + matched.size() + " of " + count + " expected requests within "
            + timeout + "; received: " + calls.stream().map(c -> c.method() + " " + c.path()).toList());
    }

    // ------------------------------------------------------------------------------------------------- http

    private void handle(HttpExchange exchange) throws IOException
    {
        try (exchange)
        {
            String path = exchange.getRequestURI().getPath();
            String authorization = exchange.getRequestHeaders().getFirst("Authorization");
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            calls.add(new Call(exchange.getRequestMethod(), path, authorization, body));
            logger.debug("module <- {} {}", exchange.getRequestMethod(), path);

            String expected = "Basic " + Base64.getEncoder()
                .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
            if (!expected.equals(authorization))
            {
                respond(exchange, 401, "{\"errorMessage\":\"Unauthorized\"}");
                return;
            }
            java.util.regex.Matcher flowState = FLOW_STATE.matcher(path);
            if (exchange.getRequestMethod().equals("GET") && flowState.matches())
            {
                String state = flowStates.get(flowState.group(1) + "/" + flowState.group(2));
                if (state == null)
                {
                    respond(exchange, 404, "");
                }
                else
                {
                    respond(exchange, 200, JSON.writeValueAsString(Map.of("name", flowState.group(2), "state", state)));
                }
                return;
            }
            Integer failure = failures.entrySet().stream().filter(e -> path.endsWith(e.getKey()))
                .map(Map.Entry::getValue).findFirst().orElse(null);
            if (failure != null)
            {
                respond(exchange, failure, "{\"errorMessage\":\"harness scripted failure " + failure + "\"}");
                return;
            }
            respond(exchange, 200, "");
        }
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

    private static void sleep(long millis)
    {
        try
        {
            Thread.sleep(millis);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void close()
    {
        if (server != null)
        {
            server.stop(0);
            server = null;
        }
    }
}
