package org.ikasan.rest.dashboard;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.ikasan.security.service.UserService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class JwtRequestFilter extends OncePerRequestFilter
{
    private UserService userService;

    private JwtTokenUtil jwtTokenUtil;

    private SecurityContextRepository securityContextRepository;

    private Cache<String, UserDetails> cache;

    public JwtRequestFilter(UserService userService, JwtTokenUtil jwtTokenUtil
        , SecurityContextRepository securityContextRepository, int userCacheTimeoutSeconds)
    {
        this.userService = userService;
        this.jwtTokenUtil = jwtTokenUtil;
        this.securityContextRepository = securityContextRepository;
        this.cache = CacheBuilder.newBuilder()
            .expireAfterAccess(userCacheTimeoutSeconds, TimeUnit.SECONDS)
            .build();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException
    {
        final String requestServletUrl = request.getServletPath();
        if (requestServletUrl.startsWith("/rest"))
        {
            final String requestTokenHeader = request.getHeader("Authorization");
            String jwtToken = null;
            // JWT Token is in the form "Bearer token". Remove Bearer word and get
            // only the Token
            if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer "))
            {
                jwtToken = requestTokenHeader.substring(7);
                try
                {
                    String username = jwtTokenUtil.getUsernameFromToken(jwtToken);

                    // Once we get the token validate it.
                    if (username != null && SecurityContextHolder.getContext().getAuthentication() == null)
                    {
                        UserDetails userDetails = cache.getIfPresent(username);

                        if(userDetails == null) {
                            userDetails = this.userService.loadUserByUsername(username);
                            cache.put(username, userDetails);
                        }
                        // if token is valid configure Spring Security to manually set
                        // authentication
                        if (jwtTokenUtil.validateToken(jwtToken, userDetails))
                        {
                            UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                            usernamePasswordAuthenticationToken
                                .setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                            // After setting the Authentication in the context, we specify
                            // that the current user is authenticated. So it passes the
                            // Spring Security Configurations successfully.
                            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
                            securityContext.setAuthentication(usernamePasswordAuthenticationToken);
                            SecurityContextHolder.setContext(securityContext);

                            securityContextRepository.saveContext(securityContext, request, response);
                        }
                    }
                }
                catch (IllegalArgumentException e)
                {
                    logger.warn("Unable to get JWT Token by [" + requestServletUrl + "]");

                }
                catch (ExpiredJwtException e)
                {
                    logger.warn("JWT Token has expired called by [" + requestServletUrl + "]");
                }
            }
            else
            {
                logger.warn(
                    "[Authorization] header does not begin with Bearer String on url [" + requestServletUrl + "]");
            }
        }
        chain.doFilter(request, response);
    }
}
