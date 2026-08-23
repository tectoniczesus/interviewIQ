package com.interviewiq.controller;

import com.interviewiq.dto.ApiResponse;
import com.interviewiq.dto.UserResponse;
import com.interviewiq.security.CurrentUserProvider;
import com.interviewiq.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final CurrentUserProvider currentUserProvider;

    public UserController(UserService userService, CurrentUserProvider currentUserProvider) {
        this.userService = userService;
        this.currentUserProvider = currentUserProvider;
    }

    /**
     * Returns (and lazily creates) the local user record for whoever the
     * validated Clerk token identifies as. This is the first real
     * endpoint the frontend calls after sign-in and is a good smoke test
     * that auth, CORS, and the database connection are all wired up.
     */
    @GetMapping("/me")
    public ApiResponse<UserResponse> me() {
        var authenticatedUser = currentUserProvider.getCurrentUser();
        var user = userService.findOrCreate(authenticatedUser);
        return ApiResponse.ok(UserResponse.from(user));
    }
}
