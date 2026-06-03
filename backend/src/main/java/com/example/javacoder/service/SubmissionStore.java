package com.example.javacoder.service;

import com.example.javacoder.model.Submission;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Service;

@Service
public class SubmissionStore {

    private final ConcurrentMap<Long, Submission> submissions = new ConcurrentHashMap<>();

    public void save(Submission submission) {
        submissions.put(submission.id(), submission);
    }

    public void update(Submission submission) {
        submissions.put(submission.id(), submission);
    }

    public Optional<Submission> findById(long id) {
        return Optional.ofNullable(submissions.get(id));
    }

    public List<Submission> findRecent() {
        return submissions.values().stream()
                .sorted(Comparator.comparing(Submission::submittedAt).reversed())
                .limit(20)
                .toList();
    }

    public long countByProblemId(long problemId) {
        return submissions.values().stream()
                .filter(submission -> submission.problemId() == problemId)
                .count();
    }

    public long acceptedCountByProblemId(long problemId) {
        return submissions.values().stream()
                .filter(submission -> submission.problemId() == problemId)
                .filter(submission -> "Accepted".equals(submission.status()))
                .count();
    }
}
