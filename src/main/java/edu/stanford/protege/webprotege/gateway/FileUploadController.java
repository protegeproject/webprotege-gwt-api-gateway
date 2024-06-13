package edu.stanford.protege.webprotege.gateway;

import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2024-06-10
 */
@RestController
public class FileUploadController {

    private final Logger logger = LoggerFactory.getLogger(GatewayController.class);

    private final FileStorageService fileStorageService;

    public FileUploadController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @PostMapping(path = "/files/submit")
    public FileSubmissionId execute(@RequestParam MultipartFile file,
                                    @AuthenticationPrincipal Jwt principal) throws java.io.IOException {
        var userId = principal.getClaimAsString("preferred_username");
        logger.info("Received a multipart file from {} with a size of {} bytes", userId, file.getSize());
        var tempFile = Files.createTempFile("webprotege-file-upload", null);
        file.transferTo(tempFile);
        return fileStorageService.storeFile(tempFile);
    }
}
