package com.microsoft.openai.samples.rag.chat.approaches.semantickernel;

import com.azure.ai.openai.OpenAIAsyncClient;
import com.microsoft.openai.samples.rag.approaches.ContentSource;
import com.microsoft.openai.samples.rag.approaches.RAGApproach;
import com.microsoft.openai.samples.rag.approaches.RAGOptions;
import com.microsoft.openai.samples.rag.approaches.RAGResponse;
import com.microsoft.openai.samples.rag.ask.approaches.semantickernel.CognitiveSearchPlugin;
import com.microsoft.openai.samples.rag.common.ChatGPTConversation;
import com.microsoft.openai.samples.rag.common.ChatGPTUtils;
import com.microsoft.openai.samples.rag.proxy.CognitiveSearchProxy;
import com.microsoft.openai.samples.rag.proxy.OpenAIProxy;
import com.microsoft.semantickernel.Kernel;
import com.microsoft.semantickernel.SKBuilders;
import com.microsoft.semantickernel.chatcompletion.ChatCompletion;
import com.microsoft.semantickernel.orchestration.SKContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Simple chat-read-retrieve-read java implementation, using the Cognitive Search and OpenAI APIs directly.
 * It uses the ChatGPT API to turn the user question into a good search query.
 * It queries Azure Cognitive Search for search results for that query (optionally using the vector embeddings for that query).
 * It then combines the search results and original user question, and asks ChatGPT API to answer the question based on the sources. It includes the last 4K of message history as well (or however many tokens are allowed by the deployed model).
 */
@Component
public class JavaSemanticKernelChainsChatApproach implements RAGApproach<ChatGPTConversation, RAGResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(JavaSemanticKernelChainsChatApproach.class);
    private static final String PLAN_PROMPT = """
            Take the input as a question and answer it finding any information needed
            """;
    private final CognitiveSearchProxy cognitiveSearchProxy;

    private final OpenAIProxy openAIProxy;

    private final OpenAIAsyncClient openAIAsyncClient;

    @Value("${openai.chatgpt.deployment}")
    private String gptChatDeploymentModelId;

    public JavaSemanticKernelChainsChatApproach(CognitiveSearchProxy cognitiveSearchProxy, OpenAIAsyncClient openAIAsyncClient, OpenAIProxy openAIProxy) {
        this.cognitiveSearchProxy = cognitiveSearchProxy;
        this.openAIAsyncClient = openAIAsyncClient;
        this.openAIProxy = openAIProxy;
    }

    /**
     * @param questionOrConversation
     * @param options
     * @return
     */
    @Override
    public RAGResponse run(ChatGPTConversation questionOrConversation, RAGOptions options) {

        String question = ChatGPTUtils.getLastUserQuestion(questionOrConversation.getMessages());

        Kernel semanticKernel = buildSemanticKernel(options);

        SKContext searchContext =
                semanticKernel.runAsync(
                        question,
                        semanticKernel.getSkill("InformationFinder").getFunction("Search", null)).block();

        var sources = formSourcesList(searchContext.getResult());

        var answerVariables = SKBuilders.variables()
                .withVariable("sources", searchContext.getResult())
                .withVariable("input", question)
                .build();

        SKContext answerExecutionContext =
                semanticKernel.runAsync(answerVariables,
                        semanticKernel.getSkill("RAG").getFunction("AnswerQuestion", null)).block();
        return new RAGResponse.Builder()
                .prompt("Prompt is managed by Semantic Kernel")
                .answer(answerExecutionContext.getResult())
                .sources(sources)
                .sourcesAsText(searchContext.getResult())
                .question(question)
                .build();
    }

    @Override
    public void runStreaming(
            ChatGPTConversation questionOrConversation,
            RAGOptions options,
            OutputStream outputStream) {
    }

    private List<ContentSource> formSourcesList(String result) {
        if (result == null) {
            return Collections.emptyList();
        }
        return Arrays.stream(result
                        .split("\n"))
                .map(source -> {
                    String[] split = source.split(":", 2);
                    if (split.length >= 2) {
                        var sourceName = split[0].trim();
                        var sourceContent = split[1].trim();
                        return new ContentSource(sourceName, sourceContent);
                    } else {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private Kernel buildSemanticKernel(RAGOptions options) {
        Kernel kernel = SKBuilders.kernel()
                .withDefaultAIService(SKBuilders.chatCompletion()
                        .withModelId(gptChatDeploymentModelId)
                        .withOpenAIClient(this.openAIAsyncClient)
                        .build())
                .build();

        kernel.importSkill(new CognitiveSearchPlugin(this.cognitiveSearchProxy, this.openAIProxy, options), "InformationFinder");

        kernel.importSkillFromResources(
                "semantickernel/Plugins",
                "RAG",
                "AnswerQuestion",
                null
        );

        return kernel;
    }

}
