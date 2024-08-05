package com.microsoft.openai.samples.indexer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import com.microsoft.openai.samples.indexer.index.SearchIndexManager;
import com.microsoft.openai.samples.indexer.parser.PDFParser;
import com.microsoft.openai.samples.indexer.parser.Page;
import com.microsoft.openai.samples.indexer.parser.TextSplitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The DocumentProcessor class is responsible for processing and indexing documents.
 * It takes a document as input, either as a file or as a byte array, and processes it for indexing.
 * The processing involves
 * 1. parsing the document into pages
 * 2. splitting the pages into sections
 * 3. Indexing these sections in Azure AI Search also adding embeddings so that semantic similarity search can be used.
 * The class uses a SearchIndexManager to manage the indexing, a PDFParser to parse the document into pages, and a TextSplitter to split the pages into sections.
 */
public class DocumentProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(DocumentProcessor.class);
    
    private SearchIndexManager searchIndexManager;
    private PDFParser pdfParser;
    private TextSplitter textSplitter;
    
    
    public DocumentProcessor(SearchIndexManager searchIndexManager, PDFParser pdfParser, TextSplitter textSplitter) {
        this.searchIndexManager = searchIndexManager;
        this.pdfParser = pdfParser;
        this.textSplitter = textSplitter;
    }
 
    public void indexDocumentfromFile(String filepath, String category) throws IOException {
        Path path = Path.of(filepath);
        byte[] bytes = Files.readAllBytes(path);
       indexDocumentFromBytes(path.getFileName().toString(), category, bytes);

    }

    public void indexDocumentFromBytes(String filename, String category, byte[] content){
        logger.debug("Indexing file {}", filename);
        //TODO add support for other file types (docx, pptx, txt, md, html, etc)
        List<Page> pages = pdfParser.parse(content);
        logger.info("Found {} pages in file {}", pages.size(), filename);



        List<SplitPage> splitPages = textSplitter.splitPages(pages);
        logger.info("file {} splitted in {} sections", filename, splitPages.size());

        List<Section> sections = splitPages.stream()
                .map(splitPage -> {
                                    return new Section(splitPage, filename, category);
                                  })
                .collect(Collectors.toList());

        searchIndexManager.updateContent(sections);
        logger.info("File {} indexed with {} splitted sections", filename,sections.size());

    }


}
