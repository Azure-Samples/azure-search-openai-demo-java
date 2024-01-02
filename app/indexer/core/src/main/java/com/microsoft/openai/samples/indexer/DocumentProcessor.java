package com.microsoft.openai.samples.indexer;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import com.microsoft.openai.samples.indexer.index.SearchIndexManager;
import com.microsoft.openai.samples.indexer.parser.PDFParser;
import com.microsoft.openai.samples.indexer.parser.Page;
import com.microsoft.openai.samples.indexer.parser.TextSplitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 
    public void indexDocumentfromFile(String filename, String category){
        logger.debug("Indexing file {}", filename);
        File file = new File(filename);

        List<Page> pages = pdfParser.parse(file);
        logger.info("Found {} pages in file {}", pages.size(), file.getName());
        
       
        
        List<SplitPage> splitPages = textSplitter.splitPages(pages);
        logger.info("file {} splitted in {} sections", file.getName(), splitPages.size());

        List<Section> sections = splitPages.stream()
                .map(splitPage -> {
                                    return new Section(splitPage, file.getName(), category);
                                  })
                .collect(Collectors.toList());
       
        searchIndexManager.updateContent(sections);
        logger.info("File {} indexed with {} splitted sections", file.getName(),sections.size()); 

    }


}
