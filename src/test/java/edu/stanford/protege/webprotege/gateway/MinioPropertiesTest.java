package edu.stanford.protege.webprotege.gateway;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2024-06-10
 */
@SpringBootTest(classes = WebprotegeGwtApiGatewayApplication.class)
@Import({MockJwtDecoderConfiguration.class})
@ExtendWith(IntegrationTestsExtension.class)
public class MinioPropertiesTest {

    @Autowired
    private MinioProperties minioProperties;

    @Test
    void shouldHaveBucketName() {
        assertThat(minioProperties.getBucketName()).isEqualTo("foobucket");
    }
}
