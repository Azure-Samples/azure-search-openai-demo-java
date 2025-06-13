package com.microsoft.openai.samples.indexer.service.langchain4j;

import com.microsoft.openai.samples.indexer.langchain4j.IndexingConfigException;
import com.microsoft.openai.samples.indexer.langchain4j.IndexingProcessingException;
import com.microsoft.openai.samples.indexer.langchain4j.Langchain4JIndexingPipeline;
import com.microsoft.openai.samples.indexer.langchain4j.embedding.EmbeddingModelConfig;
import com.microsoft.openai.samples.indexer.langchain4j.embedding.EmbeddingModelProviderFactory;
import com.microsoft.openai.samples.indexer.langchain4j.embedding.EmbeddingStoreConfig;
import com.microsoft.openai.samples.indexer.langchain4j.embedding.EmbeddingStoreProviderFactory;
import com.microsoft.openai.samples.indexer.langchain4j.loader.LoaderConfig;
import com.microsoft.openai.samples.indexer.langchain4j.loader.LoaderProviderFactory;
import com.microsoft.openai.samples.indexer.langchain4j.parser.DefaultDocumentParserProvider;
import com.microsoft.openai.samples.indexer.langchain4j.parser.ParserConfig;
import com.microsoft.openai.samples.indexer.langchain4j.providers.DocumentSourceProvider;
import com.microsoft.openai.samples.indexer.langchain4j.providers.DocumentSplitterProvider;
import com.microsoft.openai.samples.indexer.langchain4j.providers.EmbeddingModelProvider;
import com.microsoft.openai.samples.indexer.langchain4j.providers.EmbeddingStoreProvider;
import com.microsoft.openai.samples.indexer.langchain4j.splitter.DocumentSplitterProviderFactory;
import com.microsoft.openai.samples.indexer.langchain4j.splitter.SplitterConfig;
import com.microsoft.openai.samples.indexer.service.config.IngestionConfigurationProperties;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class Langchain4JIndexerService {
    private static final Logger LOGGER = LoggerFactory.getLogger(Langchain4JIndexerService.class);
    private final IngestionConfigurationProperties config;
    private final ApplicationContext applicationContext;

    public Langchain4JIndexerService(IngestionConfigurationProperties config,
                                     ApplicationContext applicationContext) {
        this.config = config;
        this.applicationContext = applicationContext;
    }


    public void index(String filenameOrUrl, Metadata metadata) {
        LOGGER.info("Search ingestion config for {}",filenameOrUrl);
        IngestionConfigurationProperties.Ingestion selectedConfig = null;
        try {
            selectedConfig = findBestConfig(filenameOrUrl);
        } catch (Exception ex) {
            throw new IndexingConfigException("Error while finding best config for: " + filenameOrUrl, ex);
        }

        if ( selectedConfig == null) {
            throw new IndexingConfigException("No suitable configuration found for: " + filenameOrUrl);
        }

        LOGGER.debug("Ingestion config found for [{}] is {}", filenameOrUrl,selectedConfig);
        var langchain4JIndexingPipeline = Langchain4JIndexingPipeline.builder()
                .documentSourceProvider(buildDocumentSourceProvider(selectedConfig.loader().convertToLoaderConfig()))
                .parserProvider(buildDocumentParserProvider(selectedConfig.convertToParserConfigs()))
                .splitterProvider(buildDocumentSplitterProvider(selectedConfig.splitter().convertToSplitterConfig()))
                .embeddingModelProvider(buildEmbeddingModelProvider(selectedConfig.embeddingModel().convertToEmbeddingModelConfig()))
                .embeddingStoreProvider(buildEmbeddingStoreProvider(selectedConfig.embeddingStore().convertToEmbeddingStoreConfig()))
                .build();

        try {
            langchain4JIndexingPipeline.addIndexedFile(filenameOrUrl,metadata);
        } catch (IOException ex) {
            throw new IndexingProcessingException("Error while indexing file "+filenameOrUrl,ex);
        }

    }

    DocumentSourceProvider buildDocumentSourceProvider(LoaderConfig config) {
        return new LoaderProviderFactory(applicationContext::getBean).create(config);
    }

    DefaultDocumentParserProvider buildDocumentParserProvider(List<ParserConfig> parserConfigs) {

        return new DefaultDocumentParserProvider(
                parserConfigs,
                applicationContext::getBean);

    }

    DocumentSplitterProvider buildDocumentSplitterProvider(SplitterConfig config) {
        return new DocumentSplitterProviderFactory(applicationContext::getBean).create(config);

    }

    EmbeddingModelProvider buildEmbeddingModelProvider(EmbeddingModelConfig config) {
        return new EmbeddingModelProviderFactory(applicationContext::getBean).create(config);

    }

    EmbeddingStoreProvider<TextSegment> buildEmbeddingStoreProvider(EmbeddingStoreConfig config) {
        return new EmbeddingStoreProviderFactory(applicationContext::getBean).create(config);
    }

    private IngestionConfigurationProperties.Ingestion findBestConfig(String pathOrUrl) {

        String ext = pathOrUrl.contains(".")
                ? pathOrUrl.substring(pathOrUrl.lastIndexOf('.') + 1)
                : "";
        return config.getIngestion().stream()
                .filter(cfg -> cfg.filename().stream()
                        .map(Pattern::compile)
                        .anyMatch(p -> p.matcher(pathOrUrl).matches()))
                .filter(cfg -> cfg.filetype().stream()
                        .map(Pattern::compile)
                        .anyMatch(p -> p.matcher(ext).matches()))
                .findFirst()
                .orElse(null );
    }

}
