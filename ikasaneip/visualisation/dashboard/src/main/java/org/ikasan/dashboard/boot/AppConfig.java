package org.ikasan.dashboard.boot;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.spring.SpringVaadinSession;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

@Configuration
class AppConfig {
    @Bean
    @Order(Integer.MIN_VALUE)
    FilterRegistrationBean<SessionSerialisationFilter> sessionSerializationDebugToolFilter() {
        return new FilterRegistrationBean<>(new SessionSerialisationFilter());
    }

    public class SessionSerialisationFilter implements Filter {

        @Override
        public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
            try {
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(new ByteArrayOutputStream());
                if(((HttpServletRequest)servletRequest).getSession() != null) {
                    HttpSession session = ((HttpServletRequest)servletRequest).getSession();
                    VaadinSession vaadinSession  = (SpringVaadinSession)session.getAttribute("com.vaadin.flow.server.VaadinSession.springServlet");
                    objectOutputStream.writeObject(vaadinSession);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            filterChain.doFilter(servletRequest, servletResponse);
        }
    }
}

