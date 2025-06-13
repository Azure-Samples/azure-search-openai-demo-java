package com.microsoft.openai.samples.indexer.langchain4j;


import com.microsoft.openai.samples.indexer.langchain4j.providers.*;
import dev.langchain4j.data.document.*;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.data.segment.TextSegmentTransformer;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class Langchain4JIndexingPipeline {

    private static final Logger LOGGER = LoggerFactory.getLogger(Langchain4JIndexingPipeline.class);

    private final DocumentSourceProvider documentSourceProvider;
    private final DocumentParserProvider parserProvider;
    private final DocumentTransformerProvider docTransformerProvider;
    private final DocumentSplitterProvider splitterProvider;
    private final TextSegmentTransformerProvider textTransformerProvider;
    private final EmbeddingModelProvider embeddingModelProvider;
    private final EmbeddingStoreProvider<TextSegment> embeddingStoreProvider;

    //TODO to evaluate if the custom logic (like the paged document for pdf) can be implemented using EmbeddingStoreIngestor
    public Langchain4JIndexingPipeline(DocumentSourceProvider documentSourceProvider,
                                       DocumentParserProvider parserProvider,
                                       DocumentTransformerProvider docTransformerProvider,
                                       DocumentSplitterProvider splitterProvider,
                                       TextSegmentTransformerProvider textTransformerProvider,
                                       EmbeddingModelProvider embeddingModelProvider,
                                       EmbeddingStoreProvider<TextSegment> embeddingStoreProvider) {
        if(documentSourceProvider == null) {
            throw new IllegalArgumentException("DocumentSource cannot be null");
        }

        if(splitterProvider == null) {
            throw new IllegalArgumentException("DocumentSplitterProvider cannot be null");
        }

        if(embeddingModelProvider == null) {
            throw new IllegalArgumentException("EmbeddingModelProvider cannot be null");
        }

        if(embeddingStoreProvider == null) {
            throw new IllegalArgumentException("EmbeddingStoreProvider cannot be null");
        }
        this.documentSourceProvider = documentSourceProvider;
        this.parserProvider = parserProvider;
        this.docTransformerProvider = docTransformerProvider;
        this.splitterProvider = splitterProvider;
        this.textTransformerProvider = textTransformerProvider;
        this.embeddingModelProvider = embeddingModelProvider;
        this.embeddingStoreProvider = embeddingStoreProvider;
    }

    public void addIndexedFile(String filenameOrUrl, Metadata indexingMetadata) throws IOException {
        PipelineContext ctx = new PipelineContext();
        LOGGER.info("Starting indexing process for file {} with Metadata {}", filenameOrUrl,indexingMetadata);
        /**
         * Unlike langchain4j Loader concept which both loads and parses a file, we just load the file content here
         * and returning a DocumentSource.
         */

        ctx.put("filename-or-url",filenameOrUrl);
        var documentSource = loadDocumentSource(ctx);
        LOGGER.info("Processing document source. {}", documentSource.metadata());

        /**
         * Adding indexing metadata to the context.
         * It can be used for documents access control list metadata like user id, group id, role or filtering like category,etc.
         */
        documentSource.metadata().putAll(indexingMetadata.toMap());

        //make documentsource available to other steps in the pipeline
        ctx.put("documentSource", documentSource);

        var document = parse(ctx, documentSource);
        //make document available to other steps in the pipeline
        ctx.put("document", document);

        var transformedDocument = transformDocument(ctx,document);
        //make transformed document available to other steps in the pipeline
        ctx.put("transformedDocument", document);

        var textSegments = split(ctx,transformedDocument);

        var transformedTextSegments = transformText(ctx,textSegments);
        var embeddings = embed(ctx,transformedTextSegments);
        store(ctx,transformedTextSegments, embeddings);

        LOGGER.info("Indexing process completed for document source. {}", documentSource.metadata());
    }

    protected DocumentSource loadDocumentSource(PipelineContext ctx) {
        var documentSource = documentSourceProvider.getSource(ctx);

        if (documentSource == null) {
            throw new IllegalStateException("DocumentSource is not set. Please ensure it is provided before calling loadDocumentSource.");
        }
        return documentSource;
    }

    protected Document parse(PipelineContext ctx, DocumentSource documentSource)throws IOException {
        var documentParser = parserProvider.getParser(ctx);
        Document document;
        try (var inputStream = documentSource.inputStream()) {
            document = documentParser.parse(inputStream);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse document from source: " + documentSource.metadata(), e);
        }

        LOGGER.info("Parsed [{}] chars for document {}", document.text().length(), document.metadata());
        return document;
    }

    protected Document transformDocument(PipelineContext ctx, Document document) {
        if( docTransformerProvider == null){
            LOGGER.debug("No document transformer provided, skipping transformation.");
            return  document;
        }

        DocumentTransformer docTransformer = this.docTransformerProvider.getTransformer(ctx);
        LOGGER.debug("Using document transformer: {}", docTransformer.getClass().getSimpleName());

        Document transformedDocument = docTransformer.transform(document);
        LOGGER.info("Tranformed [{}] chars for document {}", transformedDocument.text().length(), transformedDocument.metadata());
        return transformedDocument;
    }

    protected List<TextSegment> split(PipelineContext ctx, Document document) {
        DocumentSplitter splitter = this.splitterProvider.getSplitter(ctx);
        LOGGER.debug("Using document splitter: {}", splitterProvider.getClass().getSimpleName());
        List<TextSegment> textSegments = new ArrayList<>();

        if(document instanceof PagedDocument pagedDocument){
            LOGGER.debug("Splitting paged document with {} pages", pagedDocument.getAllPages().size());
            textSegments = splitter.splitAll(pagedDocument.getAllPages());
        } else {
            LOGGER.debug("Splitting default document");
            textSegments = splitter.split(document);
        }

        LOGGER.info("Split document into {} text segments", textSegments.size());

        return textSegments;
    }

    protected List<TextSegment> transformText(PipelineContext ctx, List<TextSegment> textSegments) {
        if( textTransformerProvider == null){
            LOGGER.debug("No text segment transformer provided, skipping transformation.");
            return textSegments;
        }

        TextSegmentTransformer textSegmentTransformer = this.textTransformerProvider.getTransformer(ctx);
        LOGGER.debug("Using text segment transformer: {}", textSegmentTransformer.getClass().getSimpleName());

        List<TextSegment> transformedTextSegments = textSegmentTransformer.transformAll(textSegments);
        LOGGER.info("Tranformed [{}] segments for document", transformedTextSegments.size());
        return transformedTextSegments;
    }

    protected List<Embedding> embed(PipelineContext ctx, List<TextSegment> textSegments) {
       EmbeddingModel embeddingModel = this.embeddingModelProvider.getEmbeddingModel(ctx);
        LOGGER.debug("Using embedding model: {}", embeddingModel.getClass().getSimpleName());

        List<Embedding> embeds = embeddingModel.embedAll(textSegments).content();
        LOGGER.info("Generated {} embeddings", embeds.size());
        return embeds;
    }

    protected void store(PipelineContext ctx, List<TextSegment> textSegments, List<Embedding> embeddings) {
        EmbeddingStore<TextSegment> embeddingStore = this.embeddingStoreProvider.getEmbeddingStore(ctx);
        LOGGER.debug("Using embedding store: {}", embeddingStore.getClass().getSimpleName());

        embeddingStore.addAll(embeddings, textSegments);
        LOGGER.info("Stored {} embeddings in the embedding store", embeddings.size());
    }



    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private DocumentSourceProvider documentSourceProvider;
        private DocumentParserProvider parserProvider;
        private DocumentTransformerProvider docTransformerProvider;
        private DocumentSplitterProvider splitterProvider;
        private TextSegmentTransformerProvider textTransformerProvider;
        private EmbeddingModelProvider embeddingModelProvider;
        private EmbeddingStoreProvider<TextSegment> embeddingStoreProvider;

        public Builder documentSourceProvider(DocumentSourceProvider l) { this.documentSourceProvider = l; return this; }
        public Builder parserProvider(DocumentParserProvider p) { this.parserProvider = p; return this; }
        public Builder docTransformerProvider(DocumentTransformerProvider t) { this.docTransformerProvider = t; return this; }
        public Builder splitterProvider(DocumentSplitterProvider s) { this.splitterProvider = s; return this; }
        public Builder textTransformerProvider(TextSegmentTransformerProvider t) { this.textTransformerProvider = t; return this; }
        public Builder embeddingModelProvider(EmbeddingModelProvider m) { this.embeddingModelProvider = m; return this; }
        public Builder embeddingStoreProvider(EmbeddingStoreProvider<TextSegment> s) { this.embeddingStoreProvider = s; return this; }

        public Langchain4JIndexingPipeline build() {
            return new Langchain4JIndexingPipeline(
                documentSourceProvider, parserProvider, docTransformerProvider,
                splitterProvider, textTransformerProvider,
                embeddingModelProvider, embeddingStoreProvider);
        }
    }
}

