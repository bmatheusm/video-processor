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

@Component
@Slf4j
@RequiredArgsConstructor
public class VideoConsumer {

    @Autowired
    private VideoProcessor processor;

    @KafkaListener(topics = "video-upload", groupId = "video-processors")
    public void consume(VideoUploadPayload payload, @Header(KafkaHeaders.RECEIVED_KEY) String userId) {
        log.info("Recebido vídeo: {}", payload.getVideoFileName());

        processor.processVideoFromS3(payload.getVideoId(), payload.getVideoFileName(), userId);
    }
}