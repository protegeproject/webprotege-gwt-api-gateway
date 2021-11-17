package edu.stanford.protege.webprotege.gateway;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.stereotype.Component;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2021-11-17
 */
@Component
@ConfigurationProperties(prefix = "webprotege.gateway")
@ConstructorBinding
public record GatewayProperties(long timeout, String replyChannel) {

}
