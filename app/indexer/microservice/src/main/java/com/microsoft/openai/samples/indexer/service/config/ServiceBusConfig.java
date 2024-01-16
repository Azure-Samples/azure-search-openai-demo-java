package com.microsoft.openai.samples.indexer.service.config;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.messaging.servicebus.*;
import com.azure.spring.cloud.service.servicebus.consumer.ServiceBusErrorHandler;
import com.azure.spring.cloud.service.servicebus.consumer.ServiceBusRecordMessageListener;
import com.microsoft.openai.samples.indexer.service.BlobMessageConsumer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration(proxyBeanMethods = false)
public class ServiceBusConfig {
    @Value("${servicebus.namespace}")
    String SERVICE_BUS_FQDN;

    @Value("${servicebus.queue-name}")
    String QUEUE_NAME;
    @Bean
    ServiceBusClientBuilder serviceBusClientBuilder(TokenCredential tokenCredential){
        String fullyQualifiedNamespace = SERVICE_BUS_FQDN+".servicebus.windows.net";
        return new ServiceBusClientBuilder()
                .fullyQualifiedNamespace(fullyQualifiedNamespace)
                .credential(tokenCredential);
    }
    @Bean
    ServiceBusProcessorClient serviceBusProcessorClient(ServiceBusClientBuilder builder, BlobMessageConsumer consumer) {
        ServiceBusProcessorClient serviceBusProcessorClient = builder.processor()
                .queueName(QUEUE_NAME)
                .processMessage(consumer::processMessage)
                .processError(consumer::processError)
                .buildProcessorClient();
        serviceBusProcessorClient.start();
        System.out.println("Started the processor");
        return serviceBusProcessorClient;
    }


    }
