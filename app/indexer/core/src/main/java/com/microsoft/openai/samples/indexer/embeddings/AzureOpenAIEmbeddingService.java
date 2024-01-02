package com.microsoft.openai.samples.indexer.embeddings;

import com.azure.ai.openai.OpenAIAsyncClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.core.credential.TokenCredential;
import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.http.policy.HttpLogOptions;

public class AzureOpenAIEmbeddingService extends AbstractTextEmbeddingsService {
    private String openAIServiceName;
    private TokenCredential tokenCredential;

    public AzureOpenAIEmbeddingService(String openAIServiceName, String openAiDeploymentName, TokenCredential tokenCredential, boolean verbose) {
        //current Azure OpenAI Embeddings service limit are 16 batch items per request 8192 tokens
        super(openAiDeploymentName, verbose, 16, 8192);
        this.openAIServiceName = openAIServiceName;
        this.tokenCredential = tokenCredential;
    }

    @Override
 protected OpenAIAsyncClient createClient() {
        String endpoint = "https://%s.openai.azure.com".formatted(openAIServiceName);

    
        var httpLogOptions = new HttpLogOptions();
        if(verbose){
            // still not sure to include the http log if verbose is true. 
            // httpLogOptions.setPrettyPrintBody(true);
            // httpLogOptions.setLogLevel(HttpLogDetailLevel.BODY_AND_HEADERS);
        }

        return new OpenAIClientBuilder()       
        .endpoint(endpoint)
                .credential(tokenCredential)
                .httpLogOptions(httpLogOptions)
                .buildAsyncClient();
    }


}

