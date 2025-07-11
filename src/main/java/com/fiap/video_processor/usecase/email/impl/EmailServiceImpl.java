package com.fiap.video_processor.usecase.email.impl;

import com.fiap.video_processor.usecase.email.EmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String emailRemetente;

    @Override
    public void sendEmail(String para, byte[] zipBytes, String nomeZip) {
        try {
            MimeMessage mensagem = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensagem, true);
            helper.setTo(para);
            helper.setSubject("Frames extraídos do vídeo");
            helper.setFrom(emailRemetente);

            if (zipBytes.length > 0) {
                helper.setText("Segue em anexo o arquivo ZIP com os frames extraídos.");
                helper.addAttachment(nomeZip + ".zip", new ByteArrayResource(zipBytes));
            }else {
                helper.setText("Ocorreu um erro no processamento do video e não foi possivel extrair os frames");
            }
            log.info("Enviando email de {} para {}", emailRemetente, para);
            mailSender.send(mensagem);
            log.info("Email enviado com sucesso!");
        } catch (Exception e) {
            log.error("Falha no envio do email: {}" ,e.getMessage());
        }

    }
}
