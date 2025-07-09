package com.fiap.video_processor.usecase.processor.impl;

import com.fiap.video_processor.infrastructure.enums.ProcessorStatus;
import com.fiap.video_processor.infrastructure.repository.JpaVideoFrameRepository;
import com.fiap.video_processor.infrastructure.repository.S3Repository;
import com.fiap.video_processor.usecase.email.EmailService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.util.FileSystemUtils;

import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static org.mockito.Mockito.*;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class VideoProcessorImplTest {

    @Mock
    private S3Repository s3Repository;

    @Mock
    private JpaVideoFrameRepository frameRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private VideoProcessorImpl videoProcessor;

    private Path mockVideoPath = Paths.get("tempvideo/mock.mp4");

    @BeforeEach
    void setup() throws Exception {
        Files.createDirectories(Paths.get("uploads"));
        Files.createDirectories(Paths.get("outputs"));
        Files.createDirectories(Paths.get("tempframes"));
        Files.createDirectories(Paths.get("tempvideo"));

        mockVideoPath = Files.createTempFile("mock_", ".mp4");
    }

    @AfterEach
    void tearDown() throws Exception {
        Files.deleteIfExists(mockVideoPath);
    }

    @Test
    void deveProcessarVideoComSucesso() throws Exception {
        String videoId = "video123";
        String email = "teste@email.com";

        Path workingDir = Files.createTempDirectory("frames_test");
        Path workingVideoDir = Files.createTempDirectory("videotemp_test");

        when(s3Repository.downloadToTempFile(eq(videoId), any(Path.class))).thenReturn(mockVideoPath);

        Path fakeFrame = Files.createTempFile(workingDir, "frame_", ".png");

        try (MockedStatic<Files> filesMock = mockStatic(Files.class, CALLS_REAL_METHODS)) {
            filesMock.when(() -> Files.createDirectories(any(Path.class))).thenReturn(null);
            filesMock.when(() -> Files.list(any(Path.class))).thenReturn(Stream.of(fakeFrame));
            filesMock.when(() -> Files.copy(eq(fakeFrame), any(OutputStream.class))).then(invocation -> {
                OutputStream os = invocation.getArgument(1);
                os.write("frame".getBytes());
                return null;
            });

            Process mockProcess = mock(Process.class);
            when(mockProcess.getInputStream()).thenReturn(new ByteArrayInputStream("".getBytes()));
            when(mockProcess.waitFor()).thenReturn(0);

            try (MockedConstruction<ProcessBuilder> pbMock = mockConstruction(ProcessBuilder.class,
                    (pb, context) -> {
                        when(pb.redirectErrorStream(true)).thenReturn(pb);
                        when(pb.start()).thenReturn(mockProcess);
                    })) {

                videoProcessor.processVideoFromS3(videoId, email);

                verify(frameRepository, atLeastOnce()).save(argThat(entity ->
                        entity.getStatus() == ProcessorStatus.PROCESSING ||
                                entity.getStatus() == ProcessorStatus.SUCCESS));

                verify(emailService).sendEmail(eq(email), any(), contains(videoId));
            }
        }

        FileSystemUtils.deleteRecursively(workingDir);
        FileSystemUtils.deleteRecursively(workingVideoDir);
    }

    @Test
    void deveRegistrarErroQuandoFfmpegFalha() throws Exception {
        String videoId = "failVideo";
        String email = "fail@email.com";

        when(s3Repository.downloadToTempFile(eq(videoId), any(Path.class))).thenReturn(mockVideoPath);

        Process mockProcess = mock(Process.class);
        when(mockProcess.getInputStream()).thenReturn(new ByteArrayInputStream("Erro".getBytes()));
        when(mockProcess.waitFor()).thenReturn(1);

        try (MockedConstruction<ProcessBuilder> pbMock = mockConstruction(ProcessBuilder.class,
                (pb, context) -> {
                    when(pb.redirectErrorStream(anyBoolean())).thenReturn(pb);
                    when(pb.start()).thenReturn(mockProcess);
                })) {

            videoProcessor.processVideoFromS3(videoId, email);

            verify(frameRepository, atLeastOnce()).save(argThat(entity ->
                    entity.getStatus() == ProcessorStatus.PROCESSING ||
                            entity.getStatus() == ProcessorStatus.ERROR));

            verify(emailService).sendEmail(eq(email), any(), contains(videoId));
        }
    }
}
