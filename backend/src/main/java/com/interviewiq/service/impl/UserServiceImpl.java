package com.interviewiq.service.impl;

import com.interviewiq.entity.User;
import com.interviewiq.repository.UserRepository;
import com.interviewiq.security.AuthenticatedUser;
import com.interviewiq.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public User findOrCreate(AuthenticatedUser authenticatedUser) {
        return userRepository.findByClerkUserId(authenticatedUser.clerkUserId())
                .orElseGet(() -> {
                    User user = new User();
                    user.setClerkUserId(authenticatedUser.clerkUserId());
                    user.setEmail(authenticatedUser.email());
                    return userRepository.save(user);
                });
    }
}
