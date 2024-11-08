package org.ikasan.rest.dashboard;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.rest.dashboard.util.TestUserService;
import org.ikasan.security.model.User;
import org.junit.Assert;
import org.junit.Before;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
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

    @Resource
    TestUserService userService;

    @BeforeEach
    @Before
    public void setUp()
    {
        mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }


    @Test
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

        ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        User user = objectMapper.readValue(content, User.class);

        Assert.assertEquals(2, user.getPrincipals().size());
        Assert.assertEquals(3, user.getPrincipals().stream().findFirst().get()
            .getRoles().size());
        Assert.assertEquals(3, user.getPrincipals().stream().findFirst().get()
            .getRoles().stream().findFirst().get().getRoleModules().size());
        Assert.assertEquals(3, user.getPrincipals().stream().findFirst().get()
            .getRoles().stream().findFirst().get().getRoleJobPlans().size());
    }

    @Test
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

        ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        List<User> users = objectMapper.readValue(content, new TypeReference<List<User>>(){});

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
