package org.ikasan.rest.dashboard;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith (SpringJUnit4ClassRunner.class)
public class JwtAuthenticationEntryPointTest
{

    private JwtAuthenticationEntryPoint uut;

    @Mock
    HttpServletRequest request;

    @Mock
    HttpServletResponse response;


    @Before
    public void setup(){
        uut = new JwtAuthenticationEntryPoint();
    }

    @Test
    public void test() throws IOException
    {

        uut.commence(request, response, new AuthenticationServiceException("Test"));

        verify(response).setContentType("application/json;charset=UTF-8");
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verifyNoMoreInteractions(request,response);

    }

}
