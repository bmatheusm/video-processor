package com.fiap.video_processor.infrastructure.dto;

import lombok.Getter;

@Getter
public class VideoUploadPayload {
    private String videoId;
    private String videoFileName;
}
