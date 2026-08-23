package com.interviewiq.dto;

import jakarta.validation.constraints.NotBlank;

public record JobFromUrlRequest(
        String title,
        String company,
        @NotBlank(message = "jobUrl must not be blank")
        String jobUrl
) {
}