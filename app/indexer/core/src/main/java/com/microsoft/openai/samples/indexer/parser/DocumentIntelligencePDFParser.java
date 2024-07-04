
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
        List<Page> pages = new ArrayList<>();
        SyncPoller<OperationResult, AnalyzeResult> analyzeLayoutResultPoller =
            client.beginAnalyzeDocument(this.modelId, fileData);

        AnalyzeResult analyzeLayoutResult = analyzeLayoutResultPoller.getFinalResult();

        int offset = 0;
        for (int page_num = 0; page_num < analyzeLayoutResult.getPages().size(); page_num++) {
            DocumentPage page = analyzeLayoutResult.getPages().get(page_num);
            List<DocumentTable> tables_on_page = new ArrayList<>();

            if(analyzeLayoutResult.getTables() != null){
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
            int[] tableChars = new int[pageLength];
            Arrays.fill(tableChars, -1);

            for (int tableId = 0; tableId < tables_on_page.size(); tableId++) {
                DocumentTable table = tables_on_page.get(tableId);
                
                for (DocumentSpan span : table.getSpans()) {
                    for (int i = 0; i < span.getLength(); i++) {
                        int idx = span.getOffset() - pageOffset + i;
                        if (idx >= 0 && idx < pageLength) {
                            tableChars[idx] = tableId;
                        }
                    }
                }
            }

            StringBuilder pageText = new StringBuilder();
            Set<Integer> addedTables = new HashSet<>();
            for (int idx = 0; idx < tableChars.length; idx++) {
                int tableId = tableChars[idx];
                if (tableId == -1) {
                    pageText.append(analyzeLayoutResult.getContent().substring(pageOffset + idx, pageOffset + idx + 1));
                } else if (!addedTables.contains(tableId)) {
                    DocumentTable table = tables_on_page.get(tableId);
                    pageText.append(tableToHtml(table));
                    addedTables.add(tableId);
                }
            }

            pages.add( new Page(page_num, offset, pageText.toString()));
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