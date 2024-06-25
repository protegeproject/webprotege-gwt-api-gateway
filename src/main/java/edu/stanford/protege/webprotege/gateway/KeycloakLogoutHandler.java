package edu.stanford.protege.webprotege.gateway;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Component
public class KeycloakLogoutHandler extends SecurityContextLogoutHandler {

    private static final Logger logger = LoggerFactory.getLogger(KeycloakLogoutHandler.class);
    private final RestTemplate restTemplate;

    public KeycloakLogoutHandler() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response,
                       Authentication auth) {
        logoutFromKeycloak((Jwt) auth.getPrincipal());
        super.logout(request, response,auth);
    }

    private void logoutFromKeycloak(Jwt token) {
        String issuer = token.getClaimAsString("iss");
        String endSessionEndpoint = issuer + "/protocol/openid-connect/logout";

        String accessToken = token.getTokenValue();
        try {
            URI logoutUri = new URI(endSessionEndpoint + "?token=" + accessToken);
            ResponseEntity<String> logoutResponse = restTemplate.getForEntity(logoutUri, String.class);
            if (logoutResponse.getStatusCode().is2xxSuccessful()) {
                logger.info("Successfulley logged out from Keycloak");
            } else {
                logger.error("Could not propagate logout to Keycloak");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}