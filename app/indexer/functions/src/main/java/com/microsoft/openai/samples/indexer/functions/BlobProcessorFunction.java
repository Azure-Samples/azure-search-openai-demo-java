package com.microsoft.openai.samples.indexer.functions;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.*;

import java.util.Optional;

/**
 * Azure Functions with Blob Trigger.
 */
public class BlobProcessorFunction {
    @FunctionName("BlobEventGridProcessor")
     /**
    * This function will be invoked when a new or updated blob is detected at the specified path. The blob contents are provided as input to this function.
    */
    @StorageAccount("AzureWebJobsStorage")
    public void run(
        @BlobTrigger(name = "content", path = "content/{filename}", dataType = "binary", source = "EventGrid" ) byte[] content,
        @BindingName("filename") String filename,
        final ExecutionContext context
    ) {
          context.getLogger().info("Java Blob trigger function processed a blob. Name: " + filename + "\n  Size: " + content.length + " Bytes");
      }
 }
