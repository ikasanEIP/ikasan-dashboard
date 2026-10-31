package org.ikasan.rest.dashboard;

import org.ikasan.rest.dashboard.util.TestModuleMetaDataService;
import org.ikasan.rest.dashboard.util.TestScheduledContextService;
import org.ikasan.rest.dashboard.util.TestSecurityService;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link SecurityController} using MockMVC.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = SecurityController.class)
@WebAppConfiguration
@EnableWebMvc
@ContextConfiguration(
    {
        "/substitute-components.xml"
    }
)
public class SecurityControllerTest extends AbstractRestMvcTest {

    protected MockMvc mvc;

    @Autowired
    WebApplicationContext webApplicationContext;

    @Autowired
    TestSecurityService securityService;

    @Autowired
    TestModuleMetaDataService moduleMetaDataService;

    @Autowired
    TestScheduledContextService scheduledContextService;

    @Before
    public void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        securityService.reset();
        moduleMetaDataService.clear();
        scheduledContextService.clear();

        // Create test module
        ModuleMetaData testModule = Mockito.mock(ModuleMetaData.class);
        Mockito.when(testModule.getName()).thenReturn("test-module");
        moduleMetaDataService.save("test-module", testModule);

        // Create module with special characters
        ModuleMetaData specialModule = Mockito.mock(ModuleMetaData.class);
        Mockito.when(specialModule.getName()).thenReturn("module-name_with_special_chars");
        moduleMetaDataService.save("module-name_with_special_chars", specialModule);

        // Create test job plan
        ScheduledContextRecord testJobPlan = Mockito.mock(ScheduledContextRecord.class);
        Mockito.when(testJobPlan.getContextName()).thenReturn("test-job-plan");
        scheduledContextService.save("test-job-plan", testJobPlan);

        // Create additional job plans
        ScheduledContextRecord jobPlan1 = Mockito.mock(ScheduledContextRecord.class);
        Mockito.when(jobPlan1.getContextName()).thenReturn("job-plan-1");
        scheduledContextService.save("job-plan-1", jobPlan1);

        ScheduledContextRecord jobPlan2 = Mockito.mock(ScheduledContextRecord.class);
        Mockito.when(jobPlan2.getContextName()).thenReturn("job-plan-2");
        scheduledContextService.save("job-plan-2", jobPlan2);
    }

    @Test
    public void testAssignModuleToRoles_success() throws Exception {
        String uri = "/rest/security/module/roles?moduleName=test-module";
        String roleNamesJson = "[\"Admin\", \"User\"]";

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.post(uri)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(roleNamesJson))
            .andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.OK.value(), status);

        // Response should have empty body
        String content = mvcResult.getResponse().getContentAsString();
        assertTrue(content.isEmpty());

        // Verify the SecurityService received the calls
        assertEquals(2, securityService.getSavedRoleModules().size());
        assertEquals("test-module", securityService.getSavedRoleModules().get(0).getModuleName());
        assertEquals("test-module", securityService.getSavedRoleModules().get(1).getModuleName());
    }

    @Test
    public void testAssignModuleToRoles_withNullModuleName_returnsBadRequest() throws Exception {
        String uri = "/rest/security/module/roles";
        String roleNamesJson = "[\"Admin\"]";

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.post(uri)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(roleNamesJson))
            .andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.BAD_REQUEST.value(), status);

        // Spring returns empty body for missing required parameter, just check status
        assertEquals(0, securityService.getSavedRoleModules().size());
    }

    @Test
    public void testAssignModuleToRoles_withEmptyModuleName_returnsBadRequest() throws Exception {
        String uri = "/rest/security/module/roles?moduleName=   ";
        String roleNamesJson = "[\"Admin\"]";

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.post(uri)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(roleNamesJson))
            .andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.BAD_REQUEST.value(), status);

        String content = mvcResult.getResponse().getContentAsString();
        assertThat(content, containsString("Module name cannot be null or empty"));

        assertEquals(0, securityService.getSavedRoleModules().size());
    }

    @Test
    public void testAssignModuleToRoles_withEmptyRoleNames_returnsBadRequest() throws Exception {
        String uri = "/rest/security/module/roles?moduleName=test-module";
        String roleNamesJson = "[]";

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.post(uri)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(roleNamesJson))
            .andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.BAD_REQUEST.value(), status);

        String content = mvcResult.getResponse().getContentAsString();
        assertThat(content, containsString("Role names list cannot be null or empty"));

        assertEquals(0, securityService.getSavedRoleModules().size());
    }

    @Test
    public void testAssignModuleToRoles_withNonExistentRole_returnsBadRequest() throws Exception {
        String uri = "/rest/security/module/roles?moduleName=test-module";
        String roleNamesJson = "[\"Admin\", \"NonExistent\", \"User\"]";

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.post(uri)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(roleNamesJson))
            .andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.BAD_REQUEST.value(), status);

        String content = mvcResult.getResponse().getContentAsString();
        assertThat(content, containsString("The following roles are not valid"));
        assertThat(content, containsString("NonExistent"));

        // Should not save any role modules when validation fails
        assertEquals(0, securityService.getSavedRoleModules().size());
    }

    @Test
    public void testAssignModuleToRoles_withSingleInvalidRole_returnsBadRequest() throws Exception {
        String uri = "/rest/security/module/roles?moduleName=test-module";
        String roleNamesJson = "[\"InvalidRole\"]";

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.post(uri)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(roleNamesJson))
            .andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.BAD_REQUEST.value(), status);

        String content = mvcResult.getResponse().getContentAsString();
        assertThat(content, containsString("The following roles are not valid"));
        assertThat(content, containsString("InvalidRole"));

        assertEquals(0, securityService.getSavedRoleModules().size());
    }

    @Test
    public void testAssignModuleToRoles_withMultipleInvalidRoles_returnsBadRequest() throws Exception {
        String uri = "/rest/security/module/roles?moduleName=test-module";
        String roleNamesJson = "[\"InvalidRole1\", \"InvalidRole2\", \"InvalidRole3\"]";

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.post(uri)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(roleNamesJson))
            .andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.BAD_REQUEST.value(), status);

        String content = mvcResult.getResponse().getContentAsString();
        assertThat(content, containsString("The following roles are not valid"));
        assertThat(content, containsString("InvalidRole1"));
        assertThat(content, containsString("InvalidRole2"));
        assertThat(content, containsString("InvalidRole3"));

        assertEquals(0, securityService.getSavedRoleModules().size());
    }

    @Test
    public void testAssignModuleToRoles_withNullRoleInList_returnsBadRequest() throws Exception {
        String uri = "/rest/security/module/roles?moduleName=test-module";
        String roleNamesJson = "[\"Admin\", null, \"User\"]";

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.post(uri)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(roleNamesJson))
            .andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.BAD_REQUEST.value(), status);

        String content = mvcResult.getResponse().getContentAsString();
        assertThat(content, containsString("The following roles are not valid"));
        assertThat(content, containsString("null"));

        assertEquals(0, securityService.getSavedRoleModules().size());
    }

    @Test
    public void testAssignModuleToRoles_withEmptyStringRoleInList_returnsBadRequest() throws Exception {
        String uri = "/rest/security/module/roles?moduleName=test-module";
        String roleNamesJson = "[\"Admin\", \"\", \"User\"]";

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.post(uri)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(roleNamesJson))
            .andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.BAD_REQUEST.value(), status);

        String content = mvcResult.getResponse().getContentAsString();
        assertThat(content, containsString("The following roles are not valid"));
        assertThat(content, containsString("(empty)"));

        assertEquals(0, securityService.getSavedRoleModules().size());
    }

    @Test
    public void testAssignModuleToRoles_withInvalidJson_returnsBadRequest() throws Exception {
        String uri = "/rest/security/module/roles?moduleName=test-module";
        String invalidJson = "not a valid json";

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.post(uri)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(invalidJson))
            .andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.BAD_REQUEST.value(), status);
    }

    @Test
    public void testAssignJobPlanToRoles_success() throws Exception {
        String uri = "/rest/security/jobplan/roles?jobPlanName=test-job-plan";
        String roleNamesJson = "[\"Admin\", \"User\"]";

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.post(uri)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(roleNamesJson))
            .andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.OK.value(), status);

        // Response should have empty body
        String content = mvcResult.getResponse().getContentAsString();
        assertTrue(content.isEmpty());

        // Verify the SecurityService received the call
        assertTrue(securityService.getJobPlanRoles().containsKey("test-job-plan"));
        assertEquals(2, securityService.getJobPlanRoles().get("test-job-plan").size());
    }

    @Test
    public void testAssignJobPlanToRoles_withNullJobPlanName_returnsBadRequest() throws Exception {
        String uri = "/rest/security/jobplan/roles";
        String roleNamesJson = "[\"Admin\"]";

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.post(uri)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(roleNamesJson))
            .andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.BAD_REQUEST.value(), status);

        // Spring returns empty body for missing required parameter, just check status
        assertTrue(securityService.getJobPlanRoles().isEmpty());
    }

    @Test
    public void testAssignJobPlanToRoles_withEmptyJobPlanName_returnsBadRequest() throws Exception {
        String uri = "/rest/security/jobplan/roles?jobPlanName=   ";
        String roleNamesJson = "[\"Admin\"]";

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.post(uri)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(roleNamesJson))
            .andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.BAD_REQUEST.value(), status);

        String content = mvcResult.getResponse().getContentAsString();
        assertThat(content, containsString("Job plan name cannot be null or empty"));

        assertTrue(securityService.getJobPlanRoles().isEmpty());
    }

    @Test
    public void testAssignJobPlanToRoles_withEmptyRoleNames_returnsBadRequest() throws Exception {
        String uri = "/rest/security/jobplan/roles?jobPlanName=test-job-plan";
        String roleNamesJson = "[]";

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.post(uri)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(roleNamesJson))
            .andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.BAD_REQUEST.value(), status);

        String content = mvcResult.getResponse().getContentAsString();
        assertThat(content, containsString("Role names list cannot be null or empty"));

        assertTrue(securityService.getJobPlanRoles().isEmpty());
    }

    @Test
    public void testAssignJobPlanToRoles_withInvalidJson_returnsBadRequest() throws Exception {
        String uri = "/rest/security/jobplan/roles?jobPlanName=test-job-plan";
        String invalidJson = "{invalid json}";

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.post(uri)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(invalidJson))
            .andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.BAD_REQUEST.value(), status);
    }

    @Test
    public void testAssignJobPlanToRoles_withInvalidRole_returnsBadRequest() throws Exception {
        String uri = "/rest/security/jobplan/roles?jobPlanName=test-job-plan";
        String roleNamesJson = "[\"Admin\", \"InvalidRole\"]";

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.post(uri)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(roleNamesJson))
            .andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.BAD_REQUEST.value(), status);

        String content = mvcResult.getResponse().getContentAsString();
        assertThat(content, containsString("The following roles are not valid"));
        assertThat(content, containsString("InvalidRole"));

        assertTrue(securityService.getJobPlanRoles().isEmpty());
    }

    @Test
    public void testAssignJobPlanToRoles_withMultipleInvalidRoles_returnsBadRequest() throws Exception {
        String uri = "/rest/security/jobplan/roles?jobPlanName=test-job-plan";
        String roleNamesJson = "[\"Bad1\", \"Bad2\"]";

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.post(uri)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(roleNamesJson))
            .andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.BAD_REQUEST.value(), status);

        String content = mvcResult.getResponse().getContentAsString();
        assertThat(content, containsString("The following roles are not valid"));
        assertThat(content, containsString("Bad1"));
        assertThat(content, containsString("Bad2"));

        assertTrue(securityService.getJobPlanRoles().isEmpty());
    }

    @Test
    public void testAssignJobPlanToRoles_withNullRoleInList_returnsBadRequest() throws Exception {
        String uri = "/rest/security/jobplan/roles?jobPlanName=test-job-plan";
        String roleNamesJson = "[null, \"Admin\"]";

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.post(uri)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(roleNamesJson))
            .andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.BAD_REQUEST.value(), status);

        String content = mvcResult.getResponse().getContentAsString();
        assertThat(content, containsString("The following roles are not valid"));
        assertThat(content, containsString("null"));

        assertTrue(securityService.getJobPlanRoles().isEmpty());
    }

    @Test
    public void testAssignJobPlanToRoles_multipleJobPlans() throws Exception {
        // First job plan
        String uri1 = "/rest/security/jobplan/roles?jobPlanName=job-plan-1";
        String roleNamesJson1 = "[\"Admin\"]";

        MvcResult result1 = mvc.perform(MockMvcRequestBuilders.post(uri1)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(roleNamesJson1))
            .andReturn();

        assertEquals(HttpStatus.OK.value(), result1.getResponse().getStatus());

        // Second job plan
        String uri2 = "/rest/security/jobplan/roles?jobPlanName=job-plan-2";
        String roleNamesJson2 = "[\"User\", \"Developer\"]";

        MvcResult result2 = mvc.perform(MockMvcRequestBuilders.post(uri2)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(roleNamesJson2))
            .andReturn();

        assertEquals(HttpStatus.OK.value(), result2.getResponse().getStatus());

        // Verify both job plans are stored
        assertEquals(2, securityService.getJobPlanRoles().size());
        assertTrue(securityService.getJobPlanRoles().containsKey("job-plan-1"));
        assertTrue(securityService.getJobPlanRoles().containsKey("job-plan-2"));
        assertEquals(1, securityService.getJobPlanRoles().get("job-plan-1").size());
        assertEquals(2, securityService.getJobPlanRoles().get("job-plan-2").size());
    }

    @Test
    public void testAssignModuleToRoles_specialCharactersInModuleName() throws Exception {
        String uri = "/rest/security/module/roles?moduleName=module-name_with_special_chars";
        String roleNamesJson = "[\"Admin\"]";

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.post(uri)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(roleNamesJson))
            .andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.OK.value(), status);

        assertEquals(1, securityService.getSavedRoleModules().size());
    }

    @Test
    public void testAssignModuleToRoles_withInvalidModuleName_returnsBadRequest() throws Exception {
        String uri = "/rest/security/module/roles?moduleName=non-existent-module";
        String roleNamesJson = "[\"Admin\", \"User\"]";

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.post(uri)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(roleNamesJson))
            .andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.BAD_REQUEST.value(), status);

        String content = mvcResult.getResponse().getContentAsString();
        assertThat(content, containsString("Module [non-existent-module] does not exist"));

        assertEquals(0, securityService.getSavedRoleModules().size());
    }

    @Test
    public void testAssignJobPlanToRoles_withInvalidJobPlanName_returnsBadRequest() throws Exception {
        String uri = "/rest/security/jobplan/roles?jobPlanName=non-existent-job-plan";
        String roleNamesJson = "[\"Admin\", \"User\"]";

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.post(uri)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(roleNamesJson))
            .andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.BAD_REQUEST.value(), status);

        String content = mvcResult.getResponse().getContentAsString();
        assertThat(content, containsString("Job plan [non-existent-job-plan] does not exist"));

        assertTrue(securityService.getJobPlanRoles().isEmpty());
    }
}
