package com.microsoft.openai.samples.indexer.cli;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.AzureDeveloperCliCredentialBuilder;

import com.microsoft.openai.samples.indexer.cli.langchain4j.UploadCommand;
import com.microsoft.openai.samples.indexer.storage.BlobManager;
import dev.langchain4j.store.embedding.azure.search.AzureAiSearchEmbeddingStore;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.util.concurrent.Callable;


public class CLI implements Callable<Integer> {

    @Option(names = {"--storageaccount"}, required = true)
    private String storageaccount;

    @Option(names = {"--container"}, required = true)
    private String container;

    @Option(names = {"--searchservice"}, required = true)
    private String searchservice;

      @Option(names = {"--searchanalyzername"}, required = false, defaultValue = "en.microsoft")
    private String searchanalyzername;

    @Option(names = {"--index"}, required = true)
    private String index;

    @Option(names = {"--openai-emb-deployment"}, required = true)
    private String openaiEmbdeployment;

    @Option(names = {"--openai-service-name"}, required = true)
    private String openaiServiceName;

    @Option(names = {"--document-intelligent-service"}, required = true)
    private String documentIntelligentServiceName;

    @Option(names = {"-v","--verbose"}, required = true)
    private boolean verbose;

    @Option(names = {"-c","--category"}, required = false, defaultValue = "default")
    private String category;

    @Option(names = {"--embedding-dimensions"}, required =false, defaultValue = "3072")
    private int dimensions;

    @Option(names = {"--indexer-url"}, required = true, defaultValue = "http://localhost:8080")
    private String indexerApiUrl;

    @Parameters(index = "0")
    private Path dataFolderPath;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new CLI()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        System.out.println(" use upload commands");
        return 0;
    }



    @Command(name = "upload")
    public void uploadCommand() {
        TokenCredential tokenCredential = new AzureDeveloperCliCredentialBuilder().build();
        
        new UploadCommand(buildBlobManager(tokenCredential),
                          buildAzureAiEmbeddingStore(tokenCredential),
                          dimensions,
                          indexerApiUrl)
                .run(dataFolderPath,category);

    }

    private BlobManager buildBlobManager(TokenCredential tokenCredential) {
        return new BlobManager(storageaccount, container, tokenCredential);
    }


    private AzureAiSearchEmbeddingStore buildAzureAiEmbeddingStore(TokenCredential tokenCredential) {

        String endpoint = "https://%s.search.windows.net".formatted(searchservice);
       return  AzureAiSearchEmbeddingStore.builder()
                .endpoint(endpoint)
                .indexName(index)
                .dimensions(dimensions)
                .tokenCredential(tokenCredential)
                .createOrUpdateIndex(true)
                .build();

    }


}


