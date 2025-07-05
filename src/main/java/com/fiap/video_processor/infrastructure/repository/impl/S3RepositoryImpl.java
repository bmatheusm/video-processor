package com.fiap.video_processor.infrastructure.repository.impl;

import com.fiap.video_processor.infrastructure.repository.S3Repository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class S3RepositoryImpl implements S3Repository {

    private final S3Client s3;

    @Value("${s3.bucket}")
    private String bucket;

    public S3RepositoryImpl(S3Client s3) {
        this.s3 = s3;
    }

    @Override
    public Path downloadToTempFile(String videoId, Path tempDir) throws IOException {
        if (Files.notExists(tempDir)) {
            Files.createDirectories(tempDir);
        }

        Path tempFile = tempDir.resolve(videoId);

        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucket)
                .key(videoId)
                .build();

        s3.getObject(request, ResponseTransformer.toFile(tempFile));

        return tempFile;
    }
}
