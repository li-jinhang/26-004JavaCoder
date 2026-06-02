package com.example.javacoder.service;

import com.example.javacoder.model.Submission;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Service;

@Service
public class SubmissionStore {

    private final List<Submission> submissions = new CopyOnWriteArrayList<>();

    public void save(Submission submission) {
        submissions.add(submission);
    }

    public List<Submission> findRecent() {
        return submissions.stream()
                .sorted(Comparator.comparing(Submission::submittedAt).reversed())
                .limit(20)
                .toList();
    }

    public long countByProblemId(long problemId) {
        return submissions.stream()
                .filter(submission -> submission.problemId() == problemId)
                .count();
    }

    public long acceptedCountByProblemId(long problemId) {
        return submissions.stream()
                .filter(submission -> submission.problemId() == problemId)
                .filter(submission -> "Accepted".equals(submission.status()))
                .count();
    }
}
