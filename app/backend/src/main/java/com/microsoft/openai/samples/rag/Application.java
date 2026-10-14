// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.openai.samples.rag;

import com.microsoft.openai.samples.rag.config.AppAuthConfigurationProperties;
import com.microsoft.openai.samples.rag.config.AppConfigurationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({AppConfigurationProperties.class, AppAuthConfigurationProperties.class})
public class Application {

    private static final Logger LOG = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {

        new SpringApplication(Application.class).run(args);
    }
}
