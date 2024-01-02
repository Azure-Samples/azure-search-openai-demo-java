package com.microsoft.openai.samples.indexer;

public class SplitPage {
    private int pageNum;
    private String text;

    public SplitPage(int pageNum, String text) {
        this.pageNum = pageNum;
        this.text = text;
    }

    public int getPageNum() {
        return pageNum;
    }

    public String getText() {
        return text;
    }
}