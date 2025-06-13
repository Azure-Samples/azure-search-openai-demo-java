package com.microsoft.openai.samples.rag.controller.config;

import com.microsoft.openai.samples.rag.config.AppConfigurationProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * This controller provides an endpoint to retrieve the application configuration properties.
 * It's used by the frontend to fetch configuration settings and enable/disable features dynamically.
 */
@RestController
@RequestMapping("/api/config")
public class AppConfigController {
    private final AppConfigurationProperties appConfig;

    public AppConfigController(AppConfigurationProperties appConfig) {
        this.appConfig = appConfig;
    }

    @GetMapping
    public AppConfigurationProperties getConfig() {
        return appConfig;
    }
}

