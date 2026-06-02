package com.example.javacoder.controller;

import com.example.javacoder.model.ProblemDetail;
import com.example.javacoder.model.ProblemSummary;
import com.example.javacoder.repository.ProblemRepository;
import com.example.javacoder.service.SubmissionStore;
import java.util.Comparator;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/problems")
public class ProblemController {

    private final ProblemRepository problemRepository;
    private final SubmissionStore submissionStore;

    public ProblemController(ProblemRepository problemRepository, SubmissionStore submissionStore) {
        this.problemRepository = problemRepository;
        this.submissionStore = submissionStore;
    }

    @GetMapping
    public List<ProblemSummary> listProblems() {
        return problemRepository.findAll().stream()
                .sorted(Comparator.comparingLong(problem -> problem.id()))
                .map(problem -> new ProblemSummary(
                        problem.id(),
                        problem.title(),
                        problem.difficulty(),
                        problem.tags(),
                        (int) submissionStore.acceptedCountByProblemId(problem.id()),
                        (int) submissionStore.countByProblemId(problem.id())
                ))
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProblemDetail> getProblem(@PathVariable long id) {
        return problemRepository.findById(id)
                .map(ProblemDetail::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
