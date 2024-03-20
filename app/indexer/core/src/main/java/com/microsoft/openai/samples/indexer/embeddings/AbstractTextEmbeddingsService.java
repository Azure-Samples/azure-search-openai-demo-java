package com.microsoft.openai.samples.indexer.embeddings;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.azure.ai.openai.OpenAIAsyncClient;
import com.azure.ai.openai.models.Embeddings;
import com.azure.ai.openai.models.EmbeddingItem;
import com.azure.ai.openai.models.EmbeddingsOptions;
import com.azure.core.exception.HttpResponseException;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;

import com.knuddels.jtokkit.api.ModelType;
import reactor.util.retry.Retry;

public abstract class AbstractTextEmbeddingsService implements TextEmbeddingsService{
    protected String openAiDeploymentName;
    protected boolean disableBatch;
    protected boolean verbose;
    protected Integer batchMaxSize;
    protected Integer batchTokenLimit;
    Encoding encoding;

    private static final Logger logger = LoggerFactory.getLogger(AbstractTextEmbeddingsService.class); 

    public AbstractTextEmbeddingsService(String openAiDeploymentName, boolean verbose,Integer batchMaxSize, Integer batchTokenLimit) {
        this.openAiDeploymentName = openAiDeploymentName;
        this.verbose = verbose;
        this.batchMaxSize = batchMaxSize;
        this.batchTokenLimit = batchTokenLimit;
        
        EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
        this.encoding = registry.getEncodingForModel(ModelType.TEXT_EMBEDDING_ADA_002);

    }

    protected abstract OpenAIAsyncClient createClient();


    public List<List<Double>> createEmbeddingBatch(List<String> texts) {
        List<EmbeddingBatch> batches = splitTextIntoBatches(texts);
        List<List<Double>> embeddings = new ArrayList<>();
        OpenAIAsyncClient client = createClient();
        for (int batchIndex =0; batchIndex < batches.size(); batchIndex++) {
                    EmbeddingBatch batch = batches.get(batchIndex);
                    EmbeddingsOptions embeddingsOptions = new EmbeddingsOptions(batch.getTexts());
                    embeddingsOptions.setUser("java-documents-ingestion-process");
                    Embeddings embResponse =  client.getEmbeddings(openAiDeploymentName, embeddingsOptions)
                    .retryWhen( Retry.backoff(10, Duration.ofSeconds(30)).jitter(0.75)
                    .filter( throwable -> {
                        if (throwable instanceof HttpResponseException ) {
                            HttpResponseException error = (HttpResponseException) throwable;
                            if (error.getResponse().getStatusCode() == 429) {
                                logger.error("Too many requests to embedding service. Retry in 30 seconds");
                                return true;
                            }
                        }
                        return false;
                    }))
                    
                    .block();
                  
                    for (EmbeddingItem data : embResponse.getData()) {
                        embeddings.add(data.getEmbedding());
                    }

                    logger.info("Embedding batch[%d] of [%d] completed. Batch size [%d] Token count [%d]".formatted(batchIndex+1, batches.size(), batch.getTexts().size(), batch.getTokenLength()));

                   
            }
        
        return embeddings;
    }

    protected List<EmbeddingBatch> splitTextIntoBatches(List<String> texts) {
        
        List<EmbeddingBatch> batches = new ArrayList<>();
        List<String> batch = new ArrayList<>();
        int batchTokenLength = 0;
        for (String text : texts) {
            int textTokenLength =  encoding.countTokens(text);
            if (batchTokenLength + textTokenLength >= this.batchTokenLimit && !batch.isEmpty()) {
                batches.add(new EmbeddingBatch(batch, batchTokenLength));
                batch = new ArrayList<>();
                batchTokenLength = 0;
            }

            batch.add(text);
            batchTokenLength += textTokenLength;
            if (batch.size() == this.batchMaxSize) {
                batches.add(new EmbeddingBatch(batch, batchTokenLength));
                batch = new ArrayList<>();
                batchTokenLength = 0;
            }
        }

        if (!batch.isEmpty()) {
            batches.add(new EmbeddingBatch(batch, batchTokenLength));
        }

        return batches;
    }
}

