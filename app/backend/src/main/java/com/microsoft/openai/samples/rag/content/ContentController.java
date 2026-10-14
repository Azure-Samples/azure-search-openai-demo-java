// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.openai.samples.rag.content;

import com.microsoft.openai.samples.rag.proxy.BlobStorageProxy;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.Map;

import com.microsoft.openai.samples.rag.security.LoggedUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 *  Controller providing the api to stream the page content from documents stored in Azure Blob Storage.
 */
@RestController
public class ContentController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContentController.class);
    private final BlobStorageProxy blobStorageProxy;
    private final LoggedUserService loggedUserService;
    private final IndexService indexService;

    ContentController(BlobStorageProxy blobStorageProxy, LoggedUserService loggedUserService, IndexService indexService) {
        this.blobStorageProxy = blobStorageProxy;
        this.loggedUserService = loggedUserService;
        this.indexService = indexService;
    }

    /**
     * Retrieves the content of a file from Azure Blob Storage.
     * The file is determined by the provided file name.
     *
     * @param fileName the name of the file to retrieve
     * @return the content of the file or an error message
     */
    @GetMapping("/api/content/{fileName}")
    public ResponseEntity<InputStreamResource> getContent(@PathVariable String fileName) {

        LOGGER.info("Received request for  content with name [{}] ]", fileName);

        if (!StringUtils.hasText(fileName)) {
            LOGGER.warn("file name cannot be null");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        String mimeType = URLConnection.guessContentTypeFromName(fileName);

        MediaType contentType = new MediaType(MimeTypeUtils.parseMimeType(mimeType));

        String entraOid = loggedUserService.getLoggedUser().entraId();
        String folderName = StringUtils.hasText(entraOid) ? entraOid : "default";
        String filePath = folderName + "/" + fileName;

        InputStream fileInputStream;

        try {
            fileInputStream = new ByteArrayInputStream(blobStorageProxy.getFileAsBytes(filePath));
        } catch (Exception ex) {
            LOGGER.error("Cannot retrieve file [{}] from blob.{}", fileName, ex.getMessage());
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .header("Content-Disposition", "inline; filename=%s".formatted(fileName))
                .contentType(contentType)
                .body(new InputStreamResource(fileInputStream));
    }

    /**
     * Lists all files in the user's folder in Azure Blob Storage.
     * The folder is determined by the user's Entra ID, or defaults to "default" if not available.
     *
     * @return a list of file names or an error message
     */
    @GetMapping("/api/list_uploaded")
    public ResponseEntity<?> listFiles() {
        String entraOid = loggedUserService.getLoggedUser().entraId();
        String folderName = StringUtils.hasText(entraOid) ? entraOid : "default";
        LOGGER.info("Received request to list files in user [{}]", folderName );
        try {
            var files = blobStorageProxy.listFilesInFolder(folderName,true);
            return ResponseEntity.ok(files);
        } catch (Exception ex) {
            LOGGER.error("Cannot list files in folder [{}]", folderName, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error listing files");
        }
    }

    /**
     * Uploads a file to the user's folder in Azure Blob Storage.
     * The folder is determined by the user's Entra ID, or defaults to "default" if not available.
     *
     * @param file the file to upload
     * @return the name of the uploaded file or an error message
     */
    @PostMapping("/api/upload")
    public ResponseEntity<?> uploadContentToFolder(@RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            LOGGER.warn("Uploaded file [{}] is empty",file.getOriginalFilename());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Uploaded file is empty");
        }

        LOGGER.info("Received request to upload file [{}]", file.getOriginalFilename());


        try {
            indexService.synchAddFile(file.getOriginalFilename(), file.getBytes());
        } catch (IOException ex) {
            LOGGER.error("Cannot store and index file [{}] ", file.getOriginalFilename(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error occurred while storing and indexing file");
        }

        return ResponseEntity.ok(Map.of("message","File uploaded successfully"));
    }

    /**
     * Deletes a file from the user's folder in Azure Blob Storage.
     * The folder is determined by the user's Entra ID, or defaults to "default" if not available.
     *
     * @param filenameRequest the request containing the name of the file to delete
     * @return a success message or an error message
     */
    @PostMapping("/api/delete_uploaded")
    public ResponseEntity<?> deleteContent(@RequestBody FilenameRequest filenameRequest) {

        if (!StringUtils.hasText(filenameRequest.filename())) {
            LOGGER.warn("file name cannot be null");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("File name cannot be null");
        }

        LOGGER.info("Received request to delete file [{}]", filenameRequest.filename());

        try {
            indexService.synchDeleteFile(filenameRequest.filename());
        } catch (Exception ex) {
            LOGGER.error("Cannot delete file [{}]", filenameRequest.filename(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error deleting file");
        }

        return ResponseEntity.ok(Map.of("message","File deleted successfully"));
    }
}


