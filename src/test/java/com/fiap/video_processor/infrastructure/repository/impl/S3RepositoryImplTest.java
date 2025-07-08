package com.fiap.video_processor.infrastructure.repository.impl;

import com.fiap.video_processor.util.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.*;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class S3RepositoryImplTest {

    @Mock
    private S3Client s3Client;

    @InjectMocks
    private S3RepositoryImpl repository;

    private final String bucket = "meu-bucket";

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        repository = new S3RepositoryImpl(s3Client);

        TestUtils.setField(repository, "bucket", bucket);
    }

    @Test
    void deveBaixarVideoQuandoDiretorioNaoExiste(@TempDir Path tempDir) throws IOException {
        // Arrange
        String videoId = "video123";
        Path targetFile = tempDir.resolve(videoId);
        Files.deleteIfExists(tempDir);

        // Act
        Path result = repository.downloadToTempFile(videoId, tempDir);

        // Assert
        assertEquals(targetFile, result);

        ArgumentCaptor<GetObjectRequest> requestCaptor = ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(s3Client).getObject(requestCaptor.capture(), any(ResponseTransformer.class));

        GetObjectRequest req = requestCaptor.getValue();
        assertEquals(bucket, req.bucket());
        assertEquals(videoId, req.key());
    }

    @Test
    void deveBaixarVideoQuandoDiretorioJaExiste(@TempDir Path tempDir) throws IOException {
        // Arrange
        String videoId = UUID.randomUUID().toString();
        Files.createDirectories(tempDir);

        // Act
        Path result = repository.downloadToTempFile(videoId, tempDir);

        // Assert
        assertTrue(Files.exists(result.getParent()));
        assertEquals(tempDir.resolve(videoId), result);

        verify(s3Client).getObject(any(GetObjectRequest.class), any(ResponseTransformer.class));
    }

    @Test
    void devePropagarIOExceptionQuandoFalhaEmCriarDiretorio(@TempDir Path tempDir) {
        // Arrange
        String videoId = "video123";

        try (MockedStatic<Files> files = mockStatic(Files.class)) {
            files.when(() -> Files.notExists(tempDir)).thenReturn(true);
            files.when(() -> Files.createDirectories(tempDir)).thenThrow(new IOException("falha"));

            // Act + Assert
            IOException thrown = assertThrows(IOException.class, () ->
                    repository.downloadToTempFile(videoId, tempDir)
            );

            assertEquals("falha", thrown.getMessage());
            verify(s3Client, never()).getObject((GetObjectRequest) any(), (ResponseTransformer<GetObjectResponse, Object>) any());
        }
    }

}
