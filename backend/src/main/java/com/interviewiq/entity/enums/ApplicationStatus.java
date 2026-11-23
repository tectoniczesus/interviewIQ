package com.interviewiq.entity.enums;

/**
 * Where a saved job is in the user's application pipeline. Stages only change
 * when the user moves them; the app may suggest a move but never makes one.
 */
public enum ApplicationStatus {
    SAVED,
    APPLIED,
    INTERVIEWING,
    OFFER,
    REJECTED,
    WITHDRAWN;

    public boolean isClosed() {
        return this == REJECTED || this == WITHDRAWN;
    }
}
