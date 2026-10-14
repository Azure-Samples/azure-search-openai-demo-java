// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.openai.samples.rag.controller.auth;

import com.microsoft.openai.samples.rag.config.AppAuthConfigurationProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *  This api is call on frontend startup to check if login custom openid javascript provider is enabled.
 *
 */
@RestController
public class AuthSetupController {
    private final AppAuthConfigurationProperties appConfig;

    public AuthSetupController(AppAuthConfigurationProperties appConfig) {
        this.appConfig = appConfig;
    }

    @GetMapping("/api/auth_setup")
    public AppAuthConfigurationProperties authSetup() {
        return appConfig;
    }
}
