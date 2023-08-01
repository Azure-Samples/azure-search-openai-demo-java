package com.microsoft.openai.samples.rag.ask.approaches.semantickernel;

import com.azure.ai.openai.OpenAIAsyncClient;
import com.microsoft.semantickernel.Kernel;
import com.microsoft.semantickernel.KernelConfig;
import com.microsoft.semantickernel.builders.SKBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
public class SemanticKernelConfiguration {

    @Value("${openai.gpt.deployment}")
    private String gptDeploymentModelId;

    @Value("${openai.chatgpt.deployment}")
    private String gptChatDeploymentModelId;

    @Autowired
    OpenAIAsyncClient openAIAsyncClient;

    @Bean
    @Scope("prototype")
    public Kernel semanticKernel() {
        KernelConfig config = SKBuilders.kernelConfig()
                .addTextCompletionService("davinci",
                        kernel -> SKBuilders.textCompletionService().build(openAIAsyncClient, gptDeploymentModelId))
                .build();

        Kernel kernel = SKBuilders.kernel()
                .withKernelConfig(config)
                .build();


        return kernel;

    }


}
