package edu.stanford.protege.webprotege.gateway;

import io.minio.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.nio.file.Files;

import static org.assertj.core.api.Assertions.*;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2024-06-12
 */

@ExtendWith(MinioTestExtension.class)
@ActiveProfiles("test")
public class FileStorageServiceIntegrationTest {

    private FileStorageService storageService;

    private MinioClient client;

    @BeforeEach
    void setUp() {
        var properties = new MinioProperties();
        properties.setBucketName("webprotege-uploads");
        properties.setSecretKey("minioadmin");
        properties.setAccessKey("minioadmin");
        properties.setEndPoint(System.getProperty("webprotege.minio.endPoint"));
        client = MinioClient.builder()
                                   .credentials(properties.getAccessKey(), properties.getSecretKey())
                                   .endpoint(properties.getEndPoint())
                                   .build();
        storageService = new FileStorageService(client, properties);
    }

    @Test
    public void testStoreFile() throws Exception {
        // Setup a temporary file
        var tempFile = Files.createTempFile("test-file-", ".txt");
        Files.write(tempFile, "This is a test file".getBytes());

        // Test storing the file
        var fileSubmissionId = storageService.storeFile(tempFile);
        assertThat(fileSubmissionId).isNotNull();

        // Cleanup
        Files.deleteIfExists(tempFile);

        var exists = client.bucketExists(BucketExistsArgs.builder().bucket("webprotege-uploads").build());
        assertThat(exists).isTrue();
    }

    @Test
    public void testStoreFileWhenMinioIsDown() throws Exception {
        // Simulate Minio down by incorrect port or server address
        storageService = new FileStorageService(MinioClient.builder()
                                                       .endpoint("http://localhost:9999")
                                                       .credentials("minioadmin", "minioadmin")
                                                       .build(),
                                            new MinioProperties());

        var tempFile = Files.createTempFile("test-file-", ".txt");
        Files.write(tempFile, "Content".getBytes());

        // Expect an exception since Minio is down
        assertThatThrownBy(() -> storageService.storeFile(tempFile))
                .isInstanceOf(StorageException.class);

        // Cleanup
        Files.deleteIfExists(tempFile);
    }
}
