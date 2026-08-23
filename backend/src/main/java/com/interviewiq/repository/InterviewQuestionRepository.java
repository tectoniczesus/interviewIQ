package com.interviewiq.repository;

import com.interviewiq.entity.InterviewQuestion;
import com.interviewiq.entity.enums.QuestionCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InterviewQuestionRepository extends JpaRepository<InterviewQuestion, UUID> {
    List<InterviewQuestion> findBySessionId(UUID sessionId);
    List<InterviewQuestion> findBySessionIdAndCategory(UUID sessionId, QuestionCategory category);
}
