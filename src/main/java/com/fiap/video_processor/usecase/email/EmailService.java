package com.fiap.video_processor.usecase.email;

public interface EmailService {
    void sendEmail(String para, byte[] zipBytes, String nomeZip);
}
