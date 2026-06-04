package com.example.javacoder.controller;

import com.example.javacoder.model.SupportedLanguage;
import com.example.javacoder.service.LanguageRegistry;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/languages")
public class LanguageController {

    private final LanguageRegistry languageRegistry;

    public LanguageController(LanguageRegistry languageRegistry) {
        this.languageRegistry = languageRegistry;
    }

    @GetMapping
    public List<SupportedLanguage> listLanguages() {
        return languageRegistry.supportedLanguages();
    }
}
