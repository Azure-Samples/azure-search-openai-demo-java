package com.microsoft.openai.samples.indexer.langchain4j.embedding;

import com.azure.core.credential.TokenCredential;
import com.microsoft.openai.samples.indexer.langchain4j.ConfigUtils;
import com.microsoft.openai.samples.indexer.langchain4j.IndexingConfigException;
import com.microsoft.openai.samples.indexer.langchain4j.providers.EmbeddingModelProvider;
import dev.langchain4j.model.azure.AzureOpenAiEmbeddingModel;

import java.util.function.Function;

/**
 * Factory class to create instances of EmbeddingModelProvider based on EmbeddingModelConfig.
 * Currently supports Azure OpenAI embedding model.
 */
public class EmbeddingModelProviderFactory {
    private final Function<String, Object> DIResolver;

    public EmbeddingModelProviderFactory(Function<String, Object> diResolver) {
        DIResolver = diResolver;
    }

   public EmbeddingModelProvider create(EmbeddingModelConfig config) {
        if (config == null || config.type() == null) {
            throw new IndexingConfigException("EmbeddingModelConfig and its type must not be null");
        }

        switch (config.type()) {
            case "langchain4j-azure-open-ai-model":
                return buildLangchain4JAzureOpenAI(config);
            default:
                throw new IllegalArgumentException("Unsupported embedding model type: " + config.type());
        }

    }

    private EmbeddingModelProvider buildLangchain4JAzureOpenAI(EmbeddingModelConfig config) {
        var azureOpenAIServiceName = ConfigUtils.getString("service-name", config.params());
        var azureOpenAIDeploymentName = ConfigUtils.getString("embedding-model-deployment", config.params());
        var identityRef = ConfigUtils.getString("identity-ref", config.params());
        var tokenCredential = (TokenCredential) DIResolver.apply(identityRef);
        var dimensions = ConfigUtils.parseIntOrDefault(config.params(),"dimensions",3072 );

        String endpoint = "https://%s.openai.azure.com".formatted(azureOpenAIServiceName);
        return ctx -> AzureOpenAiEmbeddingModel.builder()
                .tokenCredential(tokenCredential)
                .endpoint(endpoint)
                .deploymentName(azureOpenAIDeploymentName)
                .dimensions(dimensions)
                .build();

        }

}
