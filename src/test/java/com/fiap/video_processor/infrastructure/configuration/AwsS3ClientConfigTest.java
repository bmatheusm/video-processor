package com.fiap.video_processor.infrastructure.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import software.amazon.awssdk.services.s3.S3Client;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = AwsS3ClientConfig.class)
class AwsS3ClientConfigCoverageTest {

    @Autowired
    private S3Client s3Client;

    @Test
    void shouldCreateS3ClientBean() {
        assertThat(s3Client).isNotNull();
    }
}