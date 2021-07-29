package edu.stanford.protege.webprotege.gateway;

import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2021-07-21
 */
@Component
public class MessageIdFactory {

    public String createCorrelationId() {
            return UUID.randomUUID().toString();
        }
}
