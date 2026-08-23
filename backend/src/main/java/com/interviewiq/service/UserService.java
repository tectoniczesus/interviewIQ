package com.interviewiq.service;

import com.interviewiq.entity.User;
import com.interviewiq.security.AuthenticatedUser;

public interface UserService {

    /**
     * Looks up the local User row for the given Clerk identity, creating
     * it on first sign-in. This keeps a small local "users" table (used
     * for foreign keys throughout the schema) in sync with Clerk, which
     * remains the source of truth for authentication itself.
     */
    User findOrCreate(AuthenticatedUser authenticatedUser);
}
