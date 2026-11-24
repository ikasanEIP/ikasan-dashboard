package org.ikasan.dashboard.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class IkasanAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger logger = LoggerFactory.getLogger(IkasanAuthenticationEntryPoint.class);

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authenticationException)
            throws IOException, ServletException {
        // Remember where the user was going so LoginView can take them there after login. Only page
        // navigations count: the login page's own assets (stylesheets, the service worker precache, ...)
        // are also unauthenticated requests, and caching those overwrote the real deep link.
        if(isPageNavigation(request)) {
            String context = request.getRequestURI();
            if(context.startsWith("/")) {
                context = context.substring(1);
            }

            ContextCache.addContext(request.getSession().getId(), context);
            logger.debug("Cached deep link [{}] for session [{}]", context, request.getSession().getId());
        }

        if(!SecurityUtils.isUserLoggedIn()) {
            response.sendRedirect("/login");
        }
        else {
            response.sendRedirect("/");
        }
    }

    /**
     * Is this the browser loading a page (as opposed to a script, stylesheet, image or XHR)? Browsers send
     * Sec-Fetch-Dest: document for page loads; without that header, fall back to the request accepting html.
     *
     * @param request the unauthenticated request
     * @return true for a page navigation
     */
    static boolean isPageNavigation(HttpServletRequest request) {
        String fetchDest = request.getHeader("Sec-Fetch-Dest");
        if(fetchDest != null) {
            return fetchDest.equals("document");
        }
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains("text/html");
    }
}
