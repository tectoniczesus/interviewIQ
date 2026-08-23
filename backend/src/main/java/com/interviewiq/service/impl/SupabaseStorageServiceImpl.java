package com.interviewiq.service.impl;

import com.interviewiq.config.SupabaseConfig;
import com.interviewiq.exception.StorageException;
import com.interviewiq.service.SupabaseStorageService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SupabaseStorageServiceImpl implements SupabaseStorageService {

    private final WebClient storageClient;
    private final SupabaseConfig supabaseConfig;

    public SupabaseStorageServiceImpl(@Qualifier("supabaseStorageWebClient") WebClient storageClient,
                                      SupabaseConfig supabaseConfig) {
        this.storageClient = storageClient;
        this.supabaseConfig = supabaseConfig;
    }

    @Override
    public String upload(String pathPrefix, String originalFileName, byte[] content, String contentType) {
        String safeName = originalFileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        String objectPath = "%s/%s-%s".formatted(pathPrefix, UUID.randomUUID(), safeName);
        String bucket = supabaseConfig.getStorageBucket();

        try {
            storageClient.post()
                    .uri(buildObjectUri(bucket, objectPath))
                    .contentType(MediaType.parseMediaType(contentType != null ? contentType : "application/pdf"))
                    .bodyValue(content)
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(Duration.ofSeconds(30))
                    .block();

            return objectPath;
        } catch (Exception ex) {
            throw new StorageException("Failed to upload file to Supabase Storage", ex);
        }
    }

    @Override
    public byte[] download(String storagePath) {
        String bucket = supabaseConfig.getStorageBucket();
        try {
            return storageClient.get()
                    .uri(buildObjectUri(bucket, storagePath))
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .timeout(Duration.ofSeconds(30))
                    .block();
        } catch (Exception ex) {
            throw new StorageException("Failed to download file from Supabase Storage", ex);
        }
    }

    @Override
    public void delete(String storagePath) {
        String bucket = supabaseConfig.getStorageBucket();
        try {
            storageClient.delete()
                    .uri(buildObjectUri(bucket, storagePath))
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(Duration.ofSeconds(30))
                    .block();
        } catch (Exception ex) {
            throw new StorageException("Failed to delete file from Supabase Storage", ex);
        }
    }

    /**
     * Builds the full Supabase Storage object URL by hand rather than via
     * WebClient's "{var}" template expansion. objectPath legitimately
     * contains multiple "/" segments (e.g. "resumes/<userId>/<uuid>-file.pdf"),
     * and Spring's URI-template expander can misfire ("Not enough variable
     * values available to expand 'path'") on multi-segment path variables.
     * Encoding each segment ourselves and joining with literal "/" sidesteps
     * template parsing entirely - there are no "{}" left for Spring to expand.
     */
    private URI buildObjectUri(String bucket, String objectPath) {
        String encodedBucket = encodeSegment(bucket);
        String encodedPath = Arrays.stream(objectPath.split("/"))
                .map(this::encodeSegment)
                .collect(Collectors.joining("/"));

        return URI.create("%s/storage/v1/object/%s/%s".formatted(
                supabaseConfig.getUrl(), encodedBucket, encodedPath));
    }

    private String encodeSegment(String segment) {
        return URLEncoder.encode(segment, StandardCharsets.UTF_8).replace("+", "%20");
    }
}