package org.ikasan.rest.dashboard;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.ikasan.spec.security.model.User;
import org.ikasan.spec.security.service.UserService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.util.ArrayList;

import static org.mockito.Mockito.*;

@RunWith (SpringJUnit4ClassRunner.class)
public class JwtRequestFilterTest
{

    private JwtRequestFilter uut;

    @Mock
    UserService userService;

    @Mock
    JwtTokenUtil jwtTokenUtil;

    @Mock
    HttpServletRequest request;

    @Mock
    HttpServletResponse response;

    @Mock
    FilterChain chain;

    @Mock
    User userDetails;

    @Mock
    SecurityContextRepository securityContextRepository;

    @Before
    public void setup(){
        uut = new JwtRequestFilter(userService,jwtTokenUtil, securityContextRepository, 5);
    }

    @After
    public void reset_mocks() {
        SecurityContextHolder.getContext().setAuthentication(null);
    }

    @Test
    @DirtiesContext
    public void test_login_page() throws ServletException, IOException
    {

        when(request.getServletPath()).thenReturn("/login");

        uut.doFilterInternal(request,response,chain);

        verify(request).getServletPath();
        verify(chain).doFilter(request,response);

        verifyNoMoreInteractions(request,chain);

    }

    @Test
    @DirtiesContext
    public void test_rest_call() throws ServletException, IOException
    {

        when(request.getServletPath()).thenReturn("/rest");
        when(request.getHeader("Authorization")).thenReturn("Bearer test.token");
        when(jwtTokenUtil.getUsernameFromToken("test.token")).thenReturn("testUser");
        when(userService.loadUserByUsername("testUser")).thenReturn(userDetails);
        when(jwtTokenUtil.validateToken("test.token",userDetails)).thenReturn(true);
        when(userDetails.getAuthorities()).thenReturn(new ArrayList<>());

        uut.doFilterInternal(request,response,chain);

        verify(request).getServletPath();
        verify(request).getHeader("Authorization");
        verify(jwtTokenUtil).getUsernameFromToken("test.token");
        verify(userService).loadUserByUsername("testUser");
        verify(jwtTokenUtil).validateToken("test.token",userDetails);
        verify(userDetails).getAuthorities();

        verify(chain).doFilter(request,response);

        verifyNoMoreInteractions(chain,jwtTokenUtil,userService,userDetails,response);

    }

    @Test
    @DirtiesContext
    public void test_rest_call_cache() throws ServletException, IOException {

        when(request.getServletPath()).thenReturn("/rest");
        when(request.getHeader("Authorization")).thenReturn("Bearer test.token");
        when(jwtTokenUtil.getUsernameFromToken("test.token")).thenReturn("testUser");
        when(userService.loadUserByUsername("testUser")).thenReturn(userDetails);
        when(jwtTokenUtil.validateToken("test.token",userDetails)).thenReturn(true);
        when(userDetails.getAuthorities()).thenReturn(new ArrayList<>());

        uut.doFilterInternal(request,response,chain);

        // We need to remove the security context in order to force full authentication in the second call.
        SecurityContextHolder.getContext().setAuthentication(null);
        uut.doFilterInternal(request,response,chain);

        verify(request, times(2)).getServletPath();
        verify(request, times(2)).getHeader("Authorization");
        verify(jwtTokenUtil, times(2)).getUsernameFromToken("test.token");
        // The user service is only hit once due to the cache!
        verify(userService, times(1)).loadUserByUsername("testUser");
        verify(jwtTokenUtil, times(2)).validateToken("test.token",userDetails);
        verify(userDetails, times(2)).getAuthorities();

        verify(chain, times(2)).doFilter(request,response);

        verifyNoMoreInteractions(chain,jwtTokenUtil,userService,userDetails,response);
    }

    @Test
    @DirtiesContext
    public void test_rest_call_cache_expires() throws ServletException, IOException, InterruptedException {
        when(request.getServletPath()).thenReturn("/rest");
        when(request.getHeader("Authorization")).thenReturn("Bearer test.token");
        when(jwtTokenUtil.getUsernameFromToken("test.token")).thenReturn("testUser");
        when(userService.loadUserByUsername("testUser")).thenReturn(userDetails);
        when(jwtTokenUtil.validateToken("test.token",userDetails)).thenReturn(true);
        when(userDetails.getAuthorities()).thenReturn(new ArrayList<>());

        uut.doFilterInternal(request,response,chain);

        Thread.sleep(1000);
        uut.doFilterInternal(request,response,chain);

        // Cache should have expired here and the user to be re-added to the cache
        Thread.sleep(4100);
        uut.doFilterInternal(request,response,chain);

        // We need to remove the security context in order to force full authentication in the second call.
        SecurityContextHolder.getContext().setAuthentication(null);
        uut.doFilterInternal(request,response,chain);

        verify(request, times(4)).getServletPath();
        verify(request, times(4)).getHeader("Authorization");
        verify(jwtTokenUtil, times(4)).getUsernameFromToken("test.token");
        // The user service is hit twice now due to the cached user expiring!
        verify(userService, times(2)).loadUserByUsername("testUser");
        verify(jwtTokenUtil, times(2)).validateToken("test.token",userDetails);
        verify(userDetails, times(2)).getAuthorities();

        verify(chain, times(4)).doFilter(request,response);

        verifyNoMoreInteractions(chain,jwtTokenUtil,userService,userDetails,response);
    }

    @Test
    @DirtiesContext
    public void test_rest_call_when_validate_token_is_false() throws ServletException, IOException
    {

        when(request.getServletPath()).thenReturn("/rest");
        when(request.getHeader("Authorization")).thenReturn("Bearer test.token");
        when(jwtTokenUtil.getUsernameFromToken("test.token")).thenReturn("testUser");
        when(userService.loadUserByUsername("testUser")).thenReturn(userDetails);
        when(jwtTokenUtil.validateToken("test.token",userDetails)).thenReturn(false);

        uut.doFilterInternal(request,response,chain);

        verify(request).getServletPath();
        verify(request).getHeader("Authorization");
        verify(jwtTokenUtil).getUsernameFromToken("test.token");
        verify(userService).loadUserByUsername("testUser");
        verify(jwtTokenUtil).validateToken("test.token",userDetails);

        verify(chain).doFilter(request,response);

        verifyNoMoreInteractions(chain,jwtTokenUtil,userService,userDetails,response);

    }

    @Test
    @DirtiesContext
    public void test_rest_call_when_userService_returns_null() throws ServletException, IOException
    {

        when(request.getServletPath()).thenReturn("/rest");
        when(request.getHeader("Authorization")).thenReturn("Bearer test.token");
        when(jwtTokenUtil.getUsernameFromToken("test.token")).thenReturn("testUser");
        when(userService.loadUserByUsername("testUser")).thenReturn(null);

        uut.doFilterInternal(request,response,chain);

        verify(request).getServletPath();
        verify(request).getHeader("Authorization");
        verify(jwtTokenUtil).getUsernameFromToken("test.token");
        verify(userService).loadUserByUsername("testUser");

        verify(chain).doFilter(request,response);

        verifyNoMoreInteractions(chain,jwtTokenUtil,userService,userDetails,response);
    }

    @Test
    @DirtiesContext
    public void test_rest_call_when_Authorization_does_not_have_bearer() throws ServletException, IOException
    {

        when(request.getServletPath()).thenReturn("/rest");
        when(request.getHeader("Authorization")).thenReturn("Basic test.token");

        uut.doFilterInternal(request,response,chain);

        verify(request).getServletPath();
        verify(request).getHeader("Authorization");

        verify(chain).doFilter(request,response);

        verifyNoMoreInteractions(chain,jwtTokenUtil,userService,userDetails,response);

    }

    @Test
    @DirtiesContext
    public void test_rest_call_when_jwt_throws_ExpiredJwtException() throws ServletException, IOException
    {

        when(request.getServletPath()).thenReturn("/rest");
        when(request.getHeader("Authorization")).thenReturn("Bearer test.token");
        when(jwtTokenUtil.getUsernameFromToken("test.token")).thenThrow( new ExpiredJwtException(null,null,"Test"));

        uut.doFilterInternal(request,response,chain);

        verify(request).getServletPath();
        verify(request).getHeader("Authorization");
        verify(jwtTokenUtil).getUsernameFromToken("test.token");
        verify(chain).doFilter(request,response);

        verifyNoMoreInteractions(chain,jwtTokenUtil,userService,userDetails,response);

    }

    @Test
    @DirtiesContext
    public void test_rest_call_when_jwt_throws_IllegalArgumentException() throws ServletException, IOException
    {

        when(request.getServletPath()).thenReturn("/rest");
        when(request.getHeader("Authorization")).thenReturn("Bearer test.token");
        when(jwtTokenUtil.getUsernameFromToken("test.token")).thenThrow( new IllegalArgumentException("Test"));

        uut.doFilterInternal(request,response,chain);

        verify(request).getServletPath();
        verify(request).getHeader("Authorization");
        verify(jwtTokenUtil).getUsernameFromToken("test.token");
        verify(chain).doFilter(request,response);

        verifyNoMoreInteractions(chain,jwtTokenUtil,userService,userDetails,response);

    }

}
