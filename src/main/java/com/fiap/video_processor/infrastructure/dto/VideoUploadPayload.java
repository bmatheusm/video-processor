package com.fiap.video_processor.infrastructure.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class VideoUploadPayload {
    private String videoId;
    private String email;
}
