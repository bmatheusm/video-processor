package com.fiap.video_processor.usecase.processor.impl;

import com.fiap.video_processor.domain.entities.VideoFrameEntity;
import com.fiap.video_processor.domain.exceptions.VideoProcessingException;
import com.fiap.video_processor.infrastructure.enums.ProcessorStatus;
import com.fiap.video_processor.infrastructure.repository.JpaVideoFrameRepository;
import com.fiap.video_processor.infrastructure.repository.S3Repository;
import com.fiap.video_processor.usecase.email.EmailService;
import com.fiap.video_processor.usecase.processor.VideoProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
public class VideoProcessorImpl implements VideoProcessor {

    private final Path uploadsDir = Paths.get("uploads");
    private final Path outputsDir = Paths.get("outputs");
    private final Path tempDir = Paths.get("tempframes");
    private final Path tempVideoDir = Paths.get("tempvideo");


    @Autowired
    private S3Repository s3Repository;

    @Autowired
    private JpaVideoFrameRepository frameRepository;

    @Autowired
    private EmailService emailService;

    public VideoProcessorImpl() throws IOException {
        Files.createDirectories(uploadsDir);
        Files.createDirectories(outputsDir);
        Files.createDirectories(tempDir);
        Files.createDirectories(tempVideoDir);
    }

    @Override
    public void processVideoFromS3(String videoId, String email) {

        VideoFrameEntity result = new VideoFrameEntity();
        String nomeArquivo = videoId + "_frames";
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        Path workingDir = tempDir.resolve(email + timestamp);
        Path workingVideoDir = tempVideoDir.resolve(email + timestamp);
        byte[] zipBytes = new byte[0];
        try {
            this.save(result, videoId, ProcessorStatus.PROCESSING, email);

            Path videoTempPath = s3Repository.downloadToTempFile(videoId, workingVideoDir);

            Files.createDirectories(workingDir);

            String framePattern = workingDir.resolve("frame_%04d.png").toString();

            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg", "-i", videoTempPath.toString(), "-vf", "fps=1", "-y", framePattern
            );
            Process process = pb.redirectErrorStream(true).start();

            String output = new String(process.getInputStream().readAllBytes());
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new VideoProcessingException("Erro no ffmpeg: " + output);
            }

            List<Path> frames;
            try (Stream<Path> stream = Files.list(workingDir)) {
                frames = stream.filter(p -> p.toString().endsWith(".png")).toList();
            }

            if (frames.isEmpty()) {
                throw new VideoProcessingException("Nenhum frame foi extraído do vídeo");
            }

            zipBytes = createZipFile(frames);
            this.save(result, null, ProcessorStatus.SUCCESS, null);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            this.errorHandler(result, e);
        } catch (Exception e) {
            this.errorHandler(result, e);
        } finally {
            FileSystemUtils.deleteRecursively(workingDir.toFile());
            FileSystemUtils.deleteRecursively(workingVideoDir.toFile());
        }

        this.emailService.sendEmail(email, zipBytes, nomeArquivo);
    }

    private void save(VideoFrameEntity result, String videoId, ProcessorStatus status, String email) {
        if (videoId != null && !videoId.isBlank()) result.setS3VideoId(videoId);
        if (email != null && !email.isBlank()) result.setEmail(email);
        result.setStatus(status);

        frameRepository.save(result);
    }

    private byte[] createZipFile(List<Path> files) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            files.forEach(arquivo -> {
                try {
                    ZipEntry zipEntry = new ZipEntry(arquivo.getFileName().toString());
                    zos.putNextEntry(zipEntry);
                    Files.copy(arquivo, zos);
                    zos.closeEntry();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
        return baos.toByteArray();
    }

    private void errorHandler(VideoFrameEntity result, Exception e) {
        this.save(result, null, ProcessorStatus.ERROR, null);
        log.error("Ocorreu um erro no processamento do video {}", e.getMessage());
    }
}
