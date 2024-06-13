package edu.stanford.protege.webprotege.gateway;

import org.junit.jupiter.api.extension.*;
import org.slf4j.*;
import org.testcontainers.containers.MinIOContainer;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2024-06-12
 */
public class MinioTestExtension  implements BeforeAllCallback, AfterAllCallback {

    private static Logger logger = LoggerFactory.getLogger(MinioTestExtension.class);

    private MinIOContainer container;

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        container = new MinIOContainer("minio/minio:RELEASE.2024-04-06T05-26-02Z");
        container.start();

        var mappedHttpPort = container.getMappedPort(9000);
        logger.info("MinIO port 9000 is mapped to {}", mappedHttpPort);
        System.setProperty("webprotege.minio.endPoint", "http://localhost:" + mappedHttpPort);
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) throws Exception {
        if(container != null) {
            container.stop();
        }
    }
}
