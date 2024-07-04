package com.microsoft.openai.samples.indexer.service.events;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.openai.samples.indexer.service.IndexerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
public class BlobMessageListener {
    private static final Logger logger = LoggerFactory.getLogger(BlobMessageListener.class);
    final private ObjectMapper mapper;
    final private IndexerService indexerService;

    public BlobMessageListener(IndexerService indexerService){
        mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.indexerService = indexerService;
    }

    @JmsListener(destination = "${servicebus.queue-name}")
    public void receiveMessage(String message) {
        logger.debug("Message received from EventGrid: {}", message);
        String blobUrl = "";

        try {
            BlobUpsertEventGridEvent event = mapper.readValue(message, BlobUpsertEventGridEvent.class);
            blobUrl = event.data().url();
            logger.info("New request to ingest document received: {}", blobUrl);
        } catch (Exception e) {
           throw new RuntimeException("Error when trying to unmarshall event grid message %s ".formatted(message),e);
        }

        try{
            indexerService.indexBlobDocument(blobUrl);
        } catch (Exception e) {
            throw new RuntimeException("Error when trying to index document %s ".formatted(blobUrl),e);
        }


    }
}
