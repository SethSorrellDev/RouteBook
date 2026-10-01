package com.seth.routebook.support;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;

/**
 * Stand-ins for identity-service tokens. The jwt() post-processor injects an
 * already-authenticated principal, so signature verification is skipped here
 * (and covered by the end-to-end check against the real service). It also
 * bypasses our JwtAuthenticationConverter, so authorities are set explicitly.
 */
public final class TestAuth {

    private TestAuth() {}

    /** A valid user whose subject is on the admin allowlist. */
    public static JwtRequestPostProcessor adminJwt() {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(j -> j.subject("test-admin-subject").claim("type", "access"))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    /** A valid, authenticated user who is NOT on the allowlist. */
    public static JwtRequestPostProcessor nonAdminJwt() {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(j -> j.subject("some-other-user").claim("type", "access"));
    }
}
