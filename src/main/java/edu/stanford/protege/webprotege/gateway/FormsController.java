package edu.stanford.protege.webprotege.gateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.stanford.protege.webprotege.common.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2024-06-14
 */
@RestController
public class FormsController {

    private static final RpcMethod GET_FORM_DESCRIPTORS = new RpcMethod("webprotege.forms.GetProjectFormDescriptors");

    private static final RpcMethod SET_FORMS = new RpcMethod("webprotege.forms.SetProjectForms");


    private static final String PROJECT_ID = "projectId";

    private final RpcClient rpcClient;

    private final ObjectMapper objectMapper;

    public FormsController(RpcClient rpcClient, ObjectMapper objectMapper) {
        this.rpcClient = rpcClient;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/data/projects/{projectId}/forms")
    public ResponseEntity<Map<String, Object>> getForms(@PathVariable(PROJECT_ID) ProjectId projectId,
                                   @AuthenticationPrincipal Jwt jwt) {
        try {
            CorrelationMDCUtil.setCorrelationId(UUID.randomUUID().toString());
            return rpcClient.call(jwt, GET_FORM_DESCRIPTORS, Map.of(PROJECT_ID, projectId));
        } finally {
            CorrelationMDCUtil.clearCorrelationId();
        }
    }

    @PostMapping("/data/projects/{projectId}/forms")
    public ResponseEntity<Map<String, Object>> setForms(@PathVariable(PROJECT_ID) ProjectId projectId,
                                         @RequestBody String forms,
                                         @AuthenticationPrincipal Jwt jwt) {
        // changeRequestId
        // projectId,
        // formDescriptors
        // formSelectors
        try {
            var tree = objectMapper.readValue(forms, new TypeReference<Map<String, Object>>() {});
            var params = new LinkedHashMap<>(tree);
            params.put("changeRequestId", ChangeRequestId.generate());
            params.put("projectId", projectId);
            return rpcClient.call(jwt, SET_FORMS, params);

        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}
