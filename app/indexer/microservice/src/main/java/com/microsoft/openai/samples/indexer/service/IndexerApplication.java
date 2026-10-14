// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.openai.samples.indexer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
public class IndexerApplication {

    private static final Logger LOG = LoggerFactory.getLogger(IndexerApplication.class);

    public static void main(String[] args) {
        LOG.info(
                "Application profile from system property is [{}]",
                System.getProperty("spring.profiles.active"));
        SpringApplication.run(IndexerApplication.class,args);
    }



}
