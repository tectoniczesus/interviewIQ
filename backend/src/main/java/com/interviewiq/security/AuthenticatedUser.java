package com.interviewiq.security;

/**
 * Minimal representation of the caller, derived exclusively from a
 * validated Clerk JWT. Services must use getClerkUserId() to scope every
 * query - never accept a user id from the request body or path.
 */
public record AuthenticatedUser(String clerkUserId, String email) {
}
