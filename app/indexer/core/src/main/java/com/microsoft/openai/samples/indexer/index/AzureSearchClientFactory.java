package com.microsoft.openai.samples.indexer.index;

import com.azure.core.credential.TokenCredential;
import com.azure.search.documents.SearchClient;
import com.azure.search.documents.indexes.SearchIndexClient;
import com.azure.search.documents.indexes.SearchIndexerClient;
import com.azure.search.documents.SearchClientBuilder;
import com.azure.search.documents.indexes.SearchIndexClientBuilder;
import com.azure.search.documents.indexes.SearchIndexerClientBuilder;

public class AzureSearchClientFactory {
    /**
     * Class representing a connection to a search service.
     * To learn more, please visit https://learn.microsoft.com/azure/search/search-what-is-azure-search
     */
    private final String endpoint;
    private final TokenCredential credential;
    private final String indexName;
    private final boolean verbose;

    public AzureSearchClientFactory(String serviceName, TokenCredential credential, String indexName, boolean verbose) {
        this.endpoint =  "https://%s.search.windows.net".formatted(serviceName);
        this.credential = credential;
        this.indexName = indexName;
        this.verbose = verbose;
    }

    public SearchClient createSearchClient() {
        return new SearchClientBuilder()
                .endpoint(endpoint)
                .credential(credential)
                .indexName(indexName)
                .buildClient();
    }

    public SearchIndexClient createSearchIndexClient() {
        return new SearchIndexClientBuilder()
                .endpoint(endpoint)
                .credential(credential)
                .buildClient();
    }

    public SearchIndexerClient createSearchIndexerClient() {
        return new SearchIndexerClientBuilder()
                .endpoint(endpoint)
                .credential(credential)
                .buildClient();
    }

    public String getEndpoint() {
        return endpoint;
    }

    public TokenCredential getCredential() {
        return credential;
    }

    public String getIndexName() {
        return indexName;
    }

    public boolean isVerbose() {
        return verbose;
    }
    
}
