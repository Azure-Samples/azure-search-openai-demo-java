package com.microsoft.openai.samples.indexer.parser;

import com.microsoft.openai.samples.indexer.SplitPage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TextSplitterTest {

    @Test
    void testSplitTinyPages() {
        List<Page> testPages = List.of(new Page[]{
                new Page(1, 0, "hello, world")
        });
        TextSplitter splitter = new TextSplitter(false);
        List<SplitPage> result = splitter.splitPages(testPages);
        assertEquals(1, result.size());
        assertEquals("hello, world", result.get(0).getText());
    }
}