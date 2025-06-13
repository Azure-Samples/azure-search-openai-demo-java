package com.microsoft.openai.samples.indexer.langchain4j.parser;

import com.azure.ai.documentintelligence.DocumentIntelligenceClientBuilder;
import com.azure.core.credential.TokenCredential;
import com.microsoft.openai.samples.indexer.langchain4j.ConfigUtils;
import com.microsoft.openai.samples.indexer.langchain4j.IndexingConfigException;
import com.microsoft.openai.samples.indexer.langchain4j.PipelineContext;
import com.microsoft.openai.samples.indexer.langchain4j.providers.DocumentParserProvider;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentSource;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import org.apache.tika.metadata.Metadata;
import org.slf4j.Logger;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;


/** Default implementation of DocumentParserProvider that provides parsers based on the file extension or URL.
 * It supports multiple parser configurations and resolves the appropriate parser based on the file type at runtime.
 * This implementation uses Apache Tika for general document parsing and Azure Document Intelligence for specific document types.
 */
public class DefaultDocumentParserProvider implements
        DocumentParserProvider {
    private final static Logger LOGGER = org.slf4j.LoggerFactory.getLogger(DefaultDocumentParserProvider.class);
    private final List<ParserConfig> config;
    private final Function<String,Object> DIResolver;
    public DefaultDocumentParserProvider(List<ParserConfig> config, Function<String,Object> dIResolver) {
        this.config  = config;
        this.DIResolver = dIResolver;
    }


    @Override
    public DocumentParser getParser(PipelineContext ctx) {
        DocumentSource documentSource = (DocumentSource) ctx.get("documentSource");
        if (documentSource == null) {
         throw new IndexingConfigException("DocumentSource is not set in the context. Please ensure it is provided before calling getParser.");
        }

        Object filenameOrUrlObject = ctx.get("filename-or-url");
        if( filenameOrUrlObject == null ) {
                throw new IndexingConfigException("filename-or-url is not set in pipeline context. Please ensure it is provided.");
            }

        ParserConfig config = findParserProviderType(filenameOrUrlObject.toString());
        if(config == null) {
            throw new IndexingConfigException("No parser provider found for the file or URL: " + filenameOrUrlObject.toString());
        }

        switch (config.type()) {
            case "azure-document-intelligence":
                return buildAzureDocumentIntelligenceParser(documentSource,config);
            case "apache-tika":
                return buildApacheTika(documentSource,config);
            default:
                return buildApacheTika(documentSource,config);
        }

    }

    private DocumentParser buildApacheTika(DocumentSource documentSource,ParserConfig config) {
        LOGGER.debug("Building Apache Tika parser for: {}", documentSource.metadata().getString("filename"));
        //TODO pass the documentSource metadata to the Apache Tika parser to have a standard set of metadata
        //TODO: make apache tika more configurable

        Supplier<Metadata> metadataSupplier = () -> {
            org.apache.tika.metadata.Metadata tikaMetadata = new org.apache.tika.metadata.Metadata();
            documentSource.metadata().toMap().forEach((k, v) -> {
                if (v != null) {
                    tikaMetadata.set(k, v.toString());
                }
            });
            return tikaMetadata;
        };

        return new ApacheTikaDocumentParser(null,null,metadataSupplier,null,true);

    }
    private DocumentParser buildAzureDocumentIntelligenceParser(DocumentSource documentSource,ParserConfig config) {
        LOGGER.debug("Building Azure Document Intelligence parser for: {}", documentSource.metadata().getString("filename"));

        var serviceName = ConfigUtils.getString("service-name", config.params());
        var identityRef = ConfigUtils.getString("identity-ref", config.params());
        var modelId = ConfigUtils.getString("model-id", config.params());

        var tokenCredential = (TokenCredential) DIResolver.apply(identityRef);

        String endpoint = "https://%s.cognitiveservices.azure.com/".formatted(serviceName);
        /*
        var client = new DocumentAnalysisClientBuilder()
                .endpoint(endpoint)
                .credential(tokenCredential)
                .buildClient();
        */
        var client = new DocumentIntelligenceClientBuilder()
                .endpoint(endpoint)
                .credential(tokenCredential)
                .buildClient();

            return new AzureDocumentIntelligenceDocumentParser(client,documentSource.metadata(),modelId);
    }


    private ParserConfig findParserProviderType(String filenameOrUrl) {
        if(!filenameOrUrl.contains("."))
            throw new IndexingConfigException("Filename must contain an extension: " + filenameOrUrl);
        String ext =  filenameOrUrl.substring(filenameOrUrl.lastIndexOf('.') + 1);

        return config.stream()
                .filter(cfg -> cfg.extension().stream()
                        .map(Pattern::compile)
                        .anyMatch(p -> p.matcher(ext).matches()))
                .findFirst()
                .orElse(null);
    }


}
