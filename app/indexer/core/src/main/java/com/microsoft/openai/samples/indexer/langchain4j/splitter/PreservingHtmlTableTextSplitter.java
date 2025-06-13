package com.microsoft.openai.samples.indexer.langchain4j.splitter;

import com.microsoft.openai.samples.indexer.langchain4j.PagedDocument;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.segment.TextSegment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PreservingHtmlTableTextSplitter implements DocumentSplitter {
    private static final Logger LOGGER = LoggerFactory.getLogger(PreservingHtmlTableTextSplitter.class);
    private final List<String> sentenceEndings;
    private final List<String> wordBreaks;
    private final int maxSegmentSizeInChars;
    private final int sentenceSearchLimitInChars;
    private final int segmentOverlapInChars;


    public PreservingHtmlTableTextSplitter(int maxSegmentSizeInChars, int sentenceSearchLimitInChars, int segmentOverlapInChars) {
        this.sentenceEndings = Arrays.asList(".", "。", "．", "!", "?", "‼", "⁇", "⁈", "⁉");
        this.wordBreaks = Arrays.asList(",", "、", ";", ":", " ", "(", ")", "[", "]", "{", "}", "\t", "\n");

        this.maxSegmentSizeInChars = maxSegmentSizeInChars;
        this.sentenceSearchLimitInChars = sentenceSearchLimitInChars;
        this.segmentOverlapInChars = segmentOverlapInChars;
    }

    @Override
    public List<TextSegment> split(Document document) {
        throw new IllegalStateException("This splitter is designed to split a list of pages, not a single document. Please use splitAll(List<Document> pages) method instead.");
    }


    public List<TextSegment> splitAll(List<Document> pages) {

       if(pages != null && !pages.isEmpty()) {
            if( !(pages.get(0) instanceof PagedDocument) ) {
               throw new IllegalStateException("Expected PagedDocument, but got " + pages.get(0).getClass().getName());
            }
        }

        List<TextSegment> textSegments = new ArrayList<>();
        StringBuilder allText = new StringBuilder();
        for (Document page : pages) {
            allText.append(page.text());
        }
        int length = allText.length();
        int start = 0;
        int end = length;

        if (length <= maxSegmentSizeInChars) {
            int pageNumber = findPage(start, pages);
            TextSegment textSegment = new TextSegment(allText.toString(),pages.get(pageNumber).metadata().copy());
            textSegments.add(textSegment);
            return textSegments;
        }

        while (start + segmentOverlapInChars < length) {
            int lastWord = -1;
            end = start + maxSegmentSizeInChars;

            if (end > length) {
                end = length;
            } else {
                // Try to find the end of the sentence
                while (end < length && (end - start - maxSegmentSizeInChars) < sentenceSearchLimitInChars
                        && !sentenceEndings.contains(String.valueOf(allText.charAt(end)))) {
                    if (wordBreaks.contains(String.valueOf(allText.charAt(end)))) {
                        lastWord = end;
                    }
                    end++;
                }
                if (end < length && !sentenceEndings.contains(String.valueOf(allText.charAt(end))) && lastWord > 0) {
                    end = lastWord; // Fall back to at least keeping a whole word
                }
            }
            if (end < length) {
                end++;
            }

            // Try to find the start of the sentence or at least a whole word boundary
            lastWord = -1;
            while (start > 0 && start > end - maxSegmentSizeInChars - 2 * sentenceSearchLimitInChars
                    && !sentenceEndings.contains(String.valueOf(allText.charAt(start)))) {
                if (wordBreaks.contains(String.valueOf(allText.charAt(start)))) {
                    lastWord = start;
                }
                start--;
            }
            if (!sentenceEndings.contains(String.valueOf(allText.charAt(start))) && lastWord > 0) {
                start = lastWord;
            }
            if (start > 0) {
                start++;
            }

            String sectionText = allText.substring(start, end);
            Integer pageNumber = findPage(start, pages);
            TextSegment textSegment = new TextSegment(sectionText,pages.get(pageNumber).metadata().copy());
            textSegments.add(textSegment);


            int lastTableStart = sectionText.lastIndexOf("<table");
            if (lastTableStart > 2 * sentenceSearchLimitInChars && lastTableStart > sectionText.lastIndexOf("</table")) {
                // If the section ends with an unclosed table, we need to start the next section with the table.
                // If table starts inside sentenceSearchLimit, we ignore it, as that will cause an infinite loop for tables longer than MAX_SECTION_LENGTH
                // If last table starts inside sectionOverlap, keep overlapping

                LOGGER.debug("Section ends with unclosed table, starting next section with the table at page "
                        + findPage(start, pages) + " offset " + start + " table start " + lastTableStart);

                start = Math.min(end - segmentOverlapInChars, start + lastTableStart);
            } else {
                start = end - segmentOverlapInChars;
            }
        }

        if (start + segmentOverlapInChars < end) {
            String sectionText = allText.substring(start, end);
            Integer pageNumber = findPage(start, pages);
            TextSegment textSegment = new TextSegment(sectionText,pages.get(pageNumber).metadata().copy());
            textSegments.add(textSegment);
        }

        return textSegments;
    }

    private int findPage(int offset, List<Document> pages) {
        int numPages = pages.size();
        for (int i = 0; i < numPages - 1; i++) {
            if (offset >= pages.get(i).metadata().getInteger("page_offset") && offset < pages.get(i + 1).metadata().getInteger("page_offset")) {
                return pages.get(i).metadata().getInteger("page_number");
            }
        }
        return pages.get(numPages - 1).metadata().getInteger("page_number");
    }

}
