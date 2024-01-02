package com.microsoft.openai.samples.indexer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

import com.microsoft.openai.samples.indexer.embeddings.AzureOpenAIEmbeddingService;
import com.microsoft.openai.samples.indexer.embeddings.TextEmbeddingsService;
import com.microsoft.openai.samples.indexer.index.AzureSearchClientFactory;
import com.microsoft.openai.samples.indexer.index.SearchIndexManager;
import com.microsoft.openai.samples.indexer.parser.DocumentIntelligencePDFParser;
import com.microsoft.openai.samples.indexer.parser.ItextPDFParser;
import com.microsoft.openai.samples.indexer.parser.TextSplitter;
import com.microsoft.openai.samples.indexer.storage.BlobManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.AzureCliCredentialBuilder;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;


public class CLI implements Callable<Integer> {
    
    private static final Logger logger = LoggerFactory.getLogger(CLI.class);

    @Option(names = {"--storageaccount"}, required = true)
    private String storageaccount;

    @Option(names = {"--container"}, required = true)
    private String container;

    @Option(names = {"--searchservice"}, required = true)
    private String searchservice;

      @Option(names = {"--searchanalyzername"}, required = false, defaultValue = " en.microsoft")
    private String searchanalyzername;

    @Option(names = {"--index"}, required = true)
    private String index;

    @Option(names = {"--openai-emb-deployment"}, required = true)
    private String openaiEmbdeployment;

    @Option(names = {"--openai-service-name"}, required = true)
    private String openaiServiceName;

    @Option(names = {"--formrecognizerservice"}, required = true)
    private String formrecognizerservice;

    @Option(names = {"-v","--verbose"}, required = true)
    private boolean verbose;

    @Option(names = {"-c","--category"}, required = false, defaultValue = "default")
    private String category;

    @Parameters(index = "0")
    private Path dataFolderPath;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new CLI()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        System.out.println(" use add command");
        return 0;
    }

    @Command(name = "add")
    public void addCommand() {
    TokenCredential tokenCredential = new AzureCliCredentialBuilder().build();
    TextEmbeddingsService textEmbeddingsService = new AzureOpenAIEmbeddingService(openaiServiceName, openaiEmbdeployment, tokenCredential, verbose);
    AzureSearchClientFactory azureSearchClientFactory = new AzureSearchClientFactory(searchservice, tokenCredential, index, verbose);
    SearchIndexManager searchIndexManager = new SearchIndexManager(azureSearchClientFactory,searchanalyzername,textEmbeddingsService);
    
    searchIndexManager.createIndex();

    //DocumentProcessor documentProcessor = new DocumentProcessor(searchIndexManager, new ItextPDFParser(), new TextSplitter(verbose));
    DocumentProcessor documentProcessor = new DocumentProcessor(searchIndexManager, new DocumentIntelligencePDFParser(formrecognizerservice,tokenCredential,verbose), new TextSplitter(verbose));
    BlobManager blobManager = new BlobManager(storageaccount, container, tokenCredential, verbose);
    
    if(Files.isDirectory(dataFolderPath))
        processDirectory(documentProcessor, blobManager, dataFolderPath);
    else 
        processFile(documentProcessor, blobManager, dataFolderPath);
    
}

private void processDirectory(DocumentProcessor documentProcessor, BlobManager blobManager, Path directory) {
    logger.debug("Processing directory {}", directory);
    try {
        Files.newDirectoryStream(directory).forEach(path -> { 
            processFile(documentProcessor, blobManager, path);
        });
      logger.debug("All files in directory {} processed", directory.toRealPath().toString());
    } catch (Exception e) {
        throw new RuntimeException("Error processing folder ",e);
    }
}

private void processFile(DocumentProcessor documentProcessor, BlobManager blobManager, Path path) {
   try {
        String absoluteFilePath = path.toRealPath().toString();
        documentProcessor.indexDocumentfromFile(absoluteFilePath,category);
        logger.debug("file {} indexed", absoluteFilePath);
        blobManager.uploadBlob(path.toFile());
        logger.debug("file {} uploaded", absoluteFilePath);
    } catch (Exception e) {
                throw new RuntimeException("Error processing file ",e);
     }
}


}


