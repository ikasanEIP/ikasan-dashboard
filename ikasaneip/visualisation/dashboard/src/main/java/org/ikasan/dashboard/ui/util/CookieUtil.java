package org.ikasan.dashboard.ui.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.vaadin.flow.server.VaadinRequest;
import jakarta.servlet.http.Cookie;

public class CookieUtil {
    private static final Logger logger = LoggerFactory.getLogger(CookieUtil.class);

    public static Cookie getCookieByName(String name, VaadinRequest request) {
        // Fetch all cookies from the request
        Cookie[] cookies = request.getCookies();

        // Iterate to find cookie by its name
        try {
            for (Cookie cookie : cookies) {
                if (name.equals(cookie.getName())) {
                    return cookie;
                }
            }
        } catch (NullPointerException e) {
            logger.debug("No cookies available to search", e);
        }
        return null;
    }

}