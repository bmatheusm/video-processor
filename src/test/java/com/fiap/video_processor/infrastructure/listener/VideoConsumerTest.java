package com.fiap.video_processor.infrastructure.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiap.video_processor.infrastructure.dto.VideoUploadPayload;
import com.fiap.video_processor.usecase.processor.VideoProcessor;
import com.fiap.video_processor.util.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VideoConsumerTest {

    @Mock
    private VideoProcessor processor;

    @Mock
    private SqsClient sqsClient;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private VideoConsumer consumer;

    @BeforeEach
    void setup() {
        consumer = new VideoConsumer(processor, sqsClient, objectMapper);
        TestUtils.setField(consumer, "queueUrl", "https://sqs.us-east-1.amazonaws.com/123/video-processing-queue");
    }

    @Test
    void deveProcessarMensagemValida() throws Exception {
        // Arrange
        String jsonBody = "{\"videoId\":\"abc123\", \"email\":\"user@example.com\"}";

        Message message = Message.builder()
                .body(jsonBody)
                .receiptHandle("abc-handle")
                .build();

        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(ReceiveMessageResponse.builder().messages(List.of(message)).build());

        VideoUploadPayload payload = new VideoUploadPayload("abc123", "user@example.com");

        when(objectMapper.readValue(jsonBody, VideoUploadPayload.class))
                .thenReturn(payload);

        // Act
        consumer.pollMessages();

        // Assert
        verify(processor).processVideoFromS3("abc123", "user@example.com");

        verify(sqsClient).deleteMessage(argThat((DeleteMessageRequest req) ->
                "https://sqs.us-east-1.amazonaws.com/123/video-processing-queue".equals(req.queueUrl()) &&
                        "abc-handle".equals(req.receiptHandle())
        ));
    }

    @Test
    void deveIgnorarMensagemInvalida() throws Exception {
        String jsonBody = "mensagem inválida";

        Message message = Message.builder()
                .body(jsonBody)
                .receiptHandle("handle-invalido")
                .build();

        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(ReceiveMessageResponse.builder().messages(List.of(message)).build());

        when(objectMapper.readValue(jsonBody, VideoUploadPayload.class))
                .thenThrow(new com.fasterxml.jackson.core.JsonParseException(null, "Erro de parsing"));

        consumer.pollMessages();

        verify(processor, never()).processVideoFromS3(any(), any());
        verify(sqsClient, never()).deleteMessage((DeleteMessageRequest) any());
    }

    @Test
    void deveLogarErroGenericoAoProcessarMensagem() throws Exception {
        String jsonBody = "{\"videoId\":\"abc123\", \"email\":\"user@example.com\"}";

        Message message = Message.builder()
                .body(jsonBody)
                .receiptHandle("handle-erro")
                .build();

        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(ReceiveMessageResponse.builder().messages(List.of(message)).build());

        VideoUploadPayload payload = new VideoUploadPayload("abc123", "user@example.com");

        when(objectMapper.readValue(jsonBody, VideoUploadPayload.class)).thenReturn(payload);
        doThrow(new RuntimeException("Erro inesperado")).when(processor).processVideoFromS3(any(), any());

        consumer.pollMessages();

        verify(sqsClient, never()).deleteMessage((DeleteMessageRequest) any());
    }
}