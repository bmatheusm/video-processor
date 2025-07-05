package com.fiap.video_processor.usecase.processor;

public interface VideoProcessor {
    void processVideoFromS3(String videoId, String email);
}
