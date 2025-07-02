package com.fiap.video_processor.usecase.processor;

import java.io.File;

public interface VideoProcessor {
    void processVideo(File videoFile, String fileName, String email);
}
