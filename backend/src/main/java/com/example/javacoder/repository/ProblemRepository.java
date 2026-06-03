package com.example.javacoder.repository;

import com.example.javacoder.model.Problem;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Repository;

@Repository
public class ProblemRepository {

    private static final String PROBLEM_RESOURCE_PATTERN = "classpath*:/problems/*/problem.json";

    private final Map<Long, Problem> problems;

    public ProblemRepository(ObjectMapper objectMapper) {
        this(new PathMatchingResourcePatternResolver(), objectMapper);
    }

    ProblemRepository() {
        this(new ObjectMapper());
    }

    ProblemRepository(ResourcePatternResolver resourceResolver, ObjectMapper objectMapper) {
        this.problems = loadProblems(resourceResolver, objectMapper);
    }

    public Collection<Problem> findAll() {
        return problems.values();
    }

    public Optional<Problem> findById(long id) {
        return Optional.ofNullable(problems.get(id));
    }

    private static Map<Long, Problem> loadProblems(
            ResourcePatternResolver resourceResolver,
            ObjectMapper objectMapper
    ) {
        try {
            Resource[] resources = resourceResolver.getResources(PROBLEM_RESOURCE_PATTERN);
            if (resources.length == 0) {
                throw new IllegalStateException("No problem resources found at " + PROBLEM_RESOURCE_PATTERN);
            }

            Map<Long, Problem> loadedProblems = new LinkedHashMap<>();
            for (Resource resource : sorted(resources)) {
                Problem problem = readProblem(objectMapper, resource);
                Problem previous = loadedProblems.put(problem.id(), problem);
                if (previous != null) {
                    throw new IllegalStateException("Duplicate problem id: " + problem.id());
                }
            }

            return Collections.unmodifiableMap(loadedProblems);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load problem resources.", exception);
        }
    }

    private static Resource[] sorted(Resource[] resources) {
        return Arrays.stream(resources)
                .sorted((left, right) -> left.getDescription().compareTo(right.getDescription()))
                .toArray(Resource[]::new);
    }

    private static Problem readProblem(ObjectMapper objectMapper, Resource resource) throws IOException {
        try (InputStream inputStream = resource.getInputStream()) {
            return objectMapper.readValue(inputStream, Problem.class);
        } catch (IOException exception) {
            throw new IOException("Failed to read " + resource.getDescription(), exception);
        }
    }
}
