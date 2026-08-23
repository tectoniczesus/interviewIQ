package com.interviewiq.service;

import com.interviewiq.dto.DashboardResponse;
import com.interviewiq.security.AuthenticatedUser;

public interface DashboardService {
    DashboardResponse getSummary(AuthenticatedUser authenticatedUser);
}