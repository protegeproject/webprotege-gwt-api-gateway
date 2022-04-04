package edu.stanford.protege.webprotege.gateway;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2022-03-29
 */
public record Msg(byte [] payload, Map<String, String> headers) {

    public static Msg withHeader(String key, String value) {
        return new Msg(new byte[0], Map.of(key, value));
    }

    public static Msg withPayload(String s) {
        return new Msg(s.getBytes(StandardCharsets.UTF_8), Map.of());
    }
}
