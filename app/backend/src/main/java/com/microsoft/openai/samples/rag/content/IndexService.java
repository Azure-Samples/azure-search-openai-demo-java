package com.microsoft.openai.samples.rag.content;

import com.microsoft.openai.samples.rag.proxy.BlobStorageProxy;
import com.microsoft.openai.samples.rag.security.LoggedUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;


@Component
public class IndexService {

    private static final Logger LOGGER = LoggerFactory.getLogger(IndexService.class);
    private final WebClient.Builder webClientBuilder;
    private final String indexingAPIUrl;
    private final LoggedUserService loggedUserService;
    private final BlobStorageProxy blobStorageProxy;

    public IndexService(WebClient.Builder webClientBuilder, @Value("${indexing.api.url}") String indexingAPIUrl, LoggedUserService loggedUserService, BlobStorageProxy blobStorageProxy) {
        this.webClientBuilder = webClientBuilder;
        this.indexingAPIUrl = indexingAPIUrl;
        this.loggedUserService = loggedUserService;
        this.blobStorageProxy = blobStorageProxy;
    }

    public void synchAddFile(String filename, byte [] fileContent) {
        String entraOid = loggedUserService.getLoggedUser().entraId();
        entraOid = StringUtils.hasText(entraOid) ?  entraOid:"default";

        //use entraoid or default as folder name
        if(blobStorageProxy.exists(entraOid,filename)) {
            LOGGER.warn("File {} already exists in folder {}, skipping upload", filename, entraOid);
            return;
        }

        try {
         blobStorageProxy.uploadFileToFolder(entraOid, fileContent, filename);
        } catch (Exception ex) {
            throw new RuntimeException("Cannot upload file [%s] to directory [%s]".formatted(filename,entraOid), ex);
        }

        var filePath = entraOid + "/" + filename;
        var metadata = List.of(Map.of("key","oid","value",loggedUserService.getLoggedUser().entraId()));
        var indexClientRequest = new IndexClientRequest(filePath,metadata);

        try {
            webClientBuilder.build()
                    .post()
                    .uri(indexingAPIUrl + "/api/index/add")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(indexClientRequest))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception ex) {
                LOGGER.warn("Error indexing file {}: {}", filename, ex.getMessage());
                LOGGER.warn("Reverting upload of file {} to directory {}", filename, entraOid);
                blobStorageProxy.deleteIfExistsFileFromFolder(entraOid, filename);
                throw new RuntimeException("Error indexing file [%s]".formatted(filename), ex);
        }

        LOGGER.info("File {} successfully indexed", filename);
    }

    public void synchDeleteFile(String filename){
        String entraOid = loggedUserService.getLoggedUser().entraId();
        String folderName = StringUtils.hasText(entraOid) ?  entraOid:"default";

        var indexClientRequest = new IndexClientRequest(folderName+"/"+filename,null);

        try {
        webClientBuilder.build()
                .post()
                .uri(indexingAPIUrl + "/api/index/delete")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(indexClientRequest))
                .retrieve()
                .bodyToMono(String.class)
                .block();
        } catch (Exception ex) {
            throw new RuntimeException("Error deleting file [%s] from the index".formatted(filename), ex);
        }

        try {
            boolean result = blobStorageProxy.deleteIfExistsFileFromFolder(folderName,filename);
            if (!result) {
                throw new RuntimeException("Error while deleting from the store: file [%s] not found in directory [%s].".formatted(filename,folderName));
            }
        } catch (Exception ex) {
            throw new RuntimeException("Cannot delete from file [%s] from directory [%s].Please remove it manually".formatted(filename,folderName), ex);
        }

        LOGGER.info("File {} successfully deleted from index and storage", filename);
    }


}
