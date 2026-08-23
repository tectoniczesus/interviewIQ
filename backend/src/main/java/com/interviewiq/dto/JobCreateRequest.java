package com.interviewiq.dto;

import jakarta.validation.constraints.NotBlank;

public record JobCreateRequest(
        String title,
        String company,
        @NotBlank(message = "description must not be blank")
        String description
) {
}