package com.fiap.video_processor.infrastructure.listener;

import com.fiap.video_processor.infrastructure.dto.VideoUploadPayload;
import com.fiap.video_processor.usecase.processor.VideoProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
@Slf4j
@RequiredArgsConstructor
public class VideoConsumer {

    @Autowired
    private VideoProcessor processor;

    @KafkaListener(topics = "video-upload", groupId = "video-processors")
    public void consume(VideoUploadPayload payload, @Header(KafkaHeaders.RECEIVED_KEY) String userId) {
        log.info("Recebido vídeo: {}", payload.getVideoFileName());

        // Salva temporariamente o vídeo
        try {
            Path tempVideo = Files.createTempFile("video_", "_" + payload.getVideoFileName());
            Files.write(tempVideo, payload.getVideoBytes());

            processor.processVideo(tempVideo.toFile(), payload.getVideoFileName(), userId);

        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}