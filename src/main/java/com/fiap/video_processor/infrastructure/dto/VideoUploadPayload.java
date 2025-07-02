package com.fiap.video_processor.infrastructure.dto;

import lombok.Getter;

@Getter
public class VideoUploadPayload {
    private byte[] videoBytes;
    private String videoFileName;
}
