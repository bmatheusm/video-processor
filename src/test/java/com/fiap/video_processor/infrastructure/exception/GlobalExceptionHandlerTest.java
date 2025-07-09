package com.fiap.video_processor.infrastructure.exception;

import com.fiap.video_processor.domain.exceptions.VideoProcessingException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ExceptionThrowingController.class)
@Import(GlobalExceptionHandler.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void deveRetornarErro500QuandoVideoProcessingExceptionForLancada() throws Exception {
        mockMvc.perform(get("/teste-exception"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("erro de processamento de vídeo"));
    }
}

@RestController
@RequestMapping("/teste-exception")
class ExceptionThrowingController {
    @GetMapping
    public String disparaErro() {
        throw new VideoProcessingException("erro de processamento de vídeo");
    }
}
