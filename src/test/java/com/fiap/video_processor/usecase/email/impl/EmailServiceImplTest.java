package com.fiap.video_processor.usecase.email.impl;

import com.fiap.video_processor.util.DummyMimeMessage;
import com.fiap.video_processor.util.TestUtils;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.mail.javamail.JavaMailSender;

import static org.mockito.Mockito.*;

class EmailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailServiceImpl emailService;

    @Captor
    private ArgumentCaptor<MimeMessage> messageCaptor;

    private final String emailFrom = "noreply@teste.com";

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        TestUtils.setField(emailService, "emailRemetente", emailFrom);
    }

    @Test
    void deveEnviarEmailComAnexoQuandoZipNaoEstiverVazio() throws MessagingException {
        // Arrange
        String destinatario = "destino@teste.com";
        byte[] zipBytes = "fake zip content".getBytes();
        String nomeZip = "meuarquivo";

        MimeMessage mimeMessage = new DummyMimeMessage();
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // Act
        emailService.sendEmail(destinatario, zipBytes, nomeZip);

        // Assert
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void deveEnviarEmailComTextoDeErroQuandoZipEstiverVazio() throws MessagingException {
        // Arrange
        String destinatario = "destino@teste.com";
        byte[] zipBytes = new byte[0];
        String nomeZip = "meuarquivo";

        MimeMessage mimeMessage = new DummyMimeMessage();
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // Act
        emailService.sendEmail(destinatario, zipBytes, nomeZip);

        // Assert
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void deveLogarErroQuandoOcorrerMessagingException() {
        // Arrange
        String destinatario = "destino@teste.com";
        byte[] zipBytes = "falha".getBytes();
        String nomeZip = "falha_zip";

        MimeMessage mimeMessage = new DummyMimeMessage();
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        doAnswer(invocation -> {
            throw new RuntimeException(new MessagingException("Simulado"));
        }).when(mailSender).send(any(MimeMessage.class));

        // Act
        emailService.sendEmail(destinatario, zipBytes, nomeZip);

        // Assert
        verify(mailSender).send(any(MimeMessage.class));
    }

}
