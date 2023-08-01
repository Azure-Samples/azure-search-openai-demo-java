package com.microsoft.openai.samples.rag;

import com.microsoft.openai.samples.rag.ask.controller.AskRequest;
import com.microsoft.openai.samples.rag.ask.controller.AskResponse;
import com.microsoft.openai.samples.rag.chat.controller.ChatRequest;
import com.microsoft.openai.samples.rag.chat.controller.ChatResponse;
import com.microsoft.openai.samples.rag.chat.controller.ChatTurn;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ChatAPITests {

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	public void testExample() throws Exception {
		//TODO This test is failing due to external services dependencies.
		// Explore springmockserver to mock the external services response based on specific requests. Sevices to mock are: Azure token, Cognitive Search, OpenAI chat apis
/** ChatTurn chatTurn = new ChatTurn();
		chatTurn.setUserText("What does a Product Manager do?");
		List<ChatTurn> chatTurns = new ArrayList<>();
		chatTurns.add(chatTurn);

		ChatRequest chatRequest = new ChatRequest();
		chatRequest.setChatHistory(chatTurns);
		chatRequest.setApproach("rrr");
		HttpEntity<ChatRequest> request = new HttpEntity<>(chatRequest);

		ResponseEntity<ChatResponse> result = this.restTemplate.postForEntity(uri("/api/chat"), chatRequest, ChatResponse.class);

		assertEquals(HttpStatus.OK, result.getStatusCode());
**/
	}


	private URI uri(String path) {
		return restTemplate.getRestTemplate().getUriTemplateHandler().expand(path);
	}

}
