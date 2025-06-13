package com.microsoft.openai.samples.indexer.langchain4j.parser;

import com.azure.ai.documentintelligence.DocumentIntelligenceClient;

import com.azure.ai.documentintelligence.models.*;
import com.azure.core.util.BinaryData;
import com.azure.core.util.polling.SyncPoller;
import com.microsoft.openai.samples.indexer.langchain4j.DefaultPagedDocument;
import dev.langchain4j.data.document.DefaultDocument;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.Metadata;
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.*;

public class AzureDocumentIntelligenceDocumentParser implements DocumentParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(AzureDocumentIntelligenceDocumentParser.class);
    private static final String PAGE_NUMBER_METADATA_KEY = "page_number";
    private static final String PAGE_OFFSET_METADATA_KEY = "page_offset";
    private final Metadata documentMetadata;
    private final DocumentIntelligenceClient client;
    private String modelId = "prebuilt-layout";



    public AzureDocumentIntelligenceDocumentParser(DocumentIntelligenceClient client, Metadata documentMetadata) {
        this.client = client;
        this.documentMetadata = documentMetadata;
    }

    public AzureDocumentIntelligenceDocumentParser(DocumentIntelligenceClient client,Metadata documentMetadata,String modelId) {
       this(client, documentMetadata);
        this.modelId = modelId;
    }

    @Override
    public Document parse(InputStream inputStream) {

        List<Document> pages = new ArrayList<>();
        LOGGER.info("Starting document analysis with modelId: {}", this.modelId);
        // Begin the document analysis process using Azure's Document Intelligence service
        SyncPoller<AnalyzeOperationDetails, AnalyzeResult> analyzeLayoutResultPoller =
                client.beginAnalyzeDocument(this.modelId,  new AnalyzeDocumentOptions(BinaryData.fromStream(inputStream)));
        // Get the final result of the document analysis
        AnalyzeResult analyzeLayoutResult = analyzeLayoutResultPoller.getFinalResult();
        LOGGER.info("Document analysis completed. Total pages: {}", analyzeLayoutResult.getPages().size());

        int offset = 0;
        // Loop through each page in the analyzed document
        for (int page_num = 0; page_num < analyzeLayoutResult.getPages().size(); page_num++) {
            DocumentPage page = analyzeLayoutResult.getPages().get(page_num);
            LOGGER.debug("Processing page {}", page_num + 1);

            // Create a list to store the tables on the current page
            List<DocumentTable> tables_on_page = new ArrayList<>();

            // If there are tables in the analyzed document, add the tables on the current page to the list
            if (analyzeLayoutResult.getTables() != null) {
                for (DocumentTable table : analyzeLayoutResult.getTables()) {
                    BoundingRegion boundingRegion = table.getBoundingRegions().get(0);
                    if (boundingRegion.getPageNumber() == page_num + 1) {
                        tables_on_page.add(table);
                        LOGGER.debug("Table found on page {}: {} rows, {} columns", page_num + 1, table.getRowCount(), table.getColumnCount());
                    }
                }
            }

            DocumentSpan pageSpan = page.getSpans().get(0);
            int pageOffset = pageSpan.getOffset();
            int pageLength = pageSpan.getLength();
            LOGGER.debug("Page {} offset: {}, length: {}", page_num + 1, pageOffset, pageLength);

            // Create an array to store the characters in the tables on the current page
            int[] tableChars = new int[pageLength];
            Arrays.fill(tableChars, -1);

            // Loop through each table on the current page
            for (int tableId = 0; tableId < tables_on_page.size(); tableId++) {
                DocumentTable table = tables_on_page.get(tableId);
                LOGGER.debug("Marking characters for table {} on page {}", tableId, page_num + 1);
                // Loop through each span in the current table and mark the characters in the table
                for (DocumentSpan span : table.getSpans()) {
                    for (int i = 0; i < span.getLength(); i++) {
                        int idx = span.getOffset() - pageOffset + i;
                        // If the character is in the current table, store the table ID in the array
                        if (idx >= 0 && idx < pageLength) {
                            tableChars[idx] = tableId;
                        }
                    }
                }
            }

            // Create a StringBuilder to store the text of the current page
            StringBuilder pageText = new StringBuilder();

            // Create a set to store the IDs of the tables that have been added to the page text
            Set<Integer> addedTables = new HashSet<>();

            // Loop through each character in the array
            for (int idx = 0; idx < tableChars.length; idx++) {
                int tableId = tableChars[idx];
                if (tableId == -1) {
                    // If the character is not in a table, add it to the page text
                    pageText.append(analyzeLayoutResult.getContent().substring(pageOffset + idx, pageOffset + idx + 1));
                } else if (!addedTables.contains(tableId)) {
                    // If the character is in a table and the table has not been added to the page text, add the table to the page text
                    DocumentTable table = tables_on_page.get(tableId);
                    pageText.append(tableToHtml(table));
                    addedTables.add(tableId);
                    LOGGER.debug("Table {} HTML added to page {}", tableId, page_num + 1);
                }
            }

            //copy document metadata to page metadata
            Metadata pageMetadata = documentMetadata.copy();
            //adding specific metadata for the page
            pageMetadata.put(PAGE_NUMBER_METADATA_KEY, page_num+1 );
            pageMetadata.put(PAGE_OFFSET_METADATA_KEY, offset);

            var document = new DefaultDocument(pageText.toString(),pageMetadata);
            pages.add(document);
            LOGGER.debug("Page {} processed. Length: {}", page_num + 1, pageText.length());
            offset += pageText.length();
        }

        LOGGER.info("paged document source created with {} pages.",pages.size());
        return new DefaultPagedDocument(documentMetadata,pages);
    }


    private String tableToHtml(DocumentTable table) {
        LOGGER.debug("Converting table to HTML: {} rows, {} columns", table.getRowCount(), table.getColumnCount());
        StringBuilder tableHtml = new StringBuilder("<table>");
        List<List<DocumentTableCell>> rows = new ArrayList<>();
        for (int i = 0; i < table.getRowCount(); i++) {
            List<DocumentTableCell> rowCells = new ArrayList<>();
            for (DocumentTableCell cell : table.getCells()) {
                if (cell.getRowIndex() == i) {
                    rowCells.add(cell);
                }
            }
            rows.add(rowCells);
        }

        for (List<DocumentTableCell> rowCells : rows) {
            tableHtml.append("<tr>");
            for (DocumentTableCell cell : rowCells) {
                String tag = "td";
                if(cell.getKind() != null)
                     tag = (cell.getKind().equals("columnHeader") || cell.getKind().equals("rowHeader")) ? "th" : "td";

                String cellSpans = "";
                if (cell.getColumnSpan() != null && cell.getColumnSpan() > 1) {
                    cellSpans += " colSpan=" + cell.getColumnSpan();
                }
                if (cell.getRowSpan() != null && cell.getRowSpan() > 1) {
                    cellSpans += " rowSpan=" + cell.getRowSpan();
                }
                tableHtml.append("<").append(tag).append(cellSpans).append(">")
                        .append(StringEscapeUtils.escapeHtml4(cell.getContent()))
                        .append("</").append(tag).append(">");
            }
            tableHtml.append("</tr>");
        }
        tableHtml.append("</table>");
        return tableHtml.toString();
    }
}
