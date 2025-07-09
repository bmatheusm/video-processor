package com.fiap.video_processor.util;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;

public class DummyMimeMessage extends MimeMessage {
    public DummyMimeMessage() {
        super(Session.getInstance(new Properties()));
    }
}
