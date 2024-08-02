// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.openai.samples.rag.controller.auth;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *  This api is call on frontend startup to check if login custom openid javascript provider is enabled.
 *  However for this sample the default value is false. Authentication is provided instead by easyauth managed service on Azure compute platform.
 *  See 'Enabling Authentication' section on README.md for more details.
 */
@RestController
public class AuthSetup {

    @GetMapping("/api/auth_setup")
    public String authSetup() {
        return """
                {
                    "useLogin": false
                }
                """
                .stripIndent();
    }
}
