package com.fiap.video_processor.domain.entities;

import com.fiap.video_processor.infra.enums.ProcessorStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "video_frame")
public class VideoFrameEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "video_frame_gen")
    @SequenceGenerator(name = "video_frame_gen", sequenceName = "video_frame_seq")
    @Column(name = "id", nullable = false)
    private Long id;

    private String nomeArquivo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProcessorStatus status;

    private String s3VideoId;

    private String email;

    @CreationTimestamp
    private LocalDateTime criadoEm;
}
