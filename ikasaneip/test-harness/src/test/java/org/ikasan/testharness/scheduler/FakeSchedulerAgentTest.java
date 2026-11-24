package org.ikasan.testharness.scheduler;

import org.junit.jupiter.api.Test;

import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class FakeSchedulerAgentTest
{
    @Test
    void outcomeComesFromTheCommandLine()
    {
        assertThat(FakeSchedulerAgent.outcomeOf("echo alpha")).isEqualTo(new FakeSchedulerAgent.Outcome.Succeed("alpha"));
        assertThat(FakeSchedulerAgent.outcomeOf("echo boom; exit 3")).isEqualTo(new FakeSchedulerAgent.Outcome.Fail(3, "boom"));
        assertThat(FakeSchedulerAgent.outcomeOf("exit 0")).isEqualTo(new FakeSchedulerAgent.Outcome.Succeed(""));
        assertThat(FakeSchedulerAgent.outcomeOf("")).isEqualTo(new FakeSchedulerAgent.Outcome.Succeed(""));
        assertThat(FakeSchedulerAgent.sleepSeconds("sleep 30")).isEqualTo(30);
        assertThat(FakeSchedulerAgent.sleepSeconds("echo x")).isZero();
    }

    @Test
    void requestsNeedTheDashboardsCredentialsAndAreRecorded() throws Exception
    {
        int port;
        try (ServerSocket socket = new ServerSocket(0))
        {
            port = socket.getLocalPort();
        }
        try (FakeSchedulerAgent agent = new FakeSchedulerAgent("scheduler-agent", port, "admin", "admin", null).start())
        {
            HttpClient http = HttpClient.newHttpClient();
            URI uri = URI.create("http://localhost:" + port + "/scheduler-agent/rest/jobProvision");
            HttpResponse<String> anonymous = http.send(HttpRequest.newBuilder(uri)
                .PUT(HttpRequest.BodyPublishers.ofString("[]")).build(), HttpResponse.BodyHandlers.ofString());
            HttpResponse<String> authorised = http.send(HttpRequest.newBuilder(uri)
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString("admin:admin".getBytes(StandardCharsets.UTF_8)))
                .PUT(HttpRequest.BodyPublishers.ofString("[]")).build(), HttpResponse.BodyHandlers.ofString());

            assertThat(anonymous.statusCode()).isEqualTo(401);
            assertThat(authorised.statusCode()).isEqualTo(200);
            assertThat(agent.calls("PUT", "/rest/jobProvision")).hasSize(2);
        }
    }
}
