package edu.stanford.protege.webprotege.gateway;

import edu.stanford.protege.webprotege.common.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2024-06-14
 */
@RestController
public class FormsController {

    private static final RpcMethod GET_FORM_DESCRIPTORS = new RpcMethod("webprotege.forms.GetProjectFormDescriptors");

    private static final String PROJECT_ID = "projectId";

    private final RpcClient rpcClient;

    public FormsController(RpcClient rpcClient) {
        this.rpcClient = rpcClient;
    }

    @GetMapping("/data/projects/{projectId}/forms")
    public ResponseEntity<Map<String, Object>> getForms(@PathVariable(PROJECT_ID) ProjectId projectId,
                                   @AuthenticationPrincipal Jwt jwt) {
        return rpcClient.call(jwt, GET_FORM_DESCRIPTORS, Map.of(PROJECT_ID, projectId));
    }
}
