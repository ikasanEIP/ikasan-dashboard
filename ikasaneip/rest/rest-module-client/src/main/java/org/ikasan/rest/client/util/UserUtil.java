package org.ikasan.rest.client.util;

import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.security.Principal;

public class UserUtil
{
    public static String getUser()
    {
        String user = "unknown";
        SecurityContext context = SecurityContextHolder.getContext();
        if (context != null)
        {
            user = ((Principal)context.getAuthentication().getPrincipal()).getName().toString();
        }
        return user;
    }

}
