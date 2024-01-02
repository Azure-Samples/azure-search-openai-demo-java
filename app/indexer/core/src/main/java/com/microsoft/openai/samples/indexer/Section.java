
package com.microsoft.openai.samples.indexer;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import java.util.Base64;

public class Section {
    private SplitPage splitPage;
    private String filename;
    private String category;

    public Section(SplitPage splitPage, String filename, String category) {
        this.splitPage = splitPage;
        this.filename = filename;
        this.category = category;
    }

    public SplitPage getSplitPage() {
        return splitPage;
    }

    public String getFilename() {
        return filename;
    }

    public String getCategory() {
        return category;
    }

    public String getFilenameToId() {
            String filenameAscii = Pattern.compile("[^0-9a-zA-Z_-]").matcher(filename).replaceAll("_");
            String filenameHash = Base64.getEncoder().encodeToString(filename.getBytes(StandardCharsets.UTF_8));
            return "file-" + filenameAscii + "-" + filenameHash;
        
    }
}
