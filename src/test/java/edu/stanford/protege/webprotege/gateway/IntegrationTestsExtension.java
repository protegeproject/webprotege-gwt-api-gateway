package edu.stanford.protege.webprotege.gateway;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2023-05-23
 */
public class IntegrationTestsExtension implements BeforeAllCallback, AfterAllCallback {

    private static Logger logger = LoggerFactory.getLogger(IntegrationTestsExtension.class);

    private RabbitMQContainer rabbitContainer;

    private KeycloakContainer keycloakContainer;


    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        var imageName = DockerImageName.parse("rabbitmq:3.7.25-management-alpine");
        rabbitContainer = new RabbitMQContainer(imageName)
                .withExposedPorts(5672)
                .withUser("guest", "guest");
        rabbitContainer.start();

        logger.info("RabbitMQ started on port {}", rabbitContainer.getAmqpPort());

        System.setProperty("spring.rabbitmq.host", rabbitContainer.getHost());
        System.setProperty("spring.rabbitmq.port", String.valueOf(rabbitContainer.getAmqpPort()));

        keycloakContainer = new KeycloakContainer()
                .withRealmImportFile("keycloak/realm-export.json")
                .withStartupTimeout(Duration.ofSeconds(60)) // Increase startup timeout if needed
                .waitingFor(
                        Wait.forLogMessage(".*Server startup complete.*\\n", 1)
                );

        keycloakContainer.start();

        System.setProperty("spring.security.oauth2.client.registration.keycloak.redirectUri", keycloakContainer.getAuthServerUrl() + "/realms/webprotege");
        System.setProperty("spring.security.oauth2.client.provider.keycloak.authorizationUri", keycloakContainer.getAuthServerUrl() + "/realms/webprotege/protocol/openid-connect/token");
        System.setProperty("spring.security.oauth2.client.provider.keycloak.tokenUri", keycloakContainer.getAuthServerUrl() + "/realms/webprotege/protocol/openid-connect/token");
        System.setProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri", keycloakContainer.getAuthServerUrl() + "/realms/webprotege");
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) throws Exception {
        if (rabbitContainer != null) {
            rabbitContainer.stop();
        } if (keycloakContainer != null) {
            keycloakContainer.stop();
        }
    }
}
