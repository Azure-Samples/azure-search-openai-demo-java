package com.microsoft.openai.samples.indexer.service.config;

//@Configuration(proxyBeanMethods = false)
public class ServiceBusProcessorClientConfiguration {

    /**
    @Bean
    ServiceBusRecordMessageListener processMessage() {
        return context -> {
            ServiceBusReceivedMessage message = context.getMessage();
            System.out.printf("Processing message. Id: %s, Sequence #: %s. Contents: %s%n", message.getMessageId(),
                    message.getSequenceNumber(), message.getBody());
        };
    }

    @Bean
    ServiceBusErrorHandler processError() {
        return context -> {
            System.out.printf("Error when receiving messages from namespace: '%s'. Entity: '%s'%n",
                    context.getFullyQualifiedNamespace(), context.getEntityPath());
        };
    }

    **/
}