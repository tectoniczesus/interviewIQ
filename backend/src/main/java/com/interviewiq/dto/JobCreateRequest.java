package com.interviewiq.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JobCreateRequest(
        @Size(max = 255, message = "title must be 255 characters or fewer")
        String title,
        @Size(max = 255, message = "company must be 255 characters or fewer")
        String company,
        @NotBlank(message = "description must not be blank")
        @Size(max = 50000, message = "description is too long")
        String description
) {
}