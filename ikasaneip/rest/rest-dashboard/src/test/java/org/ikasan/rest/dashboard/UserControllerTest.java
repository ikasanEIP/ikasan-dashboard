package org.ikasan.rest.dashboard;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.ikasan.rest.dashboard.util.TestUserService;
import org.ikasan.security.model.*;
import org.ikasan.spec.security.model.*;
import org.junit.Assert;
import org.junit.Before;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import javax.annotation.Resource;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = UserController.class)
@WebAppConfiguration
@EnableWebMvc
@ContextConfiguration(
    {
        "/substitute-components.xml"
    }
)
public class UserControllerTest extends  AbstractRestMvcTest
{
    protected MockMvc mvc;

    @Autowired
    WebApplicationContext webApplicationContext;

    @Autowired
    TestUserService userService;

    private ObjectMapper objectMapper;

    @Before
    public void setUp()
    {
        userService.reset();
        mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addAbstractTypeMapping(IkasanPrincipal.class, IkasanPrincipalImpl.class);
        simpleModule.addAbstractTypeMapping(Role.class, RoleImpl.class);
        simpleModule.addAbstractTypeMapping(RoleModule.class, RoleModuleImpl.class);
        simpleModule.addAbstractTypeMapping(RoleJobPlan.class, RoleJobPlanImpl.class);
        simpleModule.addAbstractTypeMapping(Policy.class, PolicyImpl.class);
        simpleModule.addAbstractTypeMapping(User.class, UserImpl.class);

        objectMapper.registerModule(simpleModule);
    }


    @Test
    @DirtiesContext
    public void test_get_user_success() throws Exception
    {
        String uri = "/rest/user";

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("username", "mockuser");

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get(uri)
            .contentType(MediaType.APPLICATION_JSON_VALUE).params(params)).andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.OK.value(), status);
        String content = mvcResult.getResponse().getContentAsString();

        User user = objectMapper.readValue(content, UserImpl.class);

        Assert.assertEquals(2, user.getPrincipals().size());
        Assert.assertEquals(3, user.getPrincipals().stream().findFirst().get()
            .getRoles().size());
        Assert.assertEquals(3, user.getPrincipals().stream().findFirst().get()
            .getRoles().stream().findFirst().get().getRoleModules().size());
        Assert.assertEquals(3, user.getPrincipals().stream().findFirst().get()
            .getRoles().stream().findFirst().get().getRoleJobPlans().size());
    }

    @Test
    @DirtiesContext
    public void test_get_user_success_cache() throws Exception
    {
        String uri = "/rest/user";

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("username", "mockuser");

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get(uri)
            .contentType(MediaType.APPLICATION_JSON_VALUE).params(params)).andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.OK.value(), status);
        String content = mvcResult.getResponse().getContentAsString();

        User user = objectMapper.readValue(content, UserImpl.class);

        Assert.assertEquals(2, user.getPrincipals().size());
        Assert.assertEquals(3, user.getPrincipals().stream().findFirst().get()
            .getRoles().size());
        Assert.assertEquals(3, user.getPrincipals().stream().findFirst().get()
            .getRoles().stream().findFirst().get().getRoleModules().size());
        Assert.assertEquals(3, user.getPrincipals().stream().findFirst().get()
            .getRoles().stream().findFirst().get().getRoleJobPlans().size());

        // Call the service the second time to exercise the cache!
        mvcResult = mvc.perform(MockMvcRequestBuilders.get(uri)
        .contentType(MediaType.APPLICATION_JSON_VALUE).params(params)).andReturn();

        status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.OK.value(), status);
        content = mvcResult.getResponse().getContentAsString();

        user = objectMapper.readValue(content, User.class);

        Assert.assertEquals(2, user.getPrincipals().size());
        Assert.assertEquals(3, user.getPrincipals().stream().findFirst().get()
            .getRoles().size());
        Assert.assertEquals(3, user.getPrincipals().stream().findFirst().get()
            .getRoles().stream().findFirst().get().getRoleModules().size());
        Assert.assertEquals(3, user.getPrincipals().stream().findFirst().get()
            .getRoles().stream().findFirst().get().getRoleJobPlans().size());

        // Now confirm the user service was only hit once as the second call hit the cache!
        Assert.assertEquals(1, this.userService.getNumCallsLoadUserByUsername());
    }

    @Test
    @DirtiesContext
    public void test_get_user_success_cache_expires() throws Exception
    {
        String uri = "/rest/user";

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("username", "mockuser");

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get(uri)
            .contentType(MediaType.APPLICATION_JSON_VALUE).params(params)).andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.OK.value(), status);
        String content = mvcResult.getResponse().getContentAsString();

        User user = objectMapper.readValue(content, UserImpl.class);

        Assert.assertEquals(2, user.getPrincipals().size());
        Assert.assertEquals(3, user.getPrincipals().stream().findFirst().get()
            .getRoles().size());
        Assert.assertEquals(3, user.getPrincipals().stream().findFirst().get()
            .getRoles().stream().findFirst().get().getRoleModules().size());
        Assert.assertEquals(3, user.getPrincipals().stream().findFirst().get()
            .getRoles().stream().findFirst().get().getRoleJobPlans().size());

        Thread.sleep(1000);
        // Call the service the second time to exercise the cache!
        mvcResult = mvc.perform(MockMvcRequestBuilders.get(uri)
            .contentType(MediaType.APPLICATION_JSON_VALUE).params(params)).andReturn();

        status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.OK.value(), status);
        content = mvcResult.getResponse().getContentAsString();

        user = objectMapper.readValue(content, User.class);

        Assert.assertEquals(2, user.getPrincipals().size());
        Assert.assertEquals(3, user.getPrincipals().stream().findFirst().get()
            .getRoles().size());
        Assert.assertEquals(3, user.getPrincipals().stream().findFirst().get()
            .getRoles().stream().findFirst().get().getRoleModules().size());
        Assert.assertEquals(3, user.getPrincipals().stream().findFirst().get()
            .getRoles().stream().findFirst().get().getRoleJobPlans().size());

        // Now confirm the user service was only hit once as the second call hit the cache!
        Assert.assertEquals(1, this.userService.getNumCallsLoadUserByUsername());

        Thread.sleep(4100);
        // Call the service the second time to exercise the cache!
        mvcResult = mvc.perform(MockMvcRequestBuilders.get(uri)
            .contentType(MediaType.APPLICATION_JSON_VALUE).params(params)).andReturn();

        Assert.assertEquals(2, this.userService.getNumCallsLoadUserByUsername());
    }

    @Test
    @DirtiesContext
    public void test_get_users_success() throws Exception
    {
        String uri = "/rest/users";

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("username", "mockuser");

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get(uri)
            .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.OK.value(), status);
        String content = mvcResult.getResponse().getContentAsString();

        List<UserImpl> users = objectMapper.readValue(content, new TypeReference<>(){});

        Assert.assertEquals(3, users.size());
        Assert.assertEquals(2, users.get(0).getPrincipals().size());
        Assert.assertEquals(3, users.get(0).getPrincipals().stream().findFirst().get()
            .getRoles().size());
        Assert.assertEquals(3, users.get(0).getPrincipals().stream().findFirst().get()
            .getRoles().stream().findFirst().get().getRoleModules().size());
        Assert.assertEquals(3, users.get(0).getPrincipals().stream().findFirst().get()
            .getRoles().stream().findFirst().get().getRoleJobPlans().size());
    }

}
