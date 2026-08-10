package com.seth.routebook.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Returns a clean JSON 401 instead of Spring Security's default, which
 * sends a WWW-Authenticate: Basic header that makes Safari (and some
 * other browsers) pop up their own native credential dialog - a
 * conflict with our own React login form, discovered the hard way on
 * an earlier project. Omitting that header avoids it entirely.
 */
@Component
public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public ApiAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                          AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        ErrorResponse body = new ErrorResponse(401, "Authentication required for this operation");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
