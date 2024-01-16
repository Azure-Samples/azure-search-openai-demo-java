package com.microsoft.openai.samples.indexer.service;

import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class BlobMessageConsumer {
    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    public void processMessage(ServiceBusReceivedMessageContext context) {
        ServiceBusReceivedMessage message = context.getMessage();
        logger.info("Processing message. Id: {}, Sequence #: {}. Contents: {}",
                message.getMessageId(), message.getSequenceNumber(), message.getBody());
    }

    public void processError(ServiceBusErrorContext context) {
        logger.error("Error when receiving messages from namespace: '%s'. Entity: '%s'%n",
                context.getFullyQualifiedNamespace(), context.getEntityPath());
    }
}
