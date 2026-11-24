package org.ikasan.testharness.junit;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Tracing;
import org.ikasan.testharness.config.ConfigLoader;
import org.ikasan.testharness.config.HarnessConfig;
import org.ikasan.testharness.pages.AdminGridPage;
import org.ikasan.testharness.pages.AppLayout;
import org.ikasan.testharness.pages.LoginPage;
import org.ikasan.testharness.pages.SearchPage;
import org.ikasan.testharness.seed.Clock;
import org.ikasan.testharness.support.DashboardApi;
import org.ikasan.testharness.support.TagFilter;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.platform.commons.support.AnnotationSupport;

import java.lang.reflect.Constructor;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * JUnit 5 extension behind {@link HarnessTest}.
 * <ul>
 *     <li>establishes the baseline and starts the browser once per run ({@link HarnessSession}, kept in the
 *     root store so every test class shares it)</li>
 *     <li>gives every test a fresh browser context and page, logged in unless {@link Unauthenticated}</li>
 *     <li>injects test parameters: {@link Page}, {@link BrowserContext}, {@link HarnessConfig}, {@link Clock},
 *     {@link HarnessSession}, {@link DashboardApi} and any page object with a {@code (Page)} constructor</li>
 *     <li>applies the run's tag selection to non-parameterized tests (scenario-driven tests are filtered by
 *     {@link org.ikasan.testharness.support.Expectations})</li>
 *     <li>keeps a trace and a screenshot of failed tests under target/playwright/</li>
 * </ul>
 */
public class HarnessExtension implements BeforeAllCallback, BeforeEachCallback, AfterEachCallback, ParameterResolver,
    ExecutionCondition
{
    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(HarnessExtension.class);
    private static final Set<Class<?>> PAGE_OBJECTS = Set.of(LoginPage.class, SearchPage.class, AppLayout.class,
        AdminGridPage.class, org.ikasan.testharness.pages.scheduler.JobPlansPage.class,
        org.ikasan.testharness.pages.scheduler.SchedulerPage.class);

    public static HarnessSession session(ExtensionContext context)
    {
        return context.getRoot().getStore(NAMESPACE)
            .getOrComputeIfAbsent(HarnessSession.class, k -> new HarnessSession(), HarnessSession.class);
    }

    @Override
    public void beforeAll(ExtensionContext context)
    {
        session(context);
    }

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context)
    {
        if (context.getTestMethod().isEmpty()
            || AnnotationSupport.isAnnotated(context.getTestMethod(), ParameterizedTest.class))
        {
            return ConditionEvaluationResult.enabled("selection applies to test methods / scenarios");
        }
        HarnessConfig config = ConfigLoader.get();
        HarnessConfig.Scheduler scheduler = config.scheduler();
        java.util.Set<String> tags = context.getTags();
        // tests that drive the in-JVM fake agent cannot run against a real (or no) agent
        if (!scheduler.fake() && java.util.Arrays.stream(context.getRequiredTestMethod().getParameterTypes())
            .anyMatch(org.ikasan.testharness.scheduler.FakeSchedulerAgent.class::equals))
        {
            return ConditionEvaluationResult.disabled("needs the fake scheduler agent; scheduler.agent is '"
                + scheduler.agent() + "'");
        }
        if (tags.contains("real-agent") && !scheduler.real())
        {
            return ConditionEvaluationResult.disabled("needs the real scheduler agent (-Dharness.scheduler.agent=real); "
                + "scheduler.agent is '" + scheduler.agent() + "'");
        }
        // slow tests (minutes) only run when asked for by name
        if (tags.contains("slow") && config.run().include().stream().map(TagFilter::normalise).noneMatch("slow"::equals))
        {
            return ConditionEvaluationResult.disabled("slow: run with -Dharness.include=slow");
        }
        TagFilter filter = TagFilter.of(config);
        return filter.isUnrestricted() || filter.accepts(context.getTags())
            ? ConditionEvaluationResult.enabled("tags selected")
            : ConditionEvaluationResult.disabled("not selected by run.include/run.exclude: " + context.getTags());
    }

    @Override
    public void beforeEach(ExtensionContext context)
    {
        HarnessSession session = session(context);
        boolean anonymous = AnnotationSupport.isAnnotated(context.getTestMethod(), Unauthenticated.class)
            || AnnotationSupport.isAnnotated(context.getTestClass(), Unauthenticated.class);
        BrowserContext browserContext = session.newContext(anonymous ? null : session.authenticatedState());
        if (session.config().run().traceOnFailure())
        {
            browserContext.tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true).setSources(false));
        }
        ExtensionContext.Store store = context.getStore(NAMESPACE);
        store.put(BrowserContext.class, browserContext);
        store.put(Page.class, browserContext.newPage());
    }

    @Override
    public void afterEach(ExtensionContext context)
    {
        ExtensionContext.Store store = context.getStore(NAMESPACE);
        BrowserContext browserContext = store.remove(BrowserContext.class, BrowserContext.class);
        Page page = store.remove(Page.class, Page.class);
        if (browserContext == null)
        {
            return;
        }
        try
        {
            boolean trace = session(context).config().run().traceOnFailure();
            if (trace && context.getExecutionException().isPresent())
            {
                Path dir = ConfigLoader.harnessRoot().resolve("target/playwright");
                String name = (context.getRequiredTestClass().getSimpleName() + "-" + context.getDisplayName())
                    .replaceAll("[^A-Za-z0-9._-]+", "_");
                if (page != null && !page.isClosed())
                {
                    page.screenshot(new Page.ScreenshotOptions().setPath(dir.resolve(name + ".png")).setFullPage(true));
                }
                // tabs the test opened (e.g. a job plan instance) get their own screenshot: <name>-tab2.png, ...
                List<Page> tabs = browserContext.pages();
                for (int i = 0; i < tabs.size(); i++)
                {
                    Page tab = tabs.get(i);
                    if (tab != page && !tab.isClosed())
                    {
                        tab.screenshot(new Page.ScreenshotOptions().setPath(dir.resolve(name + "-tab" + (i + 1) + ".png"))
                            .setFullPage(true));
                    }
                }
                browserContext.tracing().stop(new Tracing.StopOptions().setPath(dir.resolve(name + ".zip")));
                System.err.println("[harness] trace: " + dir.resolve(name + ".zip")
                    + " (open with: mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args=\"show-trace <zip>\")");
            }
            else if (trace)
            {
                browserContext.tracing().stop();
            }
        }
        finally
        {
            browserContext.close();
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
    {
        Class<?> type = parameterContext.getParameter().getType();
        return type == Page.class || type == BrowserContext.class || type == HarnessConfig.class || type == Clock.class
            || type == HarnessSession.class || type == DashboardApi.class || PAGE_OBJECTS.contains(type)
            || type == org.ikasan.testharness.scheduler.SchedulerApi.class
            || type == org.ikasan.testharness.scheduler.FakeSchedulerAgent.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
    {
        Class<?> type = parameterContext.getParameter().getType();
        HarnessSession session = session(extensionContext);
        if (type == HarnessConfig.class)
        {
            return session.config();
        }
        if (type == Clock.class)
        {
            return session.clock();
        }
        if (type == HarnessSession.class)
        {
            return session;
        }
        if (type == DashboardApi.class)
        {
            return new DashboardApi(session.config());
        }
        if (type == org.ikasan.testharness.scheduler.SchedulerApi.class)
        {
            return session.schedulerApi();
        }
        if (type == org.ikasan.testharness.scheduler.FakeSchedulerAgent.class)
        {
            return session.fakeAgent().orElseThrow(() -> new ParameterResolutionException(
                "No fake scheduler agent: scheduler.agent is '" + session.config().scheduler().agent() + "'"));
        }
        ExtensionContext.Store store = extensionContext.getStore(NAMESPACE);
        if (type == BrowserContext.class)
        {
            return store.get(BrowserContext.class, BrowserContext.class);
        }
        Page page = store.get(Page.class, Page.class);
        if (page == null)
        {
            throw new ParameterResolutionException(type.getSimpleName() + " is only available in test methods");
        }
        if (type == Page.class)
        {
            return page;
        }
        try
        {
            Constructor<?> constructor = type.getConstructor(Page.class);
            return constructor.newInstance(page);
        }
        catch (ReflectiveOperationException e)
        {
            throw new ParameterResolutionException("Cannot create page object " + type.getName(), e);
        }
    }
}
