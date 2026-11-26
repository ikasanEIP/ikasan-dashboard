package org.ikasan.testharness.support;

import org.ikasan.testharness.config.HarnessConfig;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Minimal client for the dashboard REST api. Authenticates with {@code POST /authenticate}
 * ({@code JwtAuthenticationController}) and sends the JWT as a Bearer token.
 */
public class DashboardApi
{
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final HarnessConfig config;
    private final String base;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Timeouts.seconds(10)).build();
    private String token;

    private final String username;
    private final String password;

    public DashboardApi(HarnessConfig config)
    {
        this(config, config.dashboard().restUsername(), config.dashboard().restPassword());
    }

    /** A client that authenticates as the given user, e.g. a persona, instead of the REST user. */
    public DashboardApi(HarnessConfig config, String username, String password)
    {
        this.config = config;
        this.base = config.dashboard().url().replaceAll("/$", "");
        this.username = username;
        this.password = password;
    }

    public synchronized String authenticate()
    {
        HttpResponse<String> res = send(HttpRequest.newBuilder(URI.create(base + "/authenticate"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(Map.of(
                "username", username, "password", password))))
            .build());
        if (res.statusCode() != 200)
        {
            throw new IllegalStateException("POST /authenticate failed: HTTP " + res.statusCode() + " " + res.body());
        }
        token = JSON.readTree(res.body()).path("token").asString();
        if (token == null || token.isBlank())
        {
            throw new IllegalStateException("POST /authenticate returned no token");
        }
        return token;
    }

    /** Send a request with the JWT and return the response whatever its status. */
    public HttpResponse<String> call(String method, String path, String body)
    {
        if (token == null)
        {
            authenticate();
        }
        return send(HttpRequest.newBuilder(URI.create(base + path))
            .timeout(Timeouts.of(Duration.ofMinutes(1)))
            .header("Authorization", "Bearer " + token)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body))
            .build());
    }

    /** Send a request with the JWT; fails on a non-2xx response. */
    /** A call without any authentication, e.g. to check an endpoint refuses it. */
    public HttpResponse<String> callWithoutToken(String method, String path, String body)
    {
        return send(HttpRequest.newBuilder(URI.create(base + path))
            .timeout(Timeouts.of(Duration.ofMinutes(1)))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body))
            .build());
    }

    public HttpResponse<String> request(String method, String path, String body)
    {
        HttpResponse<String> res = call(method, path, body);
        if (res.statusCode() >= 300)
        {
            throw new IllegalStateException(method + " " + path + " failed: HTTP " + res.statusCode() + " "
                + res.body().substring(0, Math.min(500, res.body().length())));
        }
        return res;
    }

    public JsonNode get(String path)
    {
        String body = request("GET", path, null).body();
        return body.isBlank() ? JSON.missingNode() : JSON.readTree(body);
    }

    private HttpResponse<String> send(HttpRequest request)
    {
        try
        {
            return http.send(request, HttpResponse.BodyHandlers.ofString());
        }
        catch (IOException e)
        {
            throw new IllegalStateException(request.method() + " " + request.uri() + " failed", e);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    /** Poll {@code /actuator/health} until it reports UP or the configured startup timeout passes. */
    public static void waitForDashboard(HarnessConfig config)
    {
        HttpClient http = HttpClient.newBuilder().connectTimeout(Timeouts.seconds(5)).build();
        URI uri = URI.create(config.dashboard().url().replaceAll("/$", "") + "/actuator/health");
        long deadline = System.nanoTime() + Timeouts.seconds(config.dashboard().startupTimeoutSeconds()).toNanos();
        String last = "";
        while (System.nanoTime() < deadline)
        {
            try
            {
                HttpResponse<String> res = http.send(HttpRequest.newBuilder(uri).timeout(Timeouts.seconds(10)).build(),
                    HttpResponse.BodyHandlers.ofString());
                if (res.statusCode() == 200 && res.body().matches("(?s).*\"status\"\\s*:\\s*\"UP\".*"))
                {
                    return;
                }
                last = "HTTP " + res.statusCode() + " " + res.body().substring(0, Math.min(300, res.body().length()));
            }
            catch (IOException e)
            {
                last = e.toString();
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
            if (DashboardProcess.exited())
            {
                throw new IllegalStateException("the dashboard process exited while starting; its log ends:\n"
                    + DashboardProcess.logTail(80));
            }
            try
            {
                Thread.sleep(2000);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }
        throw new IllegalStateException("Dashboard at " + uri + " not healthy after "
            + config.dashboard().startupTimeoutSeconds() + "s: " + last);
    }
}
