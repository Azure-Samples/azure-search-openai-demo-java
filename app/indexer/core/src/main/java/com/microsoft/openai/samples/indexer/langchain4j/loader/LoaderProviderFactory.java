package com.microsoft.openai.samples.indexer.langchain4j.loader;

import com.azure.core.credential.TokenCredential;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.microsoft.openai.samples.indexer.langchain4j.ConfigUtils;
import com.microsoft.openai.samples.indexer.langchain4j.providers.DocumentSourceProvider;

import java.util.function.Function;

/**
 * Factory class to create instances of DocumentSourceProvider based on LoaderConfig.
 * Currently supports Azure Blob Storage loader.
 */
public class LoaderProviderFactory {
    private final Function<String, Object> DIResolver;

    public LoaderProviderFactory(Function<String, Object> diResolver) {
        DIResolver = diResolver;
    }

   public DocumentSourceProvider create(LoaderConfig config) {
        if (config == null || config.type() == null) {
            throw new IllegalArgumentException("LoaderConfig and its type must not be null");
        }

        switch (config.type()) {
            case "azure-blob":
                return buildAzureBlobLoaderProvider(config);
            default:
                throw new IllegalArgumentException("Unsupported loader type: " + config.type());
        }

    }

    private DocumentSourceProvider buildAzureBlobLoaderProvider(LoaderConfig config) {
        var storageAccountServiceName = ConfigUtils.getString("storage-account-name", config.params());
        var containerName = ConfigUtils.getString("container-name", config.params());
        var identityRef = ConfigUtils.getString("identity-ref", config.params());

        var tokenCredential = (TokenCredential) DIResolver.apply(identityRef);

        String endpoint = "https://%s.blob.core.windows.net".formatted(storageAccountServiceName);
        return ctx -> {
            var filenameOrUrl = (String) ctx.get("filename-or-url");
            ConfigUtils.validateString("filename-or-url", filenameOrUrl);
            var containerClient = new BlobContainerClientBuilder()
                    .endpoint(endpoint)
                    .credential(tokenCredential)
                    .containerName(containerName)
                    .buildClient();
            return new AzureBlobStorageDocumentLoader(containerClient, filenameOrUrl);

        };
    }

}
