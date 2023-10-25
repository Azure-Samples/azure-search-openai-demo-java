package com.microsoft.openai.samples.rag.ask.approaches.semantickernel;

import com.azure.ai.openai.OpenAIAsyncClient;
import com.azure.core.annotation.Get;
import com.microsoft.openai.samples.rag.approaches.ContentSource;
import com.microsoft.openai.samples.rag.approaches.RAGApproach;
import com.microsoft.openai.samples.rag.approaches.RAGOptions;
import com.microsoft.openai.samples.rag.approaches.RAGResponse;
import com.microsoft.openai.samples.rag.proxy.CognitiveSearchProxy;
import com.microsoft.openai.samples.rag.proxy.OpenAIProxy;
import com.microsoft.semantickernel.Kernel;
import com.microsoft.semantickernel.SKBuilders;
import com.microsoft.semantickernel.orchestration.SKContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 *    Use Java Semantic Kernel framework with semantic and native functions chaining. It uses an imperative style for AI orchestration through semantic kernel functions chaining.
 *    InformationFinder.Search native function and RAG.AnswerQuestion semantic function are called sequentially.
 *    Several cognitive search retrieval options are available: Text, Vector, Hybrid.
 */
@Component
public class JavaSemanticKernelChainsApproach implements RAGApproach<String, RAGResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(JavaSemanticKernelChainsApproach.class);
    private static final String PLAN_PROMPT = """
            Take the input as a question and answer it finding any information needed
            """;
    private final CognitiveSearchProxy cognitiveSearchProxy;

    private final OpenAIProxy openAIProxy;

    private final OpenAIAsyncClient openAIAsyncClient;

    @Value("${openai.chatgpt.deployment}")
    private String gptChatDeploymentModelId;

    public JavaSemanticKernelChainsApproach(CognitiveSearchProxy cognitiveSearchProxy, OpenAIAsyncClient openAIAsyncClient, OpenAIProxy openAIProxy) {
        this.cognitiveSearchProxy = cognitiveSearchProxy;
        this.openAIAsyncClient = openAIAsyncClient;
        this.openAIProxy = openAIProxy;
    }

    /**
     * @param question
     * @param options
     * @return
     */
    @Override
    public RAGResponse run(String question, RAGOptions options) {

        //Build semantic kernel context
        Kernel semanticKernel = buildSemanticKernel(options);


        //STEP 1: Retrieve relevant documents using user question. It reuses the CognitiveSearchRetriever appraoch through the CognitiveSearchPlugin native function.
        SKContext searchContext =
                semanticKernel.runAsync(
                        question,
                        semanticKernel.getSkill("InformationFinder").getFunction("Search", null)).block();

        var sources = formSourcesList(searchContext.getResult());

        //STEP 2: Build a SK context with the sources retrieved from the memory store and the user question.
        var answerVariables = SKBuilders.variables()
                .withVariable("sources", searchContext.getResult())
                .withVariable("input", question)
                .build();

        /**
         *    STEP 3:
         *    Get a reference of the semantic function [AnswerQuestion] of the [RAG] plugin (a.k.a. skill) from the SK skills registry and provide it with the pre-built context.
         *    Triggering Open AI to get an answerVariables.
         */
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
    public void runStreaming(String questionOrConversation, RAGOptions options, OutputStream outputStream) {
        throw new IllegalStateException("Streaming not supported for this approach");
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

    /**
     *  Build semantic kernel context with AnswerQuestion semantic function and InformationFinder.Search native function.
     *  AnswerQuestion is imported from src/main/resources/semantickernel/Plugins.
     *  InformationFinder.Search is implemented in a traditional Java class method: CognitiveSearchPlugin.search
     *
     * @param options
     * @return
     */
    private Kernel buildSemanticKernel( RAGOptions options) {
        Kernel kernel = SKBuilders.kernel()
                .withDefaultAIService(SKBuilders.chatCompletion()
                        .withModelId(gptChatDeploymentModelId)
                        .withOpenAIClient(this.openAIAsyncClient)
                        .build())
                .build();

        kernel.importSkill(new CognitiveSearchPlugin(this.cognitiveSearchProxy, this.openAIProxy,options), "InformationFinder");

        kernel.importSkillFromResources(
                "semantickernel/Plugins",
                "RAG",
                "AnswerQuestion",
                null
        );

        return kernel;
    }
}
