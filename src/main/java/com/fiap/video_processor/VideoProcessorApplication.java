package com.fiap.video_processor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class VideoProcessorApplication {

	public static void main(String[] args) {
		SpringApplication.run(VideoProcessorApplication.class, args);
	}

}
