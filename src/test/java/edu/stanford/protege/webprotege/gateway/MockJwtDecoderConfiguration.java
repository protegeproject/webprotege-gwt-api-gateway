package edu.stanford.protege.webprotege.gateway;

import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2021-12-06
 */
@org.springframework.boot.test.context.TestConfiguration
public class MockJwtDecoderConfiguration {

    @Bean
    JwtDecoder jwtDecoder() {
        return s -> null;
    }
}
