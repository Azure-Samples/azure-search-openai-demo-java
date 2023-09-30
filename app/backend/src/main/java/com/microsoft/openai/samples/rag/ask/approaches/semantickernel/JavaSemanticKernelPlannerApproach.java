package com.microsoft.openai.samples.rag.ask.approaches.semantickernel;

import com.azure.ai.openai.OpenAIAsyncClient;
import com.microsoft.openai.samples.rag.approaches.RAGApproach;
import com.microsoft.openai.samples.rag.approaches.RAGOptions;
import com.microsoft.openai.samples.rag.approaches.RAGResponse;
import com.microsoft.openai.samples.rag.proxy.CognitiveSearchProxy;
import com.microsoft.openai.samples.rag.proxy.OpenAIProxy;
import com.microsoft.semantickernel.Kernel;
import com.microsoft.semantickernel.SKBuilders;
import com.microsoft.semantickernel.orchestration.SKContext;
import com.microsoft.semantickernel.planner.sequentialplanner.SequentialPlanner;
import com.microsoft.semantickernel.planner.sequentialplanner.SequentialPlannerRequestSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Set;

/**
 *    Accomplish the same task as in the Retrieve-then-read approach but using Semantic Kernel framework and Planner goal oriented concept.
 */
@Component
public class JavaSemanticKernelPlannerApproach implements RAGApproach<String, RAGResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(JavaSemanticKernelPlannerApproach.class);
    private static final String PLAN_PROMPT = """
            Take the input as a question and answer it finding any information needed
            """;
    private final CognitiveSearchProxy cognitiveSearchProxy;

    private final OpenAIProxy openAIProxy;

    private final OpenAIAsyncClient openAIAsyncClient;

    @Value("${openai.chatgpt.deployment}")
    private String gptChatDeploymentModelId;

    public JavaSemanticKernelPlannerApproach(CognitiveSearchProxy cognitiveSearchProxy, OpenAIAsyncClient openAIAsyncClient, OpenAIProxy openAIProxy) {
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

        Kernel semanticKernel = buildSemanticKernel(options);

        SequentialPlanner sequentialPlanner = new SequentialPlanner(semanticKernel, new SequentialPlannerRequestSettings(
                0.7f,
                100,
                Set.of(),
                Set.of(),
                Set.of(),
                1024
        ), null);

        var plan = Objects.requireNonNull(sequentialPlanner.createPlanAsync(PLAN_PROMPT).block());

        LOGGER.debug("Semantic kernel plan calculated is [{}]", plan.toPlanString());

        SKContext planContext = Objects.requireNonNull(plan.invokeAsync(question).block());

       return new RAGResponse.Builder()
                                .prompt(plan.toPlanString())
                                .answer(planContext.getResult())
                                //.sourcesAsText(planContext.getVariables().get("sources"))
                                .sourcesAsText("sources placeholders")
                                .question(question)
                                .build();

    }

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
