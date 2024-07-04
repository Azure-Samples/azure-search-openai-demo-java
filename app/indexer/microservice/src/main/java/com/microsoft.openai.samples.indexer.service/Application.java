// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.openai.samples.indexer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jms.annotation.EnableJms;

@SpringBootApplication
@EnableJms
public class Application {

    private static final Logger LOG = LoggerFactory.getLogger(Application.class);
   // private final ServiceBusProcessorClient processorClient;

   // public Application(ServiceBusProcessorClient processorClient) {
   //     this.processorClient = processorClient;
   // }

    public static void main(String[] args) {
        LOG.info(
                "Application profile from system property is [{}]",
                System.getProperty("spring.profiles.active"));
        SpringApplication.run(Application.class,args);
    }



    public void run(String... args) throws Exception {

       // System.out.printf("Starting the processor");
       // processorClient.start();
       // TimeUnit.SECONDS.sleep(10);
       // System.out.printf("Stopping and closing the processor");
       // processorClient.close();
    }

}
