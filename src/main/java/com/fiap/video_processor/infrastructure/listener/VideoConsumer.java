package com.fiap.video_processor.infrastructure.listener;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiap.video_processor.infrastructure.dto.VideoUploadPayload;
import com.fiap.video_processor.usecase.processor.VideoProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.util.List;

@Component
@Slf4j
public class VideoConsumer {

    private final VideoProcessor processor;
    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;

    @Value("${sqs.queue.url}")
    private String queueUrl;

    public VideoConsumer(VideoProcessor processor, SqsClient sqsClient, ObjectMapper objectMapper) {
        this.processor = processor;
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelay = 5000)
    public void pollMessages() {
        ReceiveMessageRequest request = ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .maxNumberOfMessages(5)
                .waitTimeSeconds(10)
                .build();

        List<Message> messages = sqsClient.receiveMessage(request).messages();

        for (Message msg : messages) {
            try {
                VideoUploadPayload payload = objectMapper.readValue(msg.body(), VideoUploadPayload.class);

                log.info("Recebido vídeo: {}", payload.getVideoId());

                processor.processVideoFromS3(payload.getVideoId(), payload.getEmail());

                // Deleta da fila após processar
                sqsClient.deleteMessage(DeleteMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .receiptHandle(msg.receiptHandle())
                        .build());
            } catch (JsonParseException e) {
                log.warn("Erro ao deserializar mensagem, body: {}", msg.body(), e);
            } catch (Exception e) {
                log.error("Ocorreu um erro ao processar um video: {}", e.getMessage());
            }
        }

    }
}