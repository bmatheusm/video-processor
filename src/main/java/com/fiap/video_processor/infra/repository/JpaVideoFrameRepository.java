package com.fiap.video_processor.infra.repository;

import com.fiap.video_processor.domain.entities.VideoFrameEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JpaVideoFrameRepository extends JpaRepository<VideoFrameEntity, Long> {
    Optional<VideoFrameEntity> findByNomeArquivo(String nomeArquivo);
}
