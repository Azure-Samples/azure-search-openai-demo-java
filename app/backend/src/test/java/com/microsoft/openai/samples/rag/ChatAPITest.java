package com.microsoft.openai.samples.rag;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ChatAPITest {

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	void testExample() throws Exception {
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