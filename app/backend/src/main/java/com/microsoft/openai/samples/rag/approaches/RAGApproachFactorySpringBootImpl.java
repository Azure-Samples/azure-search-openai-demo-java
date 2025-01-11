package com.microsoft.openai.samples.rag.approaches;

import com.microsoft.openai.samples.rag.ask.approaches.PlainJavaAskApproach;
import com.microsoft.openai.samples.rag.ask.approaches.semantickernel.JavaSemanticKernelChainsApproach;
import com.microsoft.openai.samples.rag.ask.approaches.semantickernel.JavaSemanticKernelWithVectorStoreApproach;
import com.microsoft.openai.samples.rag.chat.approaches.PlainJavaChatApproach;
import com.microsoft.openai.samples.rag.chat.approaches.semantickernel.JavaSemanticKernelChainsChatApproach;
import com.microsoft.openai.samples.rag.chat.approaches.semantickernel.JavaSemanticKernelWithVectorStoreChatApproach;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class RAGApproachFactorySpringBootImpl implements RAGApproachFactory, ApplicationContextAware {

    private static final String JAVA_OPENAI_SDK = "jos";
    private static final String JAVA_SEMANTIC_KERNEL = "jsk";
    private static final String JAVA_SEMANTIC_KERNEL_PLANNER = "jskp";
    private ApplicationContext applicationContext;

    /**
     * Retrieve a specific approach bean definition based on approachName
     *
     * @param approachName
     * @return
     */
    @Override
    public RAGApproach createApproach(String approachName, RAGType ragType, RAGOptions ragOptions) {

        if (ragType.equals(RAGType.CHAT)) {
            if (JAVA_OPENAI_SDK.equals(approachName)) {
                return applicationContext.getBean(PlainJavaChatApproach.class);
            } else if (JAVA_SEMANTIC_KERNEL.equals(approachName)) {
                return applicationContext.getBean(JavaSemanticKernelWithVectorStoreChatApproach.class);
            } else if (
                    JAVA_SEMANTIC_KERNEL_PLANNER.equals(approachName) &&
                            ragOptions != null &&
                            ragOptions.getSemantickKernelMode() != null &&
                            ragOptions.getSemantickKernelMode() == SemanticKernelMode.chains) {
                return applicationContext.getBean(JavaSemanticKernelChainsChatApproach.class);
            }
        } else if (ragType.equals(RAGType.ASK)) {
            if (JAVA_OPENAI_SDK.equals(approachName))
                return applicationContext.getBean(PlainJavaAskApproach.class);
            else if (JAVA_SEMANTIC_KERNEL.equals(approachName))
                return applicationContext.getBean(JavaSemanticKernelWithVectorStoreApproach.class);
            else if (JAVA_SEMANTIC_KERNEL_PLANNER.equals(approachName) && ragOptions != null && ragOptions.getSemantickKernelMode() != null && ragOptions.getSemantickKernelMode() == SemanticKernelMode.chains)
                return applicationContext.getBean(JavaSemanticKernelChainsApproach.class);
        }
        //if this point is reached then the combination of approach and rag type is not supported
        throw new IllegalArgumentException("Invalid combination for approach[%s] and rag type[%s]: ".formatted(approachName, ragType));
    }

    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

}
