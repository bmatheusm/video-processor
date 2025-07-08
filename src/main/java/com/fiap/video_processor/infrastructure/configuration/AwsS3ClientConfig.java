package com.fiap.video_processor.infrastructure.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class AwsS3ClientConfig {

    @Bean
    public S3Client s3Client() {
        AwsCredentialsProvider credentials = EnvironmentVariableCredentialsProvider.create();

        return S3Client.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(credentials)
                .build();
    }
}
