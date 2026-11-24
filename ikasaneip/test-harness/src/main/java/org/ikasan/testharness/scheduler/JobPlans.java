package org.ikasan.testharness.scheduler;

import org.ikasan.testharness.config.ConfigLoader;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Job plan fixtures: data/jobplans/&lt;PLAN&gt;/ holds a plan in the dashboard's export layout
 * ({@code context/<plan>.json}, {@code jobs/{internal,quartz,file,global}/*.json}).
 * {@link #zip(String)} packs one into the zip the dashboard's "Upload Job Plan" expects
 * ({@code org.ikasan.job.orchestration.util.ContextImportZipUtils}: the plan's root directory must be the first entry).
 */
public final class JobPlans
{
    public static final String FIXTURE_DIR = "data/jobplans";

    private JobPlans() {}

    public static Path fixture(String plan)
    {
        Path dir = ConfigLoader.resolve(FIXTURE_DIR).resolve(plan);
        if (!Files.isDirectory(dir))
        {
            throw new IllegalArgumentException("No job plan fixture " + dir);
        }
        return dir;
    }

    /** Zip the fixture into target/jobplans/&lt;plan&gt;.zip and return its path. */
    public static Path zip(String plan)
    {
        Path source = fixture(plan);
        Path zip = ConfigLoader.resolve("target/jobplans").resolve(plan + ".zip");
        try
        {
            Files.createDirectories(zip.getParent());
            List<Path> entries;
            try (Stream<Path> walk = Files.walk(source))
            {
                // directories before their files, so the plan's root directory is the first entry
                entries = walk.sorted().toList();
            }
            try (OutputStream out = Files.newOutputStream(zip); ZipOutputStream zos = new ZipOutputStream(out))
            {
                for (Path p : entries)
                {
                    String name = plan + "/" + source.relativize(p).toString().replace('\\', '/');
                    if (Files.isDirectory(p))
                    {
                        zos.putNextEntry(new ZipEntry(name.endsWith("/") ? name : name + "/"));
                    }
                    else
                    {
                        zos.putNextEntry(new ZipEntry(name));
                        Files.copy(p, zos);
                    }
                    zos.closeEntry();
                }
            }
            return zip;
        }
        catch (IOException e)
        {
            throw new UncheckedIOException("Cannot zip job plan fixture " + plan, e);
        }
    }

    /** The agent names of the jobs in an exported job plan bundle (each {@code jobs/**.json} entry's agentName). */
    public static java.util.Set<String> jobAgentNames(Path bundle) throws java.io.IOException
    {
        java.util.Set<String> agents = new java.util.TreeSet<>();
        tools.jackson.databind.json.JsonMapper json = tools.jackson.databind.json.JsonMapper.builder().build();
        try (java.io.InputStream in = java.nio.file.Files.newInputStream(bundle);
             java.util.zip.ZipInputStream zip = new java.util.zip.ZipInputStream(in))
        {
            for (java.util.zip.ZipEntry e; (e = zip.getNextEntry()) != null; )
            {
                if (e.getName().contains("/jobs/") && e.getName().endsWith(".json"))
                {
                    agents.add(json.readTree(zip.readAllBytes()).path("agentName").asString());
                }
            }
        }
        return agents;
    }
}
