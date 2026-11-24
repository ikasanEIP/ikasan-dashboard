package org.ikasan.testharness.support;

import org.ikasan.testharness.config.ConfigLoader;
import org.ikasan.testharness.config.HarnessConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Thin wrapper around {@code docker compose} for the harness stack (docker/compose.yml).
 * <p>
 * Profiles: {@code <backend>} starts the datastore, {@code <backend>-dashboard} starts the dashboard for that backend.
 * Only one backend runs at a time; both dashboards publish the same host port.
 */
public class Compose
{
    private static final Logger logger = LoggerFactory.getLogger(Compose.class);

    private final HarnessConfig config;

    public Compose(HarnessConfig config)
    {
        this.config = config;
    }

    private static String port(String url, String fallback)
    {
        int port = URI.create(url.replaceFirst("^mongodb(\\+srv)?:", "http:")).getPort();
        return port > 0 ? String.valueOf(port) : fallback;
    }

    private Map<String, String> environment()
    {
        String agentJar = config.scheduler().realAgentJar() == null ? ""
            : config.scheduler().realAgentJar().replaceFirst("^~", java.util.regex.Matcher.quoteReplacement(System.getProperty("user.home")));
        return Map.of(
            "HARNESS_AGENT_JAR", agentJar.isEmpty() ? "" : ConfigLoader.resolve(agentJar).toString(),
            "HARNESS_DASHBOARD_PORT", port(config.dashboard().url(), "9190"),
            "HARNESS_DASHBOARD_JAR", ConfigLoader.resolve(config.compose().dashboardJar()).toString(),
            "HARNESS_MONGO_PORT", port(config.mongo().uri(), "27117"),
            "HARNESS_MONGO_ATLASSEARCH", String.valueOf(config.mongo().atlasSearch()),
            "HARNESS_SOLR_PORT", port(config.solr().url(), "8993"));
    }

    private String run(List<String> args, boolean quiet)
    {
        List<String> cmd = new ArrayList<>(List.of("docker", "compose",
            "-f", ConfigLoader.resolve(config.compose().file()).toString(), "-p", config.compose().project()));
        cmd.addAll(args);
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.environment().putAll(environment());
        try
        {
            Process p = pb.start();
            CompletableFuture<String> out = CompletableFuture.supplyAsync(() -> read(p.getInputStream()));
            CompletableFuture<String> err = CompletableFuture.supplyAsync(() -> read(p.getErrorStream()));
            int code = p.waitFor();
            if (!quiet && !err.join().isBlank())
            {
                logger.info("{}", err.join().strip());
            }
            if (code != 0)
            {
                throw new IllegalStateException(String.join(" ", cmd) + " exited " + code + "\n" + err.join() + "\n" + out.join());
            }
            return out.join();
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Cannot run " + String.join(" ", cmd) + " - is docker installed?", e);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private static String read(java.io.InputStream in)
    {
        try (in)
        {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        catch (IOException e)
        {
            return "";
        }
    }

    private static List<String> profiles(String... names)
    {
        List<String> out = new ArrayList<>();
        for (String n : names)
        {
            out.add("--profile");
            out.add(n);
        }
        return out;
    }

    private static List<String> concat(List<String> a, String... b)
    {
        List<String> out = new ArrayList<>(a);
        out.addAll(List.of(b));
        return out;
    }

    /**
     * Start (or keep running) the datastore and wait until healthy. Services of the other backend are removed
     * first, because both dashboards share a host port.
     */
    public void upStore()
    {
        String b = config.backend().name();
        String other = config.backend().other().name();
        run(concat(profiles(other, other + "-dashboard"), "rm", "-sf", other, "dashboard-" + other), true);
        run(concat(profiles(b), "up", "-d", "--quiet-pull", "--wait", b), false);
    }

    /** (Re)start the dashboard so it boots against the freshly seeded store. */
    public void restartDashboard()
    {
        String b = config.backend().name();
        String svc = "dashboard-" + b;
        run(concat(profiles(b, b + "-dashboard"), "rm", "-sf", svc), true);
        run(concat(profiles(b, b + "-dashboard"), "up", "-d", "--quiet-pull", "--no-deps", svc), false);
    }

    /**
     * (Re)start the real scheduler agent and wait until it is healthy. Restarted every run, after the dashboard, so
     * it starts empty and registers with the freshly seeded dashboard.
     */
    public void upAgent()
    {
        String b = config.backend().name();
        List<String> withAgent = profiles(b, b + "-dashboard", "agent");
        run(concat(withAgent, "rm", "-sf", "scheduler-agent"), true);
        run(concat(withAgent, "up", "-d", "--quiet-pull", "--no-deps", "--wait", "scheduler-agent"), false);
    }

    /**
     * Remove the real scheduler agent container, if one is left from an earlier run. Without this a fake agent run
     * is hijacked: the real agent keeps re-registering itself under the same name, with its own url.
     */
    public void removeAgent()
    {
        String b = config.backend().name();
        run(concat(profiles(b, b + "-dashboard", "agent"), "rm", "-sf", "scheduler-agent"), true);
    }

    /** Stop and remove the whole stack including volumes. */
    public void down()
    {
        run(concat(profiles("mongo", "solr", "mongo-dashboard", "solr-dashboard", "agent"), "down", "-v"), false);
    }

    public String exec(String service, List<String> command)
    {
        List<String> args = new ArrayList<>(profiles(config.backend().name()));
        args.addAll(List.of("exec", "-T", service));
        args.addAll(command);
        return run(args, false);
    }

    public String logs(String service, int tail)
    {
        String b = config.backend().name();
        return run(concat(profiles(b, b + "-dashboard", "agent"), "logs", "--no-color", "--tail", String.valueOf(tail), service), true);
    }
}
