package com.microsoft.openai.samples.indexer.index;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.microsoft.openai.samples.indexer.Section;
import com.microsoft.openai.samples.indexer.embeddings.TextEmbeddingsService;
import com.azure.search.documents.indexes.models.SearchIndex;
import com.azure.search.documents.indexes.models.SearchFieldDataType;
import com.azure.search.documents.indexes.models.SemanticConfiguration;
import com.azure.search.documents.indexes.models.SemanticField;
import com.azure.search.documents.indexes.models.SemanticPrioritizedFields;
import com.azure.search.documents.indexes.models.SemanticSearch;
import com.azure.search.documents.indexes.models.VectorSearch;
import com.azure.search.documents.indexes.models.VectorSearchAlgorithmMetric;
import com.azure.search.documents.indexes.models.VectorSearchProfile;
import com.azure.search.documents.indexes.models.HnswAlgorithmConfiguration;
import com.azure.search.documents.indexes.models.HnswParameters;
import com.azure.search.documents.indexes.models.LexicalAnalyzerName;
import com.azure.search.documents.indexes.models.SearchField;
import com.azure.core.exception.HttpResponseException;
import com.azure.search.documents.SearchClient;
import com.azure.search.documents.indexes.SearchIndexClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class SearchIndexManager {
    private AzureSearchClientFactory azureSearchClientFactory;
    private String searchAnalyzerName;
    private boolean useAcls;
    private TextEmbeddingsService embeddingsService;
    private static final Logger logger = LoggerFactory.getLogger(SearchIndexManager.class);

    public SearchIndexManager(AzureSearchClientFactory azureSearchClientFactory, String searchAnalyzerName, TextEmbeddingsService embeddingsService) {
        this.azureSearchClientFactory = azureSearchClientFactory;
        this.searchAnalyzerName = searchAnalyzerName;
        this.embeddingsService = embeddingsService;
    }

    public void createIndex() {
        if (azureSearchClientFactory.isVerbose()) {
                  logger.debug("Ensuring search index {} exists", azureSearchClientFactory.getIndexName());
                }
        
        
        SearchIndexClient searchIndexClient = azureSearchClientFactory.createSearchIndexClient();
        SearchIndex index = null;
        try {
           index = searchIndexClient.getIndex(azureSearchClientFactory.getIndexName());
        }catch(HttpResponseException httpEx) {
            if (httpEx.getResponse().getStatusCode() == 404) 
                logger.info("index {} does not exist. Creating..", azureSearchClientFactory.getIndexName());
        }

        if ( index != null) {
            logger.info("index {} already exists. Skipping creation", azureSearchClientFactory.getIndexName());
            return;
        }
    

        List<SearchField> fields = new ArrayList<>();
        fields.add(new SearchField("id", SearchFieldDataType.STRING)
                            .setKey(true)
                            .setFilterable(false)
                            .setSortable(false)
                            .setFacetable(false)
                            .setSearchable(false));
        fields.add(new SearchField("content", SearchFieldDataType.STRING)
                            .setSearchable(true)
                            .setFilterable(false)
                            .setSortable(false)
                            .setFacetable(false)
                            .setAnalyzerName(LexicalAnalyzerName.EN_MICROSOFT));
        fields.add(new SearchField("embedding", SearchFieldDataType.collection(SearchFieldDataType.SINGLE))
                            .setHidden(false)
                            .setSearchable(true)
                            .setFilterable(false)
                            .setSortable(false)
                            .setFacetable(false)
                            .setVectorSearchDimensions(1536)
                            .setVectorSearchProfileName("embedding_config"));
        fields.add(new SearchField("category", SearchFieldDataType.STRING)
                            .setFilterable(true)
                            .setFacetable(true)
                            .setSortable(false)
                            .setSearchable(false));
        fields.add(new SearchField("sourcepage", SearchFieldDataType.STRING)
                            .setFilterable(true)
                            .setFacetable(true)
                            .setSortable(false)
                            .setSearchable(false));
        fields.add(new SearchField("sourcefile", SearchFieldDataType.STRING)
                            .setFilterable(true)
                            .setFacetable(true)
                            .setSortable(false)
                            .setSearchable(false));

        if (useAcls) {
            fields.add(new SearchField("oids", SearchFieldDataType.collection(SearchFieldDataType.STRING))
                                .setFilterable(true));
            fields.add(new SearchField("groups", SearchFieldDataType.collection(SearchFieldDataType.STRING))
                                .setFilterable(true));
        }

        index = new SearchIndex(azureSearchClientFactory.getIndexName(), fields);
        
        index.setSemanticSearch(new SemanticSearch().setConfigurations(Arrays.asList(new SemanticConfiguration(
            "default", new SemanticPrioritizedFields()
            .setContentFields(new SemanticField("content"))))));

        index.setVectorSearch(new VectorSearch()
        .setAlgorithms(Collections.singletonList(
                new HnswAlgorithmConfiguration("default")
                .setParameters(new HnswParameters().setMetric(VectorSearchAlgorithmMetric.COSINE))))    
        .setProfiles(Collections.singletonList(
                new VectorSearchProfile("embedding_config", "default"))));
            
                                   
          searchIndexClient.createIndex(index);
            
        logger.info("Created index {}", azureSearchClientFactory.getIndexName());
    }

    public void updateContent(List<Section> sections) {
        int MAX_BATCH_SIZE = 1000;
        List<List<Section>> sectionBatches = new ArrayList<>();
        for (int i = 0; i < sections.size(); i += MAX_BATCH_SIZE) {
            sectionBatches.add(sections.subList(i, Math.min(i + MAX_BATCH_SIZE, sections.size())));
        }

        SearchClient searchClient = azureSearchClientFactory.createSearchClient();
        for (int batchIndex = 0; batchIndex < sectionBatches.size(); batchIndex++) {
            List<Section> sectionBatch = sectionBatches.get(batchIndex);
            List<Map<String, Object>> documents = new ArrayList<>();
            List<String> textsToEmbed = new ArrayList<>();
            for (int sectionIndex = 0; sectionIndex < sectionBatch.size(); sectionIndex++) {
                Section section = sectionBatch.get(sectionIndex);
                Map<String, Object> document = new HashMap<>();
                document.put("id", section.getFilenameToId() + "-page-" + (sectionIndex + batchIndex * MAX_BATCH_SIZE));
                document.put("content", section.getSplitPage().getText());
                document.put("category", section.getCategory());
                document.put("sourcepage", getSourcePageFromFilePage(section.getFilename(), section.getSplitPage().getPageNum()));
                document.put("sourcefile", section.getFilename());
                documents.add(document);
                textsToEmbed.add(section.getSplitPage().getText());
            }

           
            //Create embeddings for sections. 
            List<List<Float>> embeddings = embeddingsService.createEmbeddingBatch(textsToEmbed);

            //Embeddings are assigned back to section using index in batch. Using Array List assure ordering is preserved.
            for (int i = 0; i < documents.size(); i++) {
                documents.get(i).put("embedding", embeddings.get(i));
            }
        

            searchClient.uploadDocuments(documents);
        }
        
    }

    /* 
    public void removeContent(String path) {
        if (searchInfo.isVerbose()) {
            System.out.println("Removing sections from '" + (path != null ? path : "<all>") + "' from search index '" + searchInfo.getIndexName() + "'");
        }

        SearchClient searchClient = searchInfo.createSearchClient();
        while (true) {
            String filter = path != null ? "sourcefile eq '" + new File(path).getName() + "'" : null;
            SearchPagedIterable results = searchClient.search("", new SearchOptions().setFilter(filter).setTop(1000).setIncludeTotalCount(true));
            if (results.getTotalCount() == 0) {
                break;
            }

            List<Map<String, Object>> documents = new ArrayList<>();
            for (SearchResult result : results) {
                Map<String, Object> document = new HashMap<>();
                document.put("id", result.getDocument().get("id"));
                documents.add(document);
            }

            List<String> removedDocs = searchClient.deleteDocuments(documents);
            if (searchInfo.isVerbose()) {
                System.out.println("\tRemoved " + removedDocs.size() + " sections from index");
            }

            // It can take a few seconds for search results to reflect changes, so wait a bit
            Thread.sleep(2000);
        }
        
    }

    */
 
 
        private  String getSourcePageFromFilePage(String filename, int page) {
            if (filename.toLowerCase().endsWith(".pdf")) {
                return filename + "#page=" + (page + 1);
            } else {
                return new File(filename).getName();
            }
        }
    
        private  String blobNameFromFile(String filename) {
            return new File(filename).getName();
        }   
}
