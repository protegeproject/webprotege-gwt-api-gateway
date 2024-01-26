package edu.stanford.protege.webprotege.gateway;

import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Component;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2021-07-19
 */
@EnableWebSecurity
@Component
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // TODO:  This needs setting up correctly
        return http.csrf(AbstractHttpConfigurer::disable).authorizeHttpRequests((requests) -> requests
                .requestMatchers("/api/execute").permitAll()
                .anyRequest().anonymous()
        ).build();
    }
}
