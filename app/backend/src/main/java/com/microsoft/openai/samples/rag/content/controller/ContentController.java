package com.microsoft.openai.samples.rag.content.controller;

import com.microsoft.openai.samples.rag.approaches.RAGApproach;
import com.microsoft.openai.samples.rag.approaches.RAGApproachFactory;
import com.microsoft.openai.samples.rag.approaches.RAGOptions;
import com.microsoft.openai.samples.rag.approaches.RAGResponse;
import com.microsoft.openai.samples.rag.ask.controller.AskRequest;
import com.microsoft.openai.samples.rag.ask.controller.AskResponse;
import com.microsoft.openai.samples.rag.controller.Overrides;
import com.microsoft.openai.samples.rag.proxy.BlobStorageProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class ContentController {
	private static final Logger logger = LoggerFactory.getLogger(ContentController.class);
	private BlobStorageProxy blobStorageProxy;

	ContentController(BlobStorageProxy blobStorageProxy) {
		this.blobStorageProxy = blobStorageProxy;
	}

	@GetMapping("/api/content/{fileName}")
			public ResponseEntity<InputStreamResource> getContent(@PathVariable String fileName) {
		logger.info("Received request for  content with name [{}] ]", fileName);

		if (!StringUtils.hasText(fileName)) {
			logger.warn("file name cannot be null");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
		}

		String mimeType = URLConnection.guessContentTypeFromName(fileName);

		MediaType contentType = new MediaType(MimeTypeUtils.parseMimeType(mimeType));

		InputStream fileInputStream = null;

		try {
			fileInputStream = new  ByteArrayInputStream (blobStorageProxy.getFileAsBytes(fileName));
		}catch (IOException ex){
			logger.error("Cannot retrieve file [{}] from blob.{}",fileName, ex.getMessage());
			return ResponseEntity.notFound().build();
		}

		return ResponseEntity.ok()
				.header("Content-Disposition","inline; filename=%s".formatted(fileName))
				.contentType(contentType)
				.body(new InputStreamResource(fileInputStream))
				;
	}

}
