package com.fiap.video_processor.infrastructure.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import software.amazon.awssdk.services.sqs.SqsClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = AwsSqsConfig.class)
class AwsSqsConfigCoverageTest {

    @Autowired
    private SqsClient sqsClient;

    @Test
    void shouldCreateSqsClientBean() {
        assertThat(sqsClient).isNotNull();
    }
}