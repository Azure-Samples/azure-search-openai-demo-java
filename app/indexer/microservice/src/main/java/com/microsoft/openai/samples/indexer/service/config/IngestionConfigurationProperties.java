package com.microsoft.openai.samples.indexer.service.config;

import com.microsoft.openai.samples.indexer.langchain4j.ConfigUtils;
import com.microsoft.openai.samples.indexer.langchain4j.embedding.EmbeddingModelConfig;
import com.microsoft.openai.samples.indexer.langchain4j.embedding.EmbeddingStoreConfig;
import com.microsoft.openai.samples.indexer.langchain4j.loader.LoaderConfig;
import com.microsoft.openai.samples.indexer.langchain4j.parser.ParserConfig;
import com.microsoft.openai.samples.indexer.langchain4j.splitter.SplitterConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import java.util.List;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "rag")
public class IngestionConfigurationProperties {
    private List<Ingestion> ingestion;

    public List<Ingestion> getIngestion() {
        return ingestion;
    }

    public void setIngestion(List<Ingestion> ingestion) {
        this.ingestion = ingestion;
    }

    public record Ingestion(
        List<String> filename,
        List<String> filetype,
        Loader loader,
        List<Parser> parser,
        DocumentTransformer documentTransformer,
        Splitter splitter,
        TextTransformer textTransformer,
        EmbeddingModel embeddingModel,
        EmbeddingStore embeddingStore
    ) {
        public List<ParserConfig> convertToParserConfigs() {
            return parser.stream()
                    .map(parser -> new ParserConfig(
                            parser.type(),
                            parser.extension(),
                            parser.params()
                    ))
                    .toList();
        }
    }

    public record Loader(
        String type,
        Map<String, String> params
    ) {
        public LoaderConfig convertToLoaderConfig() {
            return new LoaderConfig(
                type,
                params
            );
        }
    }

    public record Parser(
        String type,
        List<String> extension,
        Map<String, String> params
    ) {

        public ParserConfig convertToParserConfigs() {
            return new ParserConfig(
                type,
                extension,
                params
            );
        }
    }

    public record DocumentTransformer(
        String type
    ) {}

    public record Splitter(
        String type,
        Integer chunkSize,
        Integer overlap,
        Map<String, String> params
    ) {
        public SplitterConfig convertToSplitterConfig() {

            return new SplitterConfig(
                type,
                chunkSize,
                overlap,
                ConfigUtils.parseIntOrDefault(params, "sentence-search-limit", 100)
            );
        }
    }

    public record TextTransformer(
        String type
    ) {}

    public record EmbeddingModel(
        String type,
        Integer dimensions,
        Map<String, String> params
    ) {
        public EmbeddingModelConfig convertToEmbeddingModelConfig() {
            return new EmbeddingModelConfig(
                    type,
                    dimensions,
                    params
            );
        }
    }

    public record EmbeddingStore(
        String type,
        Map<String, String> params
    ) {
        public EmbeddingStoreConfig convertToEmbeddingStoreConfig() {
            return new EmbeddingStoreConfig(
                    type,
                    params
            );
        }
    }


}
