package com.interviewiq.service;

import com.interviewiq.exception.ValidationException;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;

@Component
public class JobUrlValidator {

    public void validate(String url) {
        if (url == null || url.isBlank()) {
            throw new ValidationException("Please provide a job posting URL");
        }

        URI uri;
        try {
            uri = new URI(url.trim());
        } catch (URISyntaxException ex) {
            throw new ValidationException("That doesn't look like a valid URL");
        }

        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
            throw new ValidationException("Job URLs must start with http:// or https://");
        }

        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new ValidationException("That doesn't look like a valid URL");
        }
    }
}