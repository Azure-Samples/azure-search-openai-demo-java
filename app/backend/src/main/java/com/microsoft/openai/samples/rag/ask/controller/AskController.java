package com.microsoft.openai.samples.rag.ask.controller;

import com.microsoft.openai.samples.rag.approaches.*;
import com.microsoft.openai.samples.rag.controller.Overrides;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class AskController {
	private static final Logger logger = LoggerFactory.getLogger(AskController.class);
	private RAGApproachFactory ragApproachFactory;

	AskController(RAGApproachFactory ragApproachFactory) {
		this.ragApproachFactory = ragApproachFactory;
	}

	@PostMapping("/api/ask")
	public ResponseEntity<AskResponse> openAIAsk(@RequestBody AskRequest askRequest) {
		logger.info("Received request for ask api with question [{}] and approach[{}]", askRequest.getQuestion(),askRequest.getApproach());

		if (!StringUtils.hasText(askRequest.getApproach())) {
			logger.warn("approach cannot be null in ASK request");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
		}

		if (!StringUtils.hasText(askRequest.getQuestion())) {
			logger.warn("question cannot be null in ASK request");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
		}

		RAGApproach<String,RAGResponse> ragApproach = ragApproachFactory.createApproach(askRequest.getApproach(), RAGType.ASK);

		//set empty overrides if not provided
		if(askRequest.getOverrides() == null) { askRequest.setOverrides(new Overrides());}

		var ragOptions = new RAGOptions.Builder()
				.semanticRanker(askRequest.getOverrides().isSemanticRanker())
				.semanticCaptions(askRequest.getOverrides().isSemanticCaptions())
				.excludeCategory(askRequest.getOverrides().getExcludeCategory())
				.promptTemplate(askRequest.getOverrides().getPromptTemplate())
				.top(askRequest.getOverrides().getTop())
				.build();

		return ResponseEntity.ok(buildAskResponse(ragApproach.run(askRequest.getQuestion(),ragOptions)));
	}

	private AskResponse buildAskResponse(RAGResponse ragResponse) {
		var askResponse = new AskResponse();

		askResponse.setAnswer(ragResponse.getAnswer());
		List<String> dataPoints;
		if(ragResponse.getSourcesAsText() != null && !ragResponse.getSourcesAsText().isEmpty())
			dataPoints = Arrays.asList(ragResponse.getSourcesAsText().split("\n"));
		else
		dataPoints = ragResponse.getSources().stream()
				.map(source ->source.getSourceName()+": "+source.getSourceContent())
				.collect(Collectors.toList());

		askResponse.setDataPoints(dataPoints);

		askResponse.setThoughts("Question:<br>"+ragResponse.getQuestion()+"<br><br>Prompt:<br>"+ragResponse.getPrompt().replace("\n","<br>"));

		return askResponse;
	}
}