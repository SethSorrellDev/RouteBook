package com.seth.routebook.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * A single endpoint whose only purpose is to prove whether the supplied
 * Basic auth credentials are valid. POST (not GET) so it falls under the
 * SecurityConfig's "anyRequest().authenticated()" rule rather than the
 * broader GET /api/** permitAll rule - Spring Security never even
 * invokes this method body unless authentication already succeeded;
 * an invalid login gets our standard 401 JSON automatically.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @PostMapping("/verify")
    public Map<String, String> verify(Authentication authentication) {
        return Map.of("username", authentication.getName());
    }
}
