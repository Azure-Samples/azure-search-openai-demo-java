package com.microsoft.openai.samples.indexer.langchain4j.loader;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import dev.langchain4j.data.document.DocumentSource;
import dev.langchain4j.data.document.Metadata;


import java.io.InputStream;


public class AzureBlobStorageDocumentLoader implements DocumentSource {
    private final BlobClient blobClient;
    private final Metadata metadata;



    public AzureBlobStorageDocumentLoader(BlobContainerClient blobcontainerClient, String blobName) {
        this.blobClient = blobcontainerClient.getBlobClient(blobName);

        var properties = blobClient.getProperties();
        this.metadata = new Metadata();
        String filename = blobName.lastIndexOf('/') != -1 ? blobName.substring(blobName.lastIndexOf('/') + 1) : blobName;
        //source field is used by langchain4j to identify the source of the document
        metadata.put("source",blobName);
        metadata.put("file_path", blobName);
        metadata.put("file_name", filename);
        metadata.put("file_creation_time", String.valueOf(properties.getCreationTime()));
        metadata.put("file_last_modified", String.valueOf(properties.getLastModified()));
        metadata.put("file_content_length", String.valueOf(properties.getBlobSize()));

    }

    @Override
    public InputStream inputStream() {
       return blobClient.openInputStream();

    }

    @Override
    public Metadata metadata() {
        return metadata;
    }


}
