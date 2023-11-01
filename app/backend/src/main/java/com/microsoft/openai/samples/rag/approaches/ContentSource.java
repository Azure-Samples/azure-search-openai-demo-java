// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.openai.samples.rag.approaches;

public class ContentSource {

    private String sourceName;
    private String sourceContent;
    private final boolean noNewLine;

    public ContentSource(String sourceName, String sourceContent, Boolean noNewLine) {
        this.noNewLine = noNewLine;
        this.sourceName = sourceName;
        buildContent(sourceContent);
    }

    public ContentSource(String sourceName, String sourceContent) {
        this(sourceName, sourceContent, true);
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public String getSourceContent() {
        return sourceContent;
    }

    public void setSourceContent(String sourceContent) {
        this.sourceContent = sourceContent;
    }

    public boolean isNoNewLine() {
        return noNewLine;
    }

    private void buildContent(String sourceContent) {
        if (this.noNewLine) {
            this.sourceContent = sourceContent.replace("\n", "");
        }
    }
}
