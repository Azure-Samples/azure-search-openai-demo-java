package com.microsoft.openai.samples.indexer.service.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BlobUpsertEventGridEvent (
     String id,
     String eventType,
     String subject,
     String eventTime,
     String dataVersion,
     BlobEventGridData data){}
