package com.microsoft.openai.samples.indexer.service;

import com.microsoft.openai.samples.indexer.DocumentProcessor;
import com.microsoft.openai.samples.indexer.index.SearchIndexManager;
import com.microsoft.openai.samples.indexer.parser.DocumentIntelligencePDFParser;
import com.microsoft.openai.samples.indexer.parser.TextSplitter;
import com.microsoft.openai.samples.indexer.storage.BlobManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class IndexerService {

    private static final Logger logger = LoggerFactory.getLogger(IndexerService.class);

    private final BlobManager blobManager;
    private final DocumentProcessor documentProcessor;

    public IndexerService(SearchIndexManager searchIndexManager, DocumentIntelligencePDFParser documentIntelligencePDFParser, BlobManager blobManager){
       this.blobManager = blobManager;
       this.documentProcessor = new DocumentProcessor(searchIndexManager, documentIntelligencePDFParser, new TextSplitter(false));
    }
    public void indexBlobDocument(String bloburl) throws IOException {
        logger.debug("Indexer: Processing blob document {}", bloburl);
        String filename = bloburl.substring(bloburl.lastIndexOf("/") + 1);
        documentProcessor.indexDocumentFromBytes(filename, "", blobManager.getFileAsBytes(filename));
    }
}
