package com.interviewiq.security;

import com.interviewiq.exception.NotAuthenticatedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Central place services/controllers use to read "who is calling right
 * now". Never trust a userId passed in a DTO - always resolve it here,
 * from the token that Spring Security already validated.
 */
@Component
public class CurrentUserProvider {

    public AuthenticatedUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new NotAuthenticatedException("No authenticated user in the current security context");
        }

        return user;
    }

    public String getCurrentClerkUserId() {
        return getCurrentUser().clerkUserId();
    }
}