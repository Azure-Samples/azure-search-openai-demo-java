package com.microsoft.openai.samples.indexer.functions;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.AzureDeveloperCliCredentialBuilder;
import com.azure.identity.ManagedIdentityCredentialBuilder;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.*;
import com.microsoft.openai.samples.indexer.DocumentProcessor;
import com.microsoft.openai.samples.indexer.embeddings.AzureOpenAIEmbeddingService;
import com.microsoft.openai.samples.indexer.index.AzureSearchClientFactory;
import com.microsoft.openai.samples.indexer.index.SearchIndexManager;
import com.microsoft.openai.samples.indexer.parser.DocumentIntelligencePDFParser;
import com.microsoft.openai.samples.indexer.parser.TextSplitter;
import com.microsoft.openai.samples.indexer.storage.BlobManager;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Azure Functions with Blob Trigger.
 */
public class BlobProcessorFunction {


    private String storageaccount;


    private String container;


    private String searchservice;


    private String index;


    private String openaiEmbdeployment;


    private String openaiServiceName;


    private String formrecognizerservice;

    private String searchanalyzername = " en.microsoft";

    private boolean verbose = true;

    private boolean locaDev = false;


    DocumentProcessor documentProcessor;
    SearchIndexManager searchIndexManager;

    public BlobProcessorFunction() {
        this.storageaccount = System.getenv("AZURE_STORAGE_ACCOUNT");
        this.container = System.getenv("AZURE_STORAGE_CONTAINER");
        this.openaiServiceName = System.getenv("AZURE_OPENAI_SERVICE");
        this.openaiEmbdeployment = System.getenv("AZURE_OPENAI_EMB_DEPLOYMENT");
        this.searchservice = System.getenv("AZURE_SEARCH_SERVICE");
        this.index = System.getenv("AZURE_SEARCH_INDEX");
        this.formrecognizerservice = System.getenv("AZURE_FORMRECOGNIZER_SERVICE");
        String localDev = System.getenv("LOCAL_DEV");
        String userAssignedManagedIdentity = System.getenv("USER_ASSIGNED_MANAGED_IDENTITY");

        TokenCredential tokenCredential;

        if (localDev != null && localDev.equals("true")) {
            tokenCredential = new AzureDeveloperCliCredentialBuilder().build();
        }
            else {
            if (userAssignedManagedIdentity != null && !userAssignedManagedIdentity.isEmpty()) {
                tokenCredential = new ManagedIdentityCredentialBuilder().clientId(userAssignedManagedIdentity).build();
            } else {
                tokenCredential = new ManagedIdentityCredentialBuilder().build();
            }
        }

        this.searchIndexManager = new SearchIndexManager(
                new AzureSearchClientFactory(searchservice, tokenCredential, index, verbose),
                searchanalyzername,
                new AzureOpenAIEmbeddingService(openaiServiceName, openaiEmbdeployment, tokenCredential, verbose));

         //DocumentProcessor documentProcessor = new DocumentProcessor(searchIndexManager, new ItextPDFParser(), new TextSplitter(verbose));
        this.documentProcessor = new DocumentProcessor(searchIndexManager, new DocumentIntelligencePDFParser(formrecognizerservice,tokenCredential,verbose), new TextSplitter(verbose));
       // BlobManager blobManager = new BlobManager(storageaccount, container, tokenCredential, verbose);
    }
    @FunctionName("BlobEventGridProcessor")
     /**
    * This function will be invoked when a new or updated blob is detected at the specified path. The blob contents are provided as input to this function.
    */
    @StorageAccount("AzureWebJobsStorage")
    public void run(
        @BlobTrigger(name = "content", path = "content/{filename}", dataType = "binary", source = "EventGrid" ) byte[] content,
        @BindingName("filename") String filename,
        final ExecutionContext context
    ) {
        context.getLogger().info("Processing document " + filename + "\n  Size: " + content.length + " Bytes");
        documentProcessor.indexDocumentFromBytes(filename,"",content);
        context.getLogger().info("Document " + filename + " successufully indexed");

      }



 }
