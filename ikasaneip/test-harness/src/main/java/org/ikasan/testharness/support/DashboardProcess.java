package org.ikasan.testharness.support;

import org.ikasan.testharness.config.ConfigLoader;
import org.ikasan.testharness.config.HarnessConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * The dashboard as a local java process ({@code dashboardProcess.enabled}), for machines without docker. It runs the
 * same jar and harness properties as the compose stack's dashboard container (docker/dashboard/*.properties), against
 * the datastore of the configuration ({@code mongo.uri} or {@code solr.url}), on the port of {@code dashboard.url}.
 *
 * <p>One dashboard runs at a time. It is stopped when the harness session closes, and in any case when the JVM
 * exits.</p>
 */
public final class DashboardProcess
{
    private static final Logger logger = LoggerFactory.getLogger(DashboardProcess.class);

    private static Process process;
    private static Path log;

    static
    {
        Runtime.getRuntime().addShutdownHook(new Thread(DashboardProcess::stop, "harness-dashboard-stop"));
    }

    private DashboardProcess() {}

    public static boolean enabled(HarnessConfig config)
    {
        return config.dashboardProcess() != null && config.dashboardProcess().enabled();
    }

    /** Start the dashboard for the configuration, stopping the one already started by the harness first. */
    public static synchronized void start(HarnessConfig config)
    {
        stop();
        int port = port(config);
        if (listening(port))
        {
            throw new IllegalStateException("cannot start the dashboard: port " + port + " is in use (" + config.dashboard().url()
                + "). Is a dashboard from an earlier run still running?");
        }
        List<String> command = command(config, port);
        Map<String, String> environment = environment(config);
        HarnessConfig.DashboardProcess settings = config.dashboardProcess();
        log = ConfigLoader.resolve(settings.log().replaceFirst("(\\.log)?$", "-" + config.backend() + ".log"));
        Path workDir = ConfigLoader.resolve("target/dashboard-" + config.backend());
        try
        {
            Files.createDirectories(log.getParent());
            Files.createDirectories(workDir);
            ProcessBuilder builder = new ProcessBuilder(command).directory(workDir.toFile())
                .redirectErrorStream(true).redirectOutput(log.toFile());
            builder.environment().putAll(environment);
            logger.info("starting the dashboard ({} backend) on port {}, log {}: {}", config.backend(), port, log,
                String.join(" ", command));
            process = builder.start();
        }
        catch (IOException e)
        {
            throw new UncheckedIOException("cannot start the dashboard: " + String.join(" ", command), e);
        }
    }

    /** Start the dashboard unless one already answers on the configured port (e.g. when reusing seeded data). */
    public static synchronized void ensureRunning(HarnessConfig config)
    {
        if (process != null && process.isAlive() || listening(port(config)))
        {
            return;
        }
        start(config);
    }

    /** Stop the dashboard started by the harness, if any. */
    public static synchronized void stop()
    {
        if (process == null)
        {
            return;
        }
        Process stopping = process;
        process = null;
        if (!stopping.isAlive())
        {
            return;
        }
        logger.info("stopping the dashboard (pid {})", stopping.pid());
        stopping.descendants().forEach(ProcessHandle::destroy);
        stopping.destroy();
        try
        {
            if (!stopping.waitFor(Timeouts.seconds(30).toMillis(), TimeUnit.MILLISECONDS))
            {
                stopping.descendants().forEach(ProcessHandle::destroyForcibly);
                stopping.destroyForcibly().waitFor(10, TimeUnit.SECONDS);
            }
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            stopping.destroyForcibly();
        }
    }

    /** Whether the dashboard process exited (e.g. it failed to start); its exit code is in the log. */
    public static synchronized boolean exited()
    {
        return process != null && !process.isAlive();
    }

    /** The last lines of the dashboard's log. */
    public static synchronized String logTail(int lines)
    {
        if (log == null || !Files.exists(log))
        {
            return "(no dashboard log)";
        }
        try
        {
            List<String> all = Files.readAllLines(log);
            return String.join("\n", all.subList(Math.max(0, all.size() - lines), all.size()));
        }
        catch (IOException | UncheckedIOException e)
        {
            return "(cannot read " + log + ": " + e.getMessage() + ")";
        }
    }

    static List<String> command(HarnessConfig config, int port)
    {
        HarnessConfig.DashboardProcess settings = config.dashboardProcess();
        Path jar = ConfigLoader.resolve(settings.dashboardJar());
        if (!Files.isRegularFile(jar))
        {
            throw new IllegalStateException("dashboard jar " + jar + " not found: build it first "
                + "(mvn -Pall package -pl visualisation/dashboard -DskipTests) or set dashboardProcess.dashboardJar");
        }
        Path harnessConfig = ConfigLoader.resolve("docker/dashboard");
        List<String> command = new ArrayList<>();
        command.add(java(settings));
        command.addAll(settings.jvmArgs());
        command.add("-Dmodule.name=ikasan-dashboard");
        command.add("-jar");
        command.add(jar.toString());
        // the dashboard container's properties, then what differs outside docker (command line arguments win)
        command.add("--spring.config.additional-location="
            + "file:" + harnessConfig.resolve("application-harness.properties") + ","
            + "file:" + harnessConfig.resolve("application-harness-" + config.backend() + ".properties"));
        command.add("--server.port=" + port);
        command.add("--integrated.systems.image.path=" + harnessConfig.resolve("images"));
        if (config.backend() == HarnessConfig.Backend.mongo)
        {
            command.add("--mongodb.database=" + config.mongo().database());
        }
        else
        {
            command.add("--solr.username=" + config.solr().username());
            command.add("--solr.password=" + config.solr().password());
        }
        return command;
    }

    /** The datastore, as the compose stack passes it to the dashboard container (application.properties reads these). */
    static Map<String, String> environment(HarnessConfig config)
    {
        return config.backend() == HarnessConfig.Backend.mongo
            ? Map.of("MONGODB_URI", config.mongo().uri(),
                     "MONGODB_ATLAS_SEARCH_ENABLED", String.valueOf(config.mongo().atlasSearch()))
            : Map.of("SOLR_URL", config.solr().url());
    }

    private static String java(HarnessConfig.DashboardProcess settings)
    {
        if (settings.java() != null && !settings.java().isBlank())
        {
            return settings.java();
        }
        return Path.of(System.getProperty("java.home"), "bin", "java").toString();
    }

    static int port(HarnessConfig config)
    {
        URI url = URI.create(config.dashboard().url());
        return url.getPort() > 0 ? url.getPort() : "https".equals(url.getScheme()) ? 443 : 80;
    }

    private static boolean listening(int port)
    {
        try (Socket socket = new Socket())
        {
            socket.connect(new InetSocketAddress("localhost", port), 500);
            return true;
        }
        catch (IOException e)
        {
            return false;
        }
    }
}
