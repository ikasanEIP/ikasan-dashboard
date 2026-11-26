# Ikasan Dashboard Test Harness

End-to-end tests for the Ikasan Dashboard, written with **Playwright for Java** and **JUnit 5**. They run against a real dashboard and a real datastore (MongoDB or Solr). The datastore is baselined with a known dataset before every run.

- **Repeatable:** each run wipes the datastore, loads the baseline dataset, then (re)starts the dashboard against it.
- **Configurable:** YAML config, layered by environment and overridable with `-Dharness.*` or `HARNESS_*`.
- **Externalised expectations:** test inputs and expected results live in `expectations/**/*.yaml`, not in code.
- **Both backends:** one backend-neutral dataset is loaded into Mongo or Solr.

## Prerequisites

- Docker. The harness runs `docker compose` itself.
- Java 25 and Maven.
- The dashboard jar. `-am` builds it, or build it once yourself:
  ```
  mvn -Pall package -pl visualisation/dashboard -DskipTests
  ```

Playwright downloads its browser on first use.

For the real scheduler agent (optional, see [Scheduler](#scheduler)) you also need the `scheduler-agent` jar in your Maven repository (`org.ikasan:scheduler-agent:5.0.0`) and the `eclipse-temurin:26-jre` image, which compose pulls on first use.

## Running

Run from `ikasaneip`. The harness is not part of the default build.

```bash
mvn -Pall,e2e verify -pl test-harness                                    # Mongo (default)
mvn -Pall,e2e verify -pl test-harness -Dharness.env=compose-solr         # Solr
mvn -Pall,e2e verify -pl test-harness -Dharness.env=all                  # Mongo, then Solr, in one run and one report
mvn -Pall,e2e verify -pl test-harness -Dharness.include=smoke            # only tests/scenarios tagged smoke
mvn -Pall,e2e verify -pl test-harness -Dit.test=SearchIT                 # one test class
mvn -Pall,e2e verify -pl test-harness -Dharness.run.headless=false -Dharness.run.slowMoMillis=250   # watch it
mvn -Pall,e2e verify -pl test-harness -Dharness.include=scheduler        # the scheduler tests (fake agent)
mvn -Pall,e2e verify -pl test-harness -Dharness.include=scheduler -Dharness.scheduler.agent=real   # on the real agent
mvn -Pall,e2e verify -pl test-harness -Dharness.include=slow             # slow tests (CronStartIT, ~3 minutes)
```

- Every Maven `-P` flag disables the `activeByDefault` profile `all`, so always pass `all` explicitly.
- `mvn -Pall,e2e test -pl test-harness` runs only the harness's own unit tests (config, tokens, data files). These need no Docker.
- Tests can also be run from the IDE. Set the working directory to `test-harness` and choose the environment with the `HARNESS_ENV` env var.
- A full run of the scheduler tests takes about ten minutes per backend, most of it the baseline and the dashboard start.
- **Both backends in one run** (`-Dharness.env=all`, profile `all-backends` in `pom.xml`):
  - Failsafe runs `it/MongoBackendSuite` and then `it/SolrBackendSuite`, in one JVM.
  - Each suite is a JUnit Platform suite that runs every `*IT` with its own environment, so each gets its own baseline, stack, login and scheduler agent.
  - One Reporting Labs report covers both: each test appears once per backend, with a `backend` chip and filter.
  - It takes about as long as the two single-backend runs together.
  - Select tests with tags (`-Dharness.include=smoke`). `-Dit.test` names test classes directly, which bypasses the suites.
  - `HARNESS_SKIP_BASELINE` is refused, because the stack only holds the last backend.

### While writing tests

Run these from `test-harness`:

```bash
mvn -q exec:java -Dexec.args="seed"          # stack up, baseline loaded, dashboard up
HARNESS_SKIP_BASELINE=true <run tests>       # iterate without re-seeding
mvn -q exec:java -Dexec.args="stack down"    # remove the stack (and its volumes)
```

- `HARNESS_SKIP_BASELINE=true` reuses the running containers as they are. After rebuilding the dashboard jar, do one full run (or `seed`) first: the running container still has the old jar open and misbehaves (for example the login page hangs).
- Data a test creates is still there on the next skip-baseline run: write tests that tolerate it (unique names, `ensureUploaded`, rows that may be present but are not exact).

## How a run works

`@HarnessTest` (the `HarnessExtension`) sets up the following once per run (`HarnessSession`):

1. **The baseline.** `support/Baseline`:
   1. Starts the datastore with `docker compose --profile <backend>`.
   2. Wipes it and loads the dataset, then checks the document count per type.
      For Mongo it also applies the repo's index definitions (`persistence/mongo/src/main/resources/mongo/indexes/apply-indexes.js`) and waits for Atlas Search to be queryable.
   3. (Re)starts the dashboard container so its in-memory caches load from the fresh store.
      The wipe removes the `dashboardPlatformSetup` marker, so the dashboard re-creates the baseline security data on start-up. That gives the user `admin` / `admin`.
   4. Waits for `/actuator/health`.
2. **One browser and one UI login.** Each test then gets a fresh browser context that starts already logged in, unless it is marked `@Unauthenticated`.
3. **Who is logged in:** admin, unless the test or class says `@Unauthenticated` or `@LoginAs("<persona>")`.
4. **Parameters injected into test methods:** `Page`, `BrowserContext`, `HarnessConfig`, `Clock`, `DashboardApi`, `HarnessSession`, `SchedulerApi`, `FakeSchedulerAgent`, and any page object with a `(Page)` constructor.
5. **Report and failures:** each run writes an HTML report, `target/reporting-labs/index.html` (see [Reports](#reports)), with every test's Playwright steps, and a failed test's screenshot and trace.
   With the report switched off, or when running from an IDE, a failed test instead leaves a Playwright trace and a screenshot of every open tab in `target/playwright/`.
   Open a trace with `mvn exec:java -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="show-trace <zip>"`, or drop it onto https://trace.playwright.dev.
6. **Mutating tests:** classes tagged `@Tag("mutating")` run after all the others (`MutatingLastClassOrderer`).
7. **The scheduler agent:** the fake agent is started and registered, or the real agent container started and awaited (see [Scheduler](#scheduler)).
8. **Conditional tests:** tests tagged `slow` run only when included by name (`-Dharness.include=slow`), and tests tagged `real-agent` only with the real agent.

The stack uses host ports 27117 (Mongo), 8993 (Solr), 9190 (dashboard), 18080 (real scheduler agent) and 18090 (the fake scheduler agent, in the harness JVM) and 18091 (the fake Ikasan module used by the exclusion and replay tests, also in the harness JVM; `-Dharness.fakeModulePort`). They are deliberately not the defaults, so a local development Mongo or Solr is never touched.

## Reports

End-to-end runs write a [Reporting Labs](https://reportinglabs.dev) report: **`target/reporting-labs/index.html`**, one self-contained file to open in a browser or archive.
- It holds every test (display name, tags, outcome, duration, error), each test's Playwright steps (navigate, click, fill, expect; typed passwords are masked), and, for a failed test, a full-page screenshot and a Playwright trace.
- The report is written locally only: nothing is uploaded, and no account or API key is needed.
- Trends: the run history is kept in `target/reporting-labs-history.json`, so `mvn clean` starts it afresh. In CI, archive `target/reporting-labs/`, and keep the history file between builds if you want the trend chart.
- **Settings:** `src/test/resources/reporting-labs.properties` sets screenshots and traces on failure, steps always, no video and no PDF. `pom.xml` (failsafe) sets the title, environment and output folder. Any `reporting-labs.*` setting can be overridden with `-D`, for example `-Dreporting-labs.playwright.trace=always`.
- **Off:** `-Dharness.report.reportingLabs=false` (or `report.reportingLabs: false`) writes no report, and the harness goes back to its own failure capture in `target/playwright/`.
- **How it is wired:**
  - The Reporting Labs JUnit extension is registered for failsafe only, through `junit.jupiter.extensions.autodetection.enabled`, so the harness's own unit tests are not reported.
  - `HarnessExtension` attaches each test's browser context with `RlPlaywright.attach`, because tests get `Page` and page objects as parameters, which Reporting Labs' field scan cannot see. That also covers tabs a test opens, such as a job plan instance.
  - A browser context can record only one trace, so the harness starts its own trace only when Reporting Labs is off.
  - An IDE run that doesn't set the autodetection parameter keeps the `target/playwright/` capture.

## Configuration

Settings are layered; later layers win (`config/ConfigLoader`):

1. `config/harness.default.yaml`. Every setting is documented here.
2. `config/environments/<env>.yaml`, where the env comes from `-Dharness.env` or `HARNESS_ENV`: `compose-mongo` (default), `compose-solr` or `external`.
3. `config/local.yaml`. Optional and git-ignored.
4. `-Dharness.<section>.<key>=value`, for example `-Dharness.dashboard.url=http://host:9090`.
5. `HARNESS_<SECTION>_<KEY>` environment variables, for example `HARNESS_RUN_INCLUDE=smoke,search` or `HARNESS_MONGO_URI=...`.

### Machines without Docker (`local-solr`, `local-mongo`)

The `local-solr` and `local-mongo` environments create no containers. The datastore must already be running: a Solr server with an `ikasan` core built from the Ikasan configset, or a MongoDB Atlas deployment. The harness then:
- seeds the datastore;
- applies the Mongo indexes and Atlas Search index with the Java driver, then waits until the search index can be queried;
- runs the dashboard jar as a local Java process with the same harness properties as the container (`dashboardProcess`), on the port of `dashboard.url`;
- restarts the dashboard after seeding, and stops it when the run ends.

Seeding wipes the datastore, so point the harness at a Solr core or Mongo database used only for tests.

```bash
# Solr
HARNESS_SOLR_URL=http://solrhost:8983/solr HARNESS_SOLR_USERNAME=... HARNESS_SOLR_PASSWORD=... \
  mvn -Pall,e2e verify -pl test-harness -Dharness.env=local-solr

# MongoDB Atlas (a database of its own)
HARNESS_MONGO_URI='mongodb+srv://user:password@cluster/' HARNESS_MONGO_DATABASE=ikasan_harness \
  mvn -Pall,e2e verify -pl test-harness -Dharness.env=local-mongo

# both backends in one run, as -Dharness.env=all does with docker
... mvn -Pall,e2e verify -pl test-harness -Dharness.env=all -Dharness.stack=local
```

Build the dashboard jar first (`mvn -Pall package -pl visualisation/dashboard -DskipTests`), or point `dashboardProcess.dashboardJar` at one. The dashboard runs on the harness's JVM unless `dashboardProcess.java` names another `java`. Its console output goes to `target/dashboard-<backend>.log`, and its last lines are logged if it fails to start.

The fake scheduler agent and fake modules run in the test JVM, and the dashboard reaches them on `localhost`. The real agent (`scheduler.agent: real`) needs one already running: set `scheduler.realAgentUrl` to its URL.

### Slow machines and CI

Every timeout and wait is multiplied by `run.timeoutScale` (`support/Timeouts`). That covers:
- Playwright's action, navigation and assertion timeouts;
- the tests' waits for asynchronous work, such as search indexing, job states and module calls;
- the dashboard start-up timeout and HTTP timeouts.

The default of `0` means automatic: `3` when the environment variable `CI` is `true` (Travis sets it), otherwise `1`. Raise it if a CI machine is slower still, with `HARNESS_RUN_TIMEOUT_SCALE=5` or `-Dharness.run.timeoutScale=5`. Locally, a failing wait still fails at the usual speed.

Only waits are scaled, never data. Seeded events and expectations resolve their time tokens against one anchor taken at seeding, so a long run doesn't move them out of their search windows.

Tests check notifications with `Vaadin.expectNotification`. Its first argument is the page; the second, the notification's text. It checks every notification the page has shown, not just the ones still open. The dashboard closes notifications after 5 seconds, and a slow machine can spend that long between an action and its check.

When writing tests:
- wait through `Timeouts` (`Timeouts.seconds(30)`), never a bare `Duration`;
- check notifications with `Vaadin.expectNotification`, not by locating `vaadin-notification-card`.

### External environments

Use this to test a dashboard you started yourself:

```bash
mvn -Pall,e2e verify -pl test-harness -Dharness.env=external -Dharness.dashboard.url=http://host:9090 -Dharness.backend=solr
```

Seeding is off by default for `external` because it wipes the datastore. If you enable it, the security documents are preserved, and you must restart the dashboard after seeding.

## Datasets (`data/baseline`)

- `manifest.yaml` defines the datasets. A dataset can `extends` others, and documents with the same id are overridden.
- Each dataset is a directory of NDJSON files, one document per line.
  - Field names are the shared Ikasan entity fields (`org.ikasan.spec.entity.EntityFields`).
  - The primary key is in `id`. The Mongo seeder stores it as `_id`.
  - `payloadRaw` (a replay event's raw bytes) is written as base64; the Mongo seeder stores it as BSON binary.
- The `core` dataset holds wiretaps, errors, exclusions (one per error whose `errorAction` is `ExcludeEvent`; the id is `<module>:exclusion:<errorUri>` and the exclusion dialog reads that error) and replay events, over two modules.
- Time values can use **time tokens**: `{{now}}`, `{{now-5m}}`, `{{now+30d}}`, `{{today@09:00}}`, `{{now-1d|iso}}`.
  Tokens resolve against a clock anchor: the run start, or `seed.clockAnchor`. Date-based behaviour therefore gives the same results on any day.
- **Capturing state:** to add data that is easier to create through the dashboard (job plans, business streams, configuration):
  ```bash
  mvn -q exec:java -Dexec.args="seed"
  # ... create the state in the dashboard UI or via its REST api ...
  mvn -q exec:java -Dexec.args="capture my-dataset scheduledContext,jobLockCache"
  ```
  This writes one NDJSON file per type, with timestamps turned into tokens, and prints the manifest entry to add.

## Personas (`data/personas.yaml`)

Users with narrower access than admin, for the role-based access tests (`it/access`). Each persona is a user with a role of the same name holding exactly the listed policies (the dashboard's baseline policies, whose names are the authorities the UI checks), optionally scoped to modules and job plans.

- `support/Personas` writes their `securityRole`, `securityPrincipal` and `securityUser` documents straight into the store (the same fields in Mongo and Solr) the first time a test asks for a persona, after the dashboard has created its baseline policies. The password is `{noop}` + `password` from the file.
- A test (or class) logs in as one with `@LoginAs("access-reader")`; each persona logs in once per session.
- Seeding personas adds users and roles, so classes that use them are tagged `mutating` and run after the read-only classes (e.g. `AdminGridsIT`, which lists every user and role).
- `Vaadin.isShown(locator)` answers "is this offered to the user": rendered and not hidden by `setVisible(false)`, regardless of a collapsed menu.

## Expectations (`expectations/`)

Each feature has YAML scenario files. A `@ParameterizedTest` loops over the scenarios, so adding a case needs no code:

```yaml
scenarios:
  - name: wiretaps by payload term
    tags: [wiretap]                 # selection via run.include / run.exclude
    backends: [mongo, solr]         # optional
    skip: false                     # or a reason string
    input:  { from: "{{now-1d}}", to: "{{now+1h}}", types: [wiretap], term: "expedite" }
    expect: { total: 1, visibleText: [harness-module-a] }
```

```java
static Stream<Scenario> scenarios() { return Scenarios.load("search/wiretap", "search"); }

@ParameterizedTest(name = "{0}") @MethodSource("scenarios")
void search(Scenario s, SearchPage page) { page.open().search(s.input(SearchInput.class)); ... }
```

- `Scenario.input(Type.class)` and `expect(Type.class)` bind the YAML to records.

### Grid rows

Grid expectations list complete rows (`support/GridRows`):

```yaml
patterns:                                   # named regexes for values that genuinely vary
  timestamp: '/\d{2}/\d{2}/\d{4} \d{2}:\d{2}:\d{2}\.\d{3} \[.+ - .+\]/'
scenarios:
  - name: all errors in the last day
    expect:
      rowsExact: true                       # the grid holds exactly these rows (any order)
      rows:
        - { "Entity Type": error, "Module Name": harness-module-a, "Flow Name": Order Flow, "Component Name": orderProducer,
            "Event Id / Error URI": "EVT-ERR-001 / err-0001", "Timestamp": "{{now-7m|table}}" }
        - { ..., "Last Access": "@timestamp" }   # a named pattern
```

- `Grid.rows()` reads **every** row, scrolling through the virtualised grid, as `header -> cell text`.
  - Columns without a header (checkboxes, action buttons) are left out.
  - The search results' type icon is reported as `Entity Type`.
- Each expected row must give a value for **every** column, otherwise the test fails, so an expectation can't quietly check only part of a row.
- A value matches exactly. A value written as `/regex/` must match the whole cell, and `"@name"` uses a pattern from the file's `patterns:` map.
- `{{…|table}}` renders a time token the way grids display it (`dd/MM/yyyy HH:mm:ss.SSS [zone - abbreviation]`). This works because the browser's time zone is pinned to `run.timezone` (default: this machine's zone).
- The search and role/policy expectations are complete copies of the baseline data: `data/baseline/core/*.ndjson`, and `baseline-security/*.json` in persistence-initialisation. Change both together.
- A seed records its clock anchor in `target/harness-clock-anchor`. `HARNESS_SKIP_BASELINE=true` runs reuse it, so exact timestamps still match.
- `expectations/env/<env>/...` is layered over the defaults, matching on file and scenario name. Use it to adjust individual values for one environment.

## Scheduler

The scheduler tests run job plan instances end to end: upload a plan, create an instance from the Job Plans grid, start its root job, and follow the dashboard sending command jobs to a scheduler agent and the agent reporting back, to completion; plus the operator actions on running instances.

### Agent modes (`scheduler.agent`)

| Mode | What runs the jobs | Use |
|---|---|---|
| `fake` (default) | `scheduler/FakeSchedulerAgent`, an HTTP server inside the harness JVM | fast and deterministic; outcomes can be scripted |
| `real` | the Ikasan `scheduler-agent` jar in a container (compose service `scheduler-agent`, profile `agent`) | real commands, real logs, kill |
| `none` | nothing | job plans can be managed, but command jobs never run |

**Fake agent.** `HarnessSession` starts it after the baseline and registers it with the dashboard as a `SCHEDULER_AGENT` module at `http://host.docker.internal:18090/scheduler-agent` (an agent must be registered before an instance is created: the instance keeps the agents' urls).
- It answers every call the dashboard makes to an agent (job provisioning, context instances, initiations, kill, ...) and records them: `calls()`, `initiations(instanceId)`, `awaitInitiation(...)`, `awaitFireNow(...)`.
- An initiated job "runs" and reports back to `PUT /rest/event/scheduled`: a starting event, then the outcome. The outcome comes from the job's command line, the way a real agent would end it: `exit N` (N &ne; 0) fails with return code N, `sleep S` runs for S seconds, anything else succeeds (`echo X` is its output). A job initiated as skipped is reported as skipped without running, as the real agent does.
- Tests can override that: `script(job, Outcome.fail(3) | succeed() | runUntilCompleted())`, then `complete(instanceId, job, outcome)`.
- Scheduled (quartz) and file jobs are fired by the agent itself, so tests fire them with `fireJob(plan, instanceId, job, childContexts, outcome)`.
- Tests that take a `FakeSchedulerAgent` parameter are skipped in the other modes.

**Real agent.** `-Dharness.scheduler.agent=real`. After the baseline the harness (re)starts the `scheduler-agent` container and waits until the agent has registered itself with the dashboard.
- The jar is `scheduler.realAgentJar` (default `~/.m2/repository/org/ikasan/scheduler-agent/5.0.0/scheduler-agent-5.0.0.jar`); 5.x targets Java 26, hence `eclipse-temurin:26-jre`.
- The 5.x jars ship without `application.properties`: `docker/agent/application.properties` is the last committed one (from the ikasan repo), and `docker/agent/application-harness-agent.properties` adapts it to the container (in-memory H2, state under `/tmp/agent`, the url it publishes, the dashboard at `http://dashboard:9090`).
- The agent refuses to run jobs until its start-up recovery of live instances from the dashboard has completed (`context.instance.recovery.active`, keep it `true`).
- Its log is in the container: `docker exec ikasan-harness-scheduler-agent-1 cat /tmp/agent/logs/application.log`; job output is under `/tmp/agent/process-logs`.
- Tests tagged `real-agent` only run in this mode.
- In the other modes the harness removes a `scheduler-agent` container left from an earlier real run: it would keep re-registering itself under the same name and take the jobs away from the fake agent.

### Job plan fixtures (`data/jobplans/<PLAN>`)

Each fixture is an unpacked job plan bundle (`context/<PLAN>.json`, `jobs/{internal,quartz,global}/*.json`); `scheduler/JobPlans.zip(plan)` zips it for the upload dialog. All use agent `scheduler-agent`, a yearly time window (`0 0 0 1 1 ? *`, so instances start only when a test creates them), a one day duration, and may run concurrently.

| Plan | Shape | Used for |
|---|---|---|
| `E2E-CHAIN` | quartz `trigger` &rarr; `okA` (`echo alpha`) &rarr; `okB` (`echo beta`); ends when complete | the happy path |
| `E2E-FAILURE` | `trigger` &rarr; `failJob` (`echo boom; exit 3`) &rarr; `after` | errors, acknowledge, reset |
| `E2E-LOGIC` | `trigger` &rarr; `a`, `b`, `c`; `d` = AND(a, b); `e` = OR(b, c) | dependency logic, hold/release all |
| `E2E-SLOW` | `trigger` &rarr; `slowJob` (`sleep 300`) &rarr; `next` | running jobs, kill |
| `E2E-PARAMS` | `trigger` &rarr; `paramJob` (`echo $GREETING`); plan parameter `GREETING=hello` | parameters |
| `E2E-GLOBAL` | global event `e2e-go` &rarr; `afterGlobal` | global events |
| `E2E-NESTED` | child plans `STEP-1` and `STEP-2` joined by a terminal and a start job | nested plans |
| `E2E-CRON` | time window every minute, 90 second duration, not concurrent | automatic start and end (`CronStartIT`) |
| `E2E-TOGGLE`, `E2E-SCHEDULED` | copies of `E2E-CHAIN` | tests that change a plan's state, so other expectations are unaffected |

Fixture rules learnt the hard way:
- Jobs reference their plan with `contextName` and `childContextNames` (not the older `contextId`/`childContextIds`), and every root job needs its own entry in `jobDependencies`; otherwise nothing downstream is ever initiated.
- A job only receives the plan parameters it declares in its own `contextParameters`.
- The zip's first entry must be the root directory.

### Helpers

- `scheduler/SchedulerApi`: register an agent, `planExists`, live `instances()`, `instanceStatus`/`jobStatus` (from the context machine, `ENDED` once it has left), `waitForInstance`/`waitForJob` (poll, never sleep), `raiseGlobalEvent`. Operator actions under test go through the UI.
- Page objects in `pages/scheduler`: `SchedulerPage`, `JobPlansPage` (upload, new plan, enable/disable, export, create instance), `ContextInstancePage` (job grid, job and instance actions, submit/acknowledge dialogs, log viewer), `MonitoringPage`.
- An instance opens in a new browser tab; use `instance.page()` for its notifications and dialogs. Failure screenshots include every open tab (`<test>-tab2.png`, ...).

### Things to know

- **Atlas Search lags behind writes** (about a second). The dashboard's free-text grid filters use it on Mongo, so filtering a grid straight after creating a row can find nothing: the page objects wait for the row's text instead of filtering.
- Job statuses are shown with spaces in the grids (`ON HOLD`), and with underscores over REST (`ON_HOLD`).
- Reset Job puts a job back to `WAITING`; it does not run it again (submit it).

## Layout

| Path | Contents |
|---|---|
| `src/main/java/.../config` | `HarnessConfig` (typed config) and `ConfigLoader` |
| `src/main/java/.../seed` | `Clock` (time tokens), `Dataset`, the `Seeder` interface, `MongoSeeder`, `SolrSeeder` |
| `src/main/java/.../support` | `Baseline`, `Compose`, `DashboardApi` (JWT REST client), `Expectations`/`Scenario`, `TagFilter`, `Vaadin` and `Grid` helpers |
| `src/main/java/.../pages` | page objects, one per view (`DesignerPage` reads the draw2d diagram of the business stream designer and viewer in the browser) |
| `src/main/java/.../junit` | `@HarnessTest`, `HarnessExtension`, `HarnessSession`, `Scenarios`, `@Unauthenticated` |
| `src/main/java/.../scheduler` | `FakeSchedulerAgent`, `SchedulerApi`, `JobPlans` |
| `src/main/java/.../module` | `FakeIkasanModule`: stands in for integration modules (exclusion resubmit/ignore, replay targets, flows and their states) |
| `data/jobplans/` | job plan fixtures |
| `src/main/java/.../cli` | `HarnessCli`: `seed`, `stack up`/`down`, `capture` |
| `src/test/java/.../it` | the end-to-end tests (`*IT`, run by failsafe) |
| `docker/` | the compose stack (the dashboards mount `docker/dashboard/images` as the designer's integrated system images), the dashboard's harness property overrides, and the real scheduler agent's configuration (`docker/agent`) |

## Coverage

| Test | What it covers | Expectations |
|---|---|---|
| `auth/LoginIT` | login (valid, wrong password, unknown user), logout, deep links to plain and parameterised routes | `auth/login.yaml` |
| `search/SearchIT` | event search of wiretaps, errors, exclusions and replay events: date window, entity types, payload term, grid column filters | `search/*.yaml` |
| `search/ExclusionLifecycleIT` *(mutating)* | open an exclusion (fields, error details), resubmit or ignore it from its dialog or in bulk, a module that rejects the resubmission, an exclusion whose error has gone, a module the dashboard has no metadata for (dialog and bulk), bulk action without a selection; checks the module request, the recorded `exclusionEventAction` and that the exclusion leaves the results | (in the test) |
| `search/ReplayLifecycleIT` *(mutating)* | open a replay event, replay it from its dialog (form validation) or in bulk (the replay manifest), a target that rejects the credentials, bulk replay without a selection; checks the target request (base64 raw payload) and the `replay_audit` written to the store | (in the test) |
| `search/EventDetailsIT` | the wiretap and error dialogs (fields, event life link, event and error details, XML/JSON/None formatting, download, own window), exporting results as CSV and as a ZIP of JSON files, nothing to export | `search/event-details.yaml` |
| `search/JsonEventDetailsIT` *(mutating)* | a JSON event is laid out by the JSON format and shown as stored by None | (in the test) |
| `search/EventLifeIdIT` | the `eventLifeId/{id}` deep link view | `search/event-life-id.yaml` |
| `access/AccessViewsIT` | per persona: the side menu offers exactly the views the persona may open, and deep links to other views are refused; a user with no landing page is told so and may log out | `access/views.yaml` |
| `access/AccessSearchIT` | per persona: the entity types it may search, the modules whose events it sees (all, one, none), and whether exclusion resubmit/ignore and replay are offered (bulk and in the dialogs) | (in the test) |
| `access/AccessAdminIT` | an administration reader cannot add users or add/delete roles | (in the test) |
| `access/AccessSchedulerIT` *(mutating)* | a scheduler reader may only look at plans and start instances; a writer may also upload, create and enable/disable plans, but not delete them; only a writer may synchronise an agent or download its log; links to the job plan and instance pages turn away users without scheduler access | (in the test) |
| `home/HomeDashboardIT` *(mutating)* | the home dashboard: flow status counts by state (states asked of fake modules, then a state reported by a module), the modules and business stream grids and filters, the exclusions-per-day and errors-per-minute charts against the store | (in the test) |
| `ingest/IngestionIT` *(mutating)* | data sent as modules send it (`PUT /rest/harvest/...`): wiretaps, an error and its exclusion, a replay event (replaying the exact bytes sent) and system events arrive in search, the dialogs and the audit view; the audit actor filter finds hyphenated names; malformed data, no token and users without REST access are refused | (in the test) |
| `stream/BusinessStreamIT` *(mutating)* | business streams: drawn in the designer from a module's flows, a message channel and a connection, saved (stored diagram checked), saved again without a new name, and opened again; Save as validation; deleting from Manage (after confirming); an integrated system added by double-click; the designer refused to users without business stream access; the stream view framing each flow in its state's colour and following state reports; the home dashboard's list, name filter and view link | (in the test) |
| `admin/AdminNavigationIT` | every Administration menu item opens its view | `admin/navigation.yaml` |
| `admin/AdminGridsIT` | users, roles, policies, groups, quartz, job locks and sessions grids: content and columns | `admin/grids.yaml` |
| `admin/UsersIT` *(mutating)* | create user, required-field validation, duplicate usernames, default role, add/remove a role, filter by full username | `admin/users.yaml` |
| `admin/RolesIT` *(mutating)* | create/delete a role, duplicate names, add/remove policies and users | `admin/roles.yaml` |
| `admin/PoliciesIT` | filter policies, open a policy profile | `admin/policies.yaml` |
| `admin/UserDirectoriesIT` *(mutating)* | add, disable/enable and delete an LDAP directory, and form validation | `admin/directories.yaml` |
| `admin/SessionsIT` *(mutating)* | a second, separate login is listed and ended by the admin, and that browser is then logged out; another session's details | `admin/sessions.yaml` |
| `admin/AuditIT` | system event search, actor filter, event details, CSV export | `admin/audit.yaml` |
| `admin/SupportIT` | support downloads, or the precondition message where a download isn't possible | `admin/support.yaml` |
| `scheduler/SchedulerNavigationIT` | the Scheduler view, the agents grid, the agent management dialog | `scheduler/navigation.yaml` |
| `scheduler/JobPlanProvisioningIT` *(mutating)* | upload every fixture (complete grid rows), jobs provisioned to the agent, upload errors, both export bundles contain the plan and its jobs | `scheduler/job-plans.yaml` |
| `scheduler/JobPlanLifecycleIT` *(mutating)* | New Job Plan validation (required fields, an empty duration), a new plan is created disabled, can be enabled without jobs and deleted, a new plan cannot reuse an existing name, disable/enable a plan, disable/enable its scheduled jobs | `scheduler/job-plans.yaml` |
| `scheduler/JobPlanManagementIT` | the management view: heading, tabs, validation, the job templates grid, the raw JSON; exports with and without tokens (agent names replaced by `[[agent.name]]`), from the management view and the Job Plans grid | `scheduler/management.yaml` |
| `scheduler/JobPlanInstanceRunIT` *(mutating)* | an instance runs to completion (complete job rows before and after, initiation order), AND/OR dependencies, plan parameters reach the agent, nested plans, a global event starts every waiting instance | `scheduler/instance-run.yaml` |
| `scheduler/JobInstanceActionsIT` *(mutating)* | hold/release, skip, disable/reinstate, error &rarr; acknowledge &rarr; reset &rarr; resubmit, submit a command job by hand, submit the scheduled trigger, a running job, hold/release all, disable/enable an instance's scheduled jobs, ignore the duration and end an instance by hand | |
| `scheduler/ContextInstanceMonitoringIT` *(mutating)* | the active instances grid with every status count, the ERROR filter, ended instances move to the completed grid | |
| `scheduler/JobPlanEditorIT` *(mutating)* | the visual job plan editor on its own plan (E2E-EDITOR): the child plan's jobs and dependencies drawn; a new command job (form validation, saved, drawn); dependencies drawn between jobs, alone or in an AND box, saved into the plan and shown again; a connection drawn or deleted on the canvas is an unsaved change and is saved; a global event job and a scheduled job (no timezone; its save reported as a success) added; overlapping figures refused; leaving with unsaved changes asks first; bad links show page-not-found; a scheduler reader can look but not change | (in the test) |
| `scheduler/RealAgentIT` *(real-agent)* | on the real agent: the trigger fired by the agent, command output in the job log (`alpha`, `boom`, `hello` from `$GREETING`), `exit 3` &rarr; ERROR, ending an instance kills a running `sleep` | |
| `scheduler/CronStartIT` *(slow)* | an instance started and ended by the plan's own time window, then listed as completed | |

These are not covered, because they need systems the harness doesn't run:

- LDAP *Test*/*Synchronise* and LDAP groups (they need an LDAP server).
- Module log downloads, and adding modules to roles (they need a running module).
- Job lock release/reset (it needs job plans holding locks).
- Scheduler: file watcher jobs, job locks, the Prepared instances tab, Reset Job Plan, the visualisation and DAG views, statistics and the dead letter queue.

Admin users, groups and policies can't be edited or deleted in the dashboard, so there are no such tests. Each run creates uniquely named users and roles, and the next baseline removes them.

Scheduler bugs seen but not covered by a failing test:
- Intermittent (seen once in about ten runs, on Mongo): after a job was skipped, every job of the instance finished (`SKIPPED_COMPLETE`, `COMPLETE`) but its child plan stayed `RUNNING` in the context machine, so the instance never ended (`JobInstanceActionsIT#skippedJobIsNotRunAndTheChainContinues`).

Smaller inconsistencies the tests show:
- The Quartz Scheduler and Session Management grids display times in the server's zone (`Etc/UTC`), while every other grid uses the user's zone.
- In the baseline security data, the `AdminUser` and `WriteUser` roles have the same description as `RESTAccess`.

Bugs these tests found that are now fixed:
- **Duplicate role names** (`RolesIT#duplicateRoleNameIsRejected`): role ids are derived from the name, so a duplicate used to overwrite the existing role. `NewRoleDialog` now checks the name first.
- **Duplicate usernames** (`UsersIT#duplicateUsernameIsRejected`): user ids are derived from the username, so creating an existing username used to overwrite that user (for example `admin`). `NewUserDialog` now checks the username first.
- **Solr user filters** (`UsersIT#filterUsersByFullUsername`): values containing `-`, `@` or spaces never matched. `SolrUserDaoImpl.containsClause` now splits them the way the index tokenizer does.
- **Deep links:** they were lost after login.
- **Another user's session details could not be opened** (`SessionsIT#showAnotherUserSessionDetails`): `SessionDetailsDialog` read that session's locale, which Vaadin 25 only lets the session itself read. Sessions now also record their locale in the HTTP session (`SessionLocale`), which the dialog reads for other sessions. Sessions logged in before the change show no locale. The dialog's Browser field still reads the other session directly; that is only an assertion in production mode, but could fail in Vaadin development mode.
- **The `eventLifeId`/`exclusion` views:** they failed to open on Mongo because of an ambiguous `ESBSearchService` bean.
- **Enabling a job plan without jobs failed** (`JobPlanLifecycleIT#newEmptyJobPlanCanBeEnabled`) with "The following unknown agents were encountered - ": `JobProvisionServiceImpl#provisionJobs` looked up the agents for an empty list of names, which matches every agent. It now returns when there is nothing to provision.
- **Enabling scheduled jobs showed the "disabled" message** (`JobPlanLifecycleIT#scheduledJobsCanBeDisabledAndEnabled`), in `ContextTemplateWidget` and `SchedulerJobGridWidget`.
- **"Manually Submitted By" was lost** (`JobInstanceActionsIT#commandJobCanBeSubmittedByHand`): every submit path (command, file, scheduled, global and local event jobs; grid, tree view and job dialogs) sent the job first and then saved its own, out of date copy of the row, which the context machine's saves overwrote (or which could put an old status back). They now record the submitter on the current row before sending (`ManualJobSubmission`).
- **A new job plan could overwrite an existing one** (`JobPlanLifecycleIT#newJobPlanCannotReuseAnExistingName`): a plan's name is its id, and `ContextTemplateDialog#saveNew` did not check it. It now refuses an existing name on the name field.
- **Saving a new job plan reported success after an error**, and closed the dialog. It now reports the error only and keeps the dialog open.
- **An empty Duration Hours/Days/Minutes threw a `NullPointerException`** (`JobPlanLifecycleIT#emptyDurationIsAValidationError`) in `ContextTemplateDialog#validate`; it is now a validation error. The same method checked Duration Hours twice and never Duration Days, so an invalid Duration Days was ignored.
- **The management view's "Download Job Plan Bundle With Tokens" exported without tokens** (`JobPlanManagementIT#exportsWithAndWithoutTokens`): both of its links passed `withTokens=false` (`ContextTemplateManagementWidget`).
- **The job plan grid's Enabled/Disabled buttons were both clickable** (`JobPlanLifecycleIT`): the button of the plan's current state was disabled and then enabled again by the security check that follows (`ComponentSecurityVisibility.applyEnabledSecurity`); the state is now applied after it (`ContextTemplateWidget`).
- **Job plan instances on Mongo** (`JobPlanInstanceRunIT`, `JobInstanceActionsIT`, `ContextInstanceMonitoringIT`): jobs were left RUNNING or marked KILLED after completing, start/end times were not saved, instances never ended, and status counts were wrong. Causes and fixes:
  - The Mongo job instance DAO answered every lookup with Atlas Search, which indexes about a second after a write, while the scheduler reads back what it has just written. Lookups scoped to one instance now query the collection (`MongoSchedulerJobInstanceDaoImpl`, `AtlasSearchQueryHelper#searchWithoutIndex`).
  - `SchedulerJobInstanceServiceImpl#update` took the first job whose name *contained* the job's name (job `e` matched `trigger`), failed silently when it found none, and did not save start/end times. It now matches exactly, logs a missing row, and saves the times.
  - `JobLogicMachine` handed state changes to its listeners on five threads, so a job's RUNNING update could be saved after its COMPLETE update. They are now delivered in order, and a failing listener is logged instead of lost.
  - A new instance's context machine is built from the jobs just created instead of reading them back (`ContextInstanceServiceBase`).
  - `AggregateStatusCollector` (the active instances grid) could keep an ended instance when two rebuilds of its list overlapped; it now only reports instances that are still live.

## Selectors and dashboard ids

Prefer ids set in the dashboard code with `setId(...)`, plus state attributes such as `aria-pressed` and `data-*`. When a test needs an element that has no stable hook, add one to the dashboard view, keeping existing ids that the Karibu tests use.

Hooks added for the harness so far:

- `loginForm` and `loginLanguageSelect`.
- Search form fields (`searchForm*`) and the `aria-pressed` state on the type toggles.
- `searchResultsGrid`, and `searchResultsCountLabel[data-result-count]`.
- `data-entity-type` on result rows.
- Scheduler: `jobPlanGrid`, `newJobPlanButton`, `uploadJobPlanButton`, `refreshJobPlanGridButton`, `refreshJobParametersButton`; `jobPlanUpload`, `jobPlanUploadSaveButton`, `jobPlanUploadCancelButton`; `validateJobPlanButton`, `synchroniseJobsButton`, `jobTemplateGrid`, `jobTemplateGridRefreshButton`; `jobInstanceGrid`, `jobInstanceGridRefreshButton`; and ids on the management (`jobPlan*Tab`) and instance (`instance*Tab`) tabs.
- Row actions in the scheduler grids are found by their icon's `title` (the tooltip text), which every action icon already has.
