package com.microsoft.openai.samples.indexer.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.microsoft.openai.samples.indexer.SplitPage;


/**
 * It's responsible for splitting the text content of a list of pages into smaller sections.
 * It does this by identifying sentence endings and word breaks, and then using these to determine where to split the text.
 * The class also has a maximum section length, a sentence search limit, and a section overlap, which are used to fine-tune the splitting process.
 */
public class TextSplitter {
    private List<String> sentenceEndings;
    private List<String> wordBreaks;
    private int maxSectionLength;
    private int sentenceSearchLimit;
    private int sectionOverlap;
    private boolean verbose;

    public TextSplitter(boolean verbose) {
        this(true,1000,100,100);
    }

    public TextSplitter(boolean verbose, int maxSectionLength, int sentenceSearchLimit, int sectionOverlap) {
        this.sentenceEndings = Arrays.asList(".", "。", "．", "!", "?", "‼", "⁇", "⁈", "⁉");
        this.wordBreaks = Arrays.asList(",", "、", ";", ":", " ", "(", ")", "[", "]", "{", "}", "\t", "\n");

        this.maxSectionLength = maxSectionLength;
        this.sentenceSearchLimit = sentenceSearchLimit;
        this.sectionOverlap = sectionOverlap;
        this.verbose = verbose;
    }
    public List<SplitPage> splitPages(List<Page> pages) {
        List<SplitPage> splitPages = new ArrayList<>();
        StringBuilder allText = new StringBuilder();
        for (Page page : pages) {
            allText.append(page.getText());
        }
        int length = allText.length();
        int start = 0;
        int end = length;

        if (length <= maxSectionLength) {
            splitPages.add(new SplitPage(findPage(start, pages), allText.toString()));
            return splitPages;
        }

        while (start + sectionOverlap < length) {
            int lastWord = -1;
            end = start + maxSectionLength;

            if (end > length) {
                end = length;
            } else {
                // Try to find the end of the sentence
                while (end < length && (end - start - maxSectionLength) < sentenceSearchLimit
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
            while (start > 0 && start > end - maxSectionLength - 2 * sentenceSearchLimit
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
            splitPages.add(new SplitPage(findPage(start, pages), sectionText));

            int lastTableStart = sectionText.lastIndexOf("<table");
            if (lastTableStart > 2 * sentenceSearchLimit && lastTableStart > sectionText.lastIndexOf("</table")) {
                // If the section ends with an unclosed table, we need to start the next section with the table.
                // If table starts inside sentenceSearchLimit, we ignore it, as that will cause an infinite loop for tables longer than MAX_SECTION_LENGTH
                // If last table starts inside sectionOverlap, keep overlapping
                if (verbose) {
                    System.out.println("Section ends with unclosed table, starting next section with the table at page "
                            + findPage(start, pages) + " offset " + start + " table start " + lastTableStart);
                }
                start = Math.min(end - sectionOverlap, start + lastTableStart);
            } else {
                start = end - sectionOverlap;
            }
        }

        if (start + sectionOverlap < end) {
            splitPages.add(new SplitPage(findPage(start, pages), allText.substring(start, end)));
        }

        return splitPages;
    }

    private int findPage(int offset, List<Page> pages) {
        int numPages = pages.size();
        for (int i = 0; i < numPages - 1; i++) {
            if (offset >= pages.get(i).getOffset() && offset < pages.get(i + 1).getOffset()) {
                return pages.get(i).getPageNum();
            }
        }
        return pages.get(numPages - 1).getPageNum();
    }
}

