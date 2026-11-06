package com.example.ailearning.l02requests;

import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class ModelRequestController {
    private final ModelRequestService modelRequestService;

    public ModelRequestController(ModelRequestService modelRequestService) {
        this.modelRequestService = modelRequestService;
    }

    @PostMapping(value = "/api/model-requests", consumes = MediaType.TEXT_PLAIN_VALUE)
    public String ask(@RequestBody @NotBlank String question) {
        try {
            return modelRequestService.ask(question);
        } catch (RuntimeException error) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "모델 요청에 실패했습니다. 잠시 후 다시 시도하세요.", error);
        }
    }
}
