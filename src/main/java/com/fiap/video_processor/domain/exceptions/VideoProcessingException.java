package com.fiap.video_processor.domain.exceptions;

public class VideoProcessingException extends RuntimeException {
    public VideoProcessingException(String message) {
        super(message);
    }
}
