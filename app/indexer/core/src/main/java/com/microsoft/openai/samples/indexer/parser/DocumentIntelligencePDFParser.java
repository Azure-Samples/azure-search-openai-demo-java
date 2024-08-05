
package com.microsoft.openai.samples.indexer.parser;


import com.azure.ai.formrecognizer.documentanalysis.DocumentAnalysisClient;
import com.azure.ai.formrecognizer.documentanalysis.DocumentAnalysisClientBuilder;

import com.azure.ai.formrecognizer.documentanalysis.models.AnalyzeResult;

import com.azure.ai.formrecognizer.documentanalysis.models.DocumentPage;
import com.azure.ai.formrecognizer.documentanalysis.models.BoundingRegion;
import com.azure.ai.formrecognizer.documentanalysis.models.DocumentSpan;
import com.azure.ai.formrecognizer.documentanalysis.models.DocumentTableCell;


import com.azure.ai.formrecognizer.documentanalysis.models.DocumentTable;
import com.azure.ai.formrecognizer.documentanalysis.models.OperationResult;
import com.azure.core.credential.TokenCredential;
import com.azure.core.util.BinaryData;
import com.azure.core.util.polling.SyncPoller;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This is an implementation of a PDF parser using Azure's Document Intelligence service.
 * It is designed to extract text and table data from PDF files and convert them into a structured format.
 *
 * It initializes an instance of DocumentAnalysisClient from Azure's Document Intelligence service in the constructor.
 * It provides two parse methods, one accepting a File object and another accepting a byte array. Both methods convert the input into BinaryData and pass it to a private parse method.
 * The private parse method sends the BinaryData to Azure's Document Intelligence service for analysis. It then processes the analysis result, extracting text and table data from each page of the PDF. Tables are converted into HTML format.
 * The tableToHtml method is used to convert a DocumentTable object into an HTML table. It handles row and column spans and escapes any HTML characters in the cell content.
 */
public class DocumentIntelligencePDFParser implements PDFParser {
       private static final Logger logger = LoggerFactory.getLogger(DocumentIntelligencePDFParser.class); 

    private final DocumentAnalysisClient  client;
    private boolean verbose = false;
    private String modelId = "prebuilt-layout";


    public DocumentIntelligencePDFParser(String serviceName, TokenCredential tokenCredential, Boolean verbose) {
        this.client = new DocumentAnalysisClientBuilder()
                .endpoint("https://%s.cognitiveservices.azure.com/".formatted(serviceName))
                .credential(tokenCredential)
                .buildClient();
        this.verbose = verbose;
    }


    @Override
    public List<Page> parse(File file) {
        if (verbose) {
            logger.info("Extracting text from {} using Azure Document Intelligence", file.getName());
        }

        Path filePath = file.toPath();
        BinaryData fileData = BinaryData.fromFile(filePath, (int) file.length());
        return parse(fileData);
    }

    @Override
    public List<Page> parse(byte[] content) {
        BinaryData fileData = BinaryData.fromBytes(content);
        return parse(fileData);
    }

    private List<Page> parse(BinaryData fileData) {
        // Create a list to store the pages of the PDF
        List<Page> pages = new ArrayList<>();

        // Begin the document analysis process using Azure's Document Intelligence service
        SyncPoller<OperationResult, AnalyzeResult> analyzeLayoutResultPoller =
                client.beginAnalyzeDocument(this.modelId, fileData);

        // Get the final result of the document analysis
        AnalyzeResult analyzeLayoutResult = analyzeLayoutResultPoller.getFinalResult();

        int offset = 0;
        // Loop through each page in the analyzed document
        for (int page_num = 0; page_num < analyzeLayoutResult.getPages().size(); page_num++) {
            DocumentPage page = analyzeLayoutResult.getPages().get(page_num);

            // Create a list to store the tables on the current page
            List<DocumentTable> tables_on_page = new ArrayList<>();

            // If there are tables in the analyzed document, add the tables on the current page to the list
            if (analyzeLayoutResult.getTables() != null) {
                for (DocumentTable table : analyzeLayoutResult.getTables()) {
                    BoundingRegion boundingRegion = table.getBoundingRegions().get(0);
                    if (boundingRegion.getPageNumber() == page_num + 1) {
                        tables_on_page.add(table);
                    }
                }
            }

            DocumentSpan pageSpan = page.getSpans().get(0);
            int pageOffset = pageSpan.getOffset();
            int pageLength = pageSpan.getLength();

            // Create an array to store the characters in the tables on the current page
            int[] tableChars = new int[pageLength];
            Arrays.fill(tableChars, -1);

            // Loop through each table on the current page
            for (int tableId = 0; tableId < tables_on_page.size(); tableId++) {
                DocumentTable table = tables_on_page.get(tableId);

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
                }
            }

            // Add the current page to the list of pages
            pages.add(new Page(page_num, offset, pageText.toString()));

            offset += pageText.length();

        }
        return pages;
    }
                    

    private String tableToHtml(DocumentTable table) {
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
                String tag = (cell.getKind().equals("columnHeader") || cell.getKind().equals("rowHeader")) ? "th" : "td";
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