package com.interviewiq.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JobFromUrlRequest(
        @Size(max = 255, message = "title must be 255 characters or fewer")
        String title,
        @Size(max = 255, message = "company must be 255 characters or fewer")
        String company,
        @NotBlank(message = "jobUrl must not be blank")
        @Size(max = 2048, message = "jobUrl is too long")
        String jobUrl
) {
}