package com.interviewiq.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Binds app.supabase.* properties. The service-role key is only ever used
 * server-side (SupabaseStorageService) and must never be sent to the
 * frontend.
 */
@Configuration
@ConfigurationProperties(prefix = "app.supabase")
public class SupabaseConfig {

    private String url;
    private String anonKey;
    private String serviceRoleKey;
    private String storageBucket;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getAnonKey() {
        return anonKey;
    }

    public void setAnonKey(String anonKey) {
        this.anonKey = anonKey;
    }

    public String getServiceRoleKey() {
        return serviceRoleKey;
    }

    public void setServiceRoleKey(String serviceRoleKey) {
        this.serviceRoleKey = serviceRoleKey;
    }

    public String getStorageBucket() {
        return storageBucket;
    }

    public void setStorageBucket(String storageBucket) {
        this.storageBucket = storageBucket;
    }

    /**
     * WebClient authenticated with the service-role key, used exclusively
     * by SupabaseStorageService for server-side upload/download/delete
     * operations against the Supabase Storage REST API.
     */
    @Bean
    public WebClient supabaseStorageWebClient() {
        return WebClient.builder()
                .baseUrl(url + "/storage/v1")
                .defaultHeader("Authorization", "Bearer " + serviceRoleKey)
                .defaultHeader("apikey", serviceRoleKey)
                .build();
    }
}
