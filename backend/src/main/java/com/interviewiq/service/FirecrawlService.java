package com.interviewiq.service;

/**
 * Wraps the Firecrawl API for extracting readable content from a job
 * posting URL. Callers (JobService, added in Phase 4) are responsible for
 * validating the URL before it reaches this service and for deciding what
 * to do with an empty/failed extraction.
 */
public interface FirecrawlService {

    /**
     * @param url a job posting URL, already validated by the caller
     * @return extracted page content in markdown/plain text
     */
    String scrapeUrl(String url);
}
