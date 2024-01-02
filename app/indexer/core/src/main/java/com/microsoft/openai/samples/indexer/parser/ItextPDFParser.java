package com.microsoft.openai.samples.indexer.parser;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

public class ItextPDFParser implements PDFParser {
    @Override
    public List<Page> parse(File file) {
        List<Page> pages = new ArrayList<>();
        PdfReader reader = null;

        try {
            reader = new PdfReader(file.getAbsolutePath());
            Integer offset = 0;
            for (int i = 1; i <= reader.getNumberOfPages(); i++) {
                String pageText = PdfTextExtractor.getTextFromPage(reader, i);
                Page page = new Page(i, offset, pageText);
                offset += pageText.length();
                pages.add(page);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        return pages;
    }
}
    