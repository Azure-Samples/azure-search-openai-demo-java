package com.microsoft.openai.samples.rag.ask.approaches.semantickernel;

import com.azure.ai.openai.OpenAIAsyncClient;
import com.azure.search.documents.models.*;
import com.microsoft.openai.samples.rag.approaches.RAGApproach;
import com.microsoft.openai.samples.rag.approaches.RAGOptions;
import com.microsoft.openai.samples.rag.approaches.RAGResponse;
import com.microsoft.openai.samples.rag.proxy.CognitiveSearchProxy;
import com.microsoft.semantickernel.Kernel;
import com.microsoft.semantickernel.KernelConfig;
import com.microsoft.semantickernel.builders.SKBuilders;
import com.microsoft.semantickernel.orchestration.SKContext;
import com.microsoft.semantickernel.planner.sequentialplanner.SequentialPlanner;
import com.microsoft.semantickernel.planner.sequentialplanner.SequentialPlannerRequestSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;

/**
 *    Accomplish the same task as in the Retrieve-then-read approach but using Semantic Kernel framework and Planner goal oriented concept.
 */
@Component
public class ReadRetrieveReadApproach implements RAGApproach<String, RAGResponse> {
    private static final Logger logger = LoggerFactory.getLogger(ReadRetrieveReadApproach.class);
    private final String planPrompt = """
            Take the input as a question and answer it finding any information needed
            """;

    private CognitiveSearchProxy cognitiveSearchProxy;
    // This will be injected as prototype bean
    @Value("${openai.gpt.deployment}")
    private String gptDeploymentModelId;

    OpenAIAsyncClient openAIAsyncClient;

    public ReadRetrieveReadApproach(CognitiveSearchProxy cognitiveSearchProxy, OpenAIAsyncClient openAIAsyncClient) {
        this.cognitiveSearchProxy = cognitiveSearchProxy;
        this.openAIAsyncClient = openAIAsyncClient;


    }

    /**
     * @param question
     * @param options
     * @return
     */
    @Override
    public RAGResponse run(String question, RAGOptions options) {

        Kernel semanticKernel = buildSemanticKernel(options);

        String customPlannerPrompt;
        try (InputStream altprompt = getClass().getClassLoader().getResourceAsStream("semantickernel/require_context_variable_planner_prompt.txt")) {
            customPlannerPrompt = new String(altprompt.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Cannot create custom Sequential Planner. Cannot found [semantickernel/require_context_variable_planner_prompt.txt] in the classpath ",e);
        }

        SequentialPlanner sequentialPlanner = new SequentialPlanner(semanticKernel, new SequentialPlannerRequestSettings(
                0.7,
                100,
                Set.of(),
                Set.of(),
                Set.of(),
                1024
        ), customPlannerPrompt);

        var plan = sequentialPlanner.createPlanAsync(this.planPrompt).block();

        logger.debug("Semantic kernel plan calculated is [{}]",plan.toPlanString());

        SKContext planContext = plan.invokeAsync(question).block();

       return new RAGResponse.Builder()
                                .prompt(plan.toPlanString())
                                .answer(planContext.getResult())
                                .sourcesAsText(planContext.getVariables().get("sources"))
                                .question(question)
                                .build();

    }


    private Kernel buildSemanticKernel( RAGOptions options) {
        KernelConfig config = SKBuilders.kernelConfig()
                .addTextCompletionService("davinci",
                        kernel -> SKBuilders.textCompletionService().build(this.openAIAsyncClient, gptDeploymentModelId))
                .build();

        Kernel kernel = SKBuilders.kernel()
                .withKernelConfig(config)
                .build();

        kernel.importSkill(new CognitiveSearchPlugin(this.cognitiveSearchProxy, buildSearchOptions(options),options), "CognitiveSearchPlugin");


        kernel.importSkillFromResources(
                "semantickernel/Plugins",
                "RAG",
                "AnswerQuestion",
                null
        );

        return kernel;
    }


    private SearchOptions buildSearchOptions(RAGOptions options){
        var searchOptions = new SearchOptions();

        Optional.ofNullable(options.getTop()).ifPresentOrElse(
                value -> searchOptions.setTop(value),
                () -> searchOptions.setTop(3));
        Optional.ofNullable(options.getExcludeCategory())
                .ifPresentOrElse(
                        value -> searchOptions.setFilter("category ne '%s'".formatted(value.replace("'", "''"))),
                        () -> searchOptions.setFilter(null));

        Optional.ofNullable(options.isSemanticRanker()).ifPresent(isSemanticRanker -> {
            if(isSemanticRanker) {
                searchOptions.setQueryType(QueryType.SEMANTIC);
                searchOptions.setQueryLanguage(QueryLanguage.EN_US);
                searchOptions.setSpeller(QuerySpellerType.LEXICON);
                searchOptions.setSemanticConfigurationName("default");
                searchOptions.setQueryCaption(QueryCaptionType.EXTRACTIVE);
                searchOptions.setQueryCaptionHighlightEnabled(false);
            }
        });
        return searchOptions;
    }

}
