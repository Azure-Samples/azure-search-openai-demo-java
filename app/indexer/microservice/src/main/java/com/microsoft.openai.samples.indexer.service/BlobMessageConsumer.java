package com.microsoft.openai.samples.indexer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BlobMessageConsumer {
    private static final Logger logger = LoggerFactory.getLogger(BlobMessageConsumer.class);



    /**
    public void processMessage(ServiceBusReceivedMessageContext context) {
        ServiceBusReceivedMessage message = context.getMessage();
        logger.info("Processing message. Id: {}, Sequence #: {}. Contents: {}",
                message.getMessageId(), message.getSequenceNumber(), message.getBody());
    }

    public void processError(ServiceBusErrorContext context) {
        logger.error("Error when receiving messages from namespace: '%s'. Entity: '%s'%n",
                context.getFullyQualifiedNamespace(), context.getEntityPath());
    } */
}
