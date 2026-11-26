package org.ikasan.testharness.support;

import org.ikasan.testharness.config.ConfigLoader;
import org.ikasan.testharness.config.HarnessConfig;
import org.ikasan.testharness.seed.Seeder;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Users with narrower access than admin ({@code data/personas.yaml}), written straight into the store as the
 * dashboard's security documents: a role holding the persona's policies (and modules / job plans), a principal with
 * that role and a user with that principal. The documents have the same fields in Mongo and Solr
 * ({@code securityRole}, {@code securityPrincipal}, {@code securityUser}); the policies are the baseline ones the
 * dashboard creates on start-up, referenced by id ({@code <name>-securityPolicy}).
 */
public final class Personas
{
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final YAMLMapper YAML = YAMLMapper.builder().build();
    public static final String FILE = "data/personas.yaml";

    /** A persona: a user of this name with a role of the same name. */
    public record Persona(String name, String description, List<String> policies, List<String> modules, List<String> jobPlans) {}

    public record Definitions(String password, Map<String, Persona> personas) {}

    private Personas() {}

    @SuppressWarnings("unchecked")
    public static Definitions load()
    {
        Path file = ConfigLoader.resolve(FILE);
        try
        {
            Map<String, Object> raw = YAML.readValue(Files.readString(file), Map.class);
            Map<String, Persona> personas = new LinkedHashMap<>();
            ((Map<String, Map<String, Object>>) raw.get("personas")).forEach((name, p) -> personas.put(name, new Persona(name,
                String.valueOf(p.get("description")),
                (List<String>) p.getOrDefault("policies", List.of()),
                (List<String>) p.getOrDefault("modules", List.of()),
                (List<String>) p.getOrDefault("jobPlans", List.of()))));
            return new Definitions(String.valueOf(raw.get("password")), personas);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException("Cannot read " + file, e);
        }
    }

    /** Write every persona's role, principal and user (insert or replace). */
    public static void seed(HarnessConfig config, Definitions definitions)
    {
        List<Map<String, Object>> documents = new ArrayList<>();
        definitions.personas().values().forEach(p -> documents.addAll(documents(p, definitions.password())));
        try (Seeder seeder = Seeder.create(config))
        {
            seeder.load(documents);
        }
    }

    /** The role, principal and user documents of a persona. */
    static List<Map<String, Object>> documents(Persona p, String password)
    {
        long now = System.currentTimeMillis();
        String created = Instant.ofEpochMilli(now).toString();
        String roleId = p.name() + "-securityRole";
        String principalId = p.name() + "-securityPrincipal";

        Map<String, Object> role = base(roleId, "securityRole", p.name(), now);
        role.put("payload", json(ordered("createdDateTime", created, "description", p.description(), "id", roleId,
            "name", p.name(), "updatedDateTime", created)));
        role.put("policiesRelatedEntity", p.policies().stream().map(policy -> policy + "-securityPolicy").toList());
        role.put("roleModuleRelatedEntity", p.modules());
        role.put("roleJobPlanRelatedEntity", p.jobPlans());

        Map<String, Object> principal = base(principalId, "securityPrincipal", p.name(), now);
        principal.put("ikasanPrincipalType", "user");
        principal.put("description", p.description());
        principal.put("payload", json(ordered("createdDateTime", created, "description", p.description(), "id", principalId,
            "name", p.name(), "type", "user", "updatedDateTime", created)));
        principal.put("rolesRelatedEntity", List.of(roleId));

        Map<String, Object> user = base(p.name() + "-securityUser", "securityUser", p.name(), now);
        user.put("firstName", p.name());
        user.put("surname", "Persona");
        user.put("email", p.name() + "@harness.invalid");
        user.put("payload", json(ordered("accountNonExpired", true, "accountNonLocked", true, "credentialsNonExpired", true,
            "enabled", true, "email", p.name() + "@harness.invalid", "firstName", p.name(), "name", p.name(),
            // the delegating password encoder takes {noop}: the persona logs in with the plain password
            "password", "{noop}" + password, "requiresPasswordChange", false, "surname", "Persona", "username", p.name())));
        user.put("principalRelatedEntity", List.of(principalId));

        return List.of(role, principal, user);
    }

    private static Map<String, Object> base(String id, String type, String name, long now)
    {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("id", id);
        d.put("type", type);
        d.put("name", name);
        d.put("timestamp", now);
        d.put("modifiedTimestamp", now);
        d.put("expiry", -1L);
        return d;
    }

    private static Map<String, Object> ordered(Object... keyValues)
    {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2)
        {
            m.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        }
        return m;
    }

    private static String json(Object value)
    {
        return JSON.writeValueAsString(value);
    }
}
