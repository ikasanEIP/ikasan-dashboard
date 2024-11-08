package org.ikasan.dashboard.ui.security.view;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class IkasanErrorController implements ErrorController
{
 
    @RequestMapping("/error")
    public String handleError(HttpServletRequest request) {
        //do something like logging
        return "login";
    }
}