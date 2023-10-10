package com.microsoft.openai.samples.rag.config;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthSetup {

    @GetMapping("/api/auth_setup")
    public String authSetup() {
        return """
                {
                    "useLogin": false
                }
                """.stripIndent();
    }
}
