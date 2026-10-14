package com.microsoft.openai.samples.indexer.service.events;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.openai.samples.indexer.service.langchain4j.Langchain4JIndexerService;
import com.microsoft.openai.samples.indexer.storage.BlobManager;
import dev.langchain4j.data.document.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import java.io.IOException;


@Component
@ConditionalOnProperty(name = "indexing.useServiceBus", havingValue = "true", matchIfMissing = false)
public class BlobMessageListener {
    private static final Logger logger = LoggerFactory.getLogger(BlobMessageListener.class);
    final private ObjectMapper mapper;
    private final Langchain4JIndexerService indexerService;
    private final BlobManager blobManager;

    public BlobMessageListener(Langchain4JIndexerService indexerService, BlobManager blobManager){
        mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.indexerService = indexerService;
        this.blobManager = blobManager;
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
          throw new RuntimeException("Error when trying to unmarshall event grid message %s ".formatted(message), e);
      }

      //extract filename
      String filename = "";

      //Check lastindex is not out of bound
      if (blobUrl.lastIndexOf('/') != -1) {
          filename = blobUrl.substring(blobUrl.lastIndexOf('/') + 1);
      } else {
          throw new RuntimeException("Cannot extract filename from URL: " + blobUrl); // If no '/' found, use the whole URL as filename
      }

      //Moving file from staging to default folder
      try {
          blobManager.moveFileToFolder(filename, "staging", "default");
      } catch (IOException ex) {
        logger.error("Error moving file {} from staging to default folder ",filename,ex);
         }

        // eventgrid upload document request goes to default folder
        filename = "default/" + filename;

        Metadata indexingMetadata = new Metadata();
        //set default as oid as this is the default folder in blob storage for public available documents
        indexingMetadata.put("oid", "default");

        // Use Langchain4JIndexerService to index the file or URL
        indexerService.index(filename, indexingMetadata);

    }
}
