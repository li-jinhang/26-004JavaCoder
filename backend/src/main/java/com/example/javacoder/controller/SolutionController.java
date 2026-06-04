package com.example.javacoder.controller;

import com.example.javacoder.model.CurrentUser;
import com.example.javacoder.model.Solution;
import com.example.javacoder.model.SolutionRequest;
import com.example.javacoder.model.UserRole;
import com.example.javacoder.repository.ProblemRepository;
import com.example.javacoder.service.AdminAccountStore;
import com.example.javacoder.service.SolutionStore;
import com.example.javacoder.service.UserStore;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class SolutionController {

    private final ProblemRepository problemRepository;
    private final SolutionStore solutionStore;
    private final UserStore userStore;
    private final AdminAccountStore adminAccountStore;

    public SolutionController(
            ProblemRepository problemRepository,
            SolutionStore solutionStore,
            UserStore userStore,
            AdminAccountStore adminAccountStore
    ) {
        this.problemRepository = problemRepository;
        this.solutionStore = solutionStore;
        this.userStore = userStore;
        this.adminAccountStore = adminAccountStore;
    }

    @GetMapping("/problems/{problemId}/solutions")
    public ResponseEntity<?> listByProblem(@PathVariable long problemId) {
        if (problemRepository.findById(problemId).isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "题目不存在。"));
        }
        return ResponseEntity.ok(solutionStore.findByProblemId(problemId));
    }

    @PostMapping("/problems/{problemId}/solutions")
    public ResponseEntity<?> create(
            @PathVariable long problemId,
            @RequestBody SolutionRequest request,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
    ) {
        Optional<CurrentUser> user = requirePrincipal(authorization);
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "请先登录后再发布题解。"));
        }

        return problemRepository.findById(problemId)
                .<ResponseEntity<?>>map(problem -> {
                    try {
                        Solution solution = solutionStore.create(problem, user.orElseThrow(), request);
                        return ResponseEntity.status(HttpStatus.CREATED).body(solution);
                    } catch (IllegalArgumentException exception) {
                        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
                    }
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "题目不存在。")));
    }

    @GetMapping("/solutions/{id}")
    public ResponseEntity<?> get(@PathVariable long id) {
        return solutionStore.findById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "题解不存在。")));
    }

    @PutMapping("/solutions/{id}")
    public ResponseEntity<?> update(
            @PathVariable long id,
            @RequestBody SolutionRequest request,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
    ) {
        Optional<CurrentUser> user = requirePrincipal(authorization);
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "请先登录后再编辑题解。"));
        }

        Optional<Solution> existing = solutionStore.findById(id);
        if (existing.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "题解不存在。"));
        }
        if (existing.orElseThrow().authorId() != user.orElseThrow().id()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "只能编辑自己发布的题解。"));
        }

        try {
            return ResponseEntity.ok(solutionStore.update(id, request));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
        }
    }

    @DeleteMapping("/solutions/{id}")
    public ResponseEntity<?> delete(
            @PathVariable long id,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
    ) {
        Optional<CurrentUser> principal = requirePrincipal(authorization);
        if (principal.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "请先登录后再删除题解。"));
        }

        Optional<Solution> existing = solutionStore.findById(id);
        if (existing.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "题解不存在。"));
        }

        CurrentUser currentUser = principal.orElseThrow();
        boolean canDelete = currentUser.role() == UserRole.ADMIN || existing.orElseThrow().authorId() == currentUser.id();
        if (!canDelete) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "只能删除自己发布的题解。"));
        }

        solutionStore.delete(id);
        return ResponseEntity.noContent().build();
    }

    private Optional<CurrentUser> requirePrincipal(String authorization) {
        String token = extractBearerToken(authorization);
        return adminAccountStore.findByToken(token).or(() -> userStore.findByToken(token));
    }

    private String extractBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization.substring("Bearer ".length()).trim();
    }
}
